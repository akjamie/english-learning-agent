package org.akj.lingo.learn.data.remote.minimax

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Volcengine plan ASR wire constants.
internal const val C_FULL_CLIENT_REQUEST = 1
internal const val C_AUDIO_ONLY_CLIENT = 2
internal const val C_FULL_SERVER_RESPONSE = 9
internal const val C_AUDIO_ONLY_SERVER = 11
internal const val C_FRONT_END_RESULT_SERVER = 12
internal const val C_ERROR = 15
internal const val F_NO_SEQ = 0
internal const val F_POSITIVE_SEQ = 1
internal const val F_LAST_NO_SEQ = 2
internal const val F_NEGATIVE_SEQ = 3
internal const val F_WITH_EVENT = 4
internal const val S_RAW = 0
internal const val S_JSON = 1
internal const val CMP_NONE = 0
internal const val CMP_GZIP = 1
private val CONNECTION_EVENTS = setOf(1, 2, 50, 51, 52)
private val SEQUENCE_TYPES = setOf(C_FULL_CLIENT_REQUEST, C_AUDIO_ONLY_CLIENT, C_FULL_SERVER_RESPONSE, C_AUDIO_ONLY_SERVER, C_FRONT_END_RESULT_SERVER)

/** Parsed Volcengine binary frame; [payload] is the raw (still gzipped when compression==CMP_GZIP) payload. */
internal data class AsrFrame(
    val type: Int,
    val flag: Int,
    val serialization: Int,
    val compression: Int,
    val event: Int? = null,
    val sessionId: String = "",
    val connectId: String = "",
    val sequence: Int = 0,
    val errorCode: Int = 0,
    val payload: ByteArray = ByteArray(0)
) {
    fun decompressedPayload(): ByteArray =
        if (compression == CMP_GZIP && payload.isNotEmpty()) gunzip(payload) else payload
}

/**
 * Marshal a client frame. Wire layout (big-endian):
 * byte0 = (1<<4)|1 (version=1, header_size=1), byte1 = (type<<4)|flag,
 * byte2 = (serialization<<4)|compression, byte3 = 0x00,
 * then if flag in {PositiveSeq, NegativeSeq}: int32 sequence,
 * then uint32 payload_len + payload.
 */
internal fun marshalAsrFrame(
    type: Int,
    flag: Int,
    serialization: Int = S_JSON,
    compression: Int = CMP_GZIP,
    sequence: Int,
    payload: ByteArray
): ByteArray {
    val header = byteArrayOf(
        ((1 shl 4) or 1).toByte(),
        ((type shl 4) or flag).toByte(),
        ((serialization shl 4) or compression).toByte(),
        0x00
    )
    val includeSeq = flag == F_POSITIVE_SEQ || flag == F_NEGATIVE_SEQ
    val buf = ByteBuffer.allocate(4 + (if (includeSeq) 4 else 0) + 4 + payload.size)
    buf.put(header)
    if (includeSeq) buf.putInt(sequence)
    buf.putInt(payload.size)
    buf.put(payload)
    return buf.array()
}

/**
 * Unmarshal a server frame. Mirrors the doubao reference client:
 * after the 4-byte header, WithEvent(flag bit 0x4) frames carry int32 event,
 * then session_id(len+bytes) for non-connection events, else connect_id
 * (len+bytes) for connection events on FullServerResponse; Error frames carry
 * uint32 error_code; PositiveSeq/NegativeSeq frames carry int32 sequence;
 * finally uint32 payload_len + payload.
 */
internal fun unmarshalAsrFrame(data: ByteArray): AsrFrame {
    require(data.size >= 4) { "frame too short: ${data.size} bytes" }
    val headerSize = (data[0].toInt() and 0x0F) * 4
    require(headerSize >= 4 && headerSize <= data.size) { "invalid header size $headerSize" }
    val type = (data[1].toInt() and 0xFF) shr 4
    val flag = data[1].toInt() and 0x0F
    val serialization = (data[2].toInt() and 0xFF) shr 4
    val compression = data[2].toInt() and 0x0F
    var offset = headerSize

    var event: Int? = null
    var sessionId = ""
    var connectId = ""
    if (flag and F_WITH_EVENT != 0) {
        event = readInt32(data, offset)
        offset += 4
        if (event !in CONNECTION_EVENTS) {
            val sidLen = readUInt32(data, offset)
            offset += 4
            sessionId = String(data, offset, sidLen, Charsets.UTF_8)
            offset += sidLen
        } else if (type == C_FULL_SERVER_RESPONSE) {
            val cidLen = readUInt32(data, offset)
            offset += 4
            if (cidLen > 0) {
                connectId = String(data, offset, cidLen, Charsets.UTF_8)
                offset += cidLen
            }
        }
    }

    var errorCode = 0
    var sequence = 0
    if (type == C_ERROR) {
        errorCode = readInt32(data, offset)
        offset += 4
    } else if (flag in setOf(F_POSITIVE_SEQ, F_NEGATIVE_SEQ) && type in SEQUENCE_TYPES) {
        sequence = readInt32(data, offset)
        offset += 4
    }

    val payloadLen = readUInt32(data, offset)
    offset += 4
    val payload = data.copyOfRange(offset, offset + payloadLen)
    return AsrFrame(type, flag, serialization, compression, event, sessionId, connectId, sequence, errorCode, payload)
}

private fun readInt32(data: ByteArray, offset: Int): Int =
    ((data[offset].toInt() and 0xFF) shl 24) or
        ((data[offset + 1].toInt() and 0xFF) shl 16) or
        ((data[offset + 2].toInt() and 0xFF) shl 8) or
        (data[offset + 3].toInt() and 0xFF)

private fun readUInt32(data: ByteArray, offset: Int): Int = readInt32(data, offset)

internal fun gzip(data: ByteArray): ByteArray = ByteArrayOutputStream().use { bos ->
    GZIPOutputStream(bos).use { gz -> gz.write(data) }
    bos.toByteArray()
}

internal fun gunzip(data: ByteArray): ByteArray =
    GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytes() }

internal data class PlanAsrResultPayload(val result: PlanAsrResult? = null)

internal data class PlanAsrResult(
    @SerializedName("text") val text: String? = null,
    @SerializedName("is_final") val isFinal: Boolean? = null
)

@Singleton
class PlanAsrWsClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) : PlanAsrClient {

    private val gson = Gson()

    override suspend fun transcribe(
        pcm16: ByteArray,
        sampleRate: Int,
        resourceId: String,
        apiKey: String,
        wsUrl: String
    ): String = suspendCancellableCoroutine { cont ->
        val requestId = UUID.randomUUID().toString()
        val request = Request.Builder()
            .url(wsUrl)
            .header("X-Api-Key", apiKey)
            .header("X-Api-Resource-Id", resourceId)
            .header("X-Api-Request-Id", requestId)
            .header("X-Api-Connect-Id", UUID.randomUUID().toString())
            .build()

        var text = ""
        var completed = false
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val segment = maxOf(1, sampleRate * 2 * 200 / 1000) // 200ms PCM16 mono
                val initJson = gson.toJson(
                    mapOf(
                        "user" to mapOf("uid" to requestId),
                        "audio" to mapOf(
                            "format" to "pcm",
                            "codec" to "raw",
                            "rate" to sampleRate,
                            "bits" to 16,
                            "channel" to 1
                        ),
                        "request" to mapOf(
                            "model_name" to "bigmodel",
                            "enable_itn" to true,
                            "enable_punc" to true,
                            "enable_ddc" to true,
                            "show_utterances" to true,
                            "enable_nonstream" to false
                        )
                    )
                )
                webSocket.send(
                    ByteString.of(
                        *marshalAsrFrame(
                            type = C_FULL_CLIENT_REQUEST,
                            flag = F_POSITIVE_SEQ,
                            sequence = 1,
                            payload = gzip(initJson.toByteArray())
                        )
                    )
                )

                val chunks = if (pcm16.isEmpty()) listOf(ByteArray(0)) else
                    (0 until pcm16.size step segment).map { pcm16.copyOfRange(it, minOf(it + segment, pcm16.size)) }
                var seq = 2
                chunks.forEachIndexed { index, chunk ->
                    val isLast = index == chunks.size - 1
                    webSocket.send(
                        ByteString.of(
                            *marshalAsrFrame(
                                type = C_AUDIO_ONLY_CLIENT,
                                flag = if (isLast) F_NEGATIVE_SEQ else F_POSITIVE_SEQ,
                                sequence = if (isLast) -seq else seq,
                                payload = gzip(chunk)
                            )
                        )
                    )
                    seq++
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val frame = try {
                    unmarshalAsrFrame(bytes.toByteArray())
                } catch (e: Exception) {
                    if (!completed) {
                        completed = true
                        cont.resumeWithException(e)
                    }
                    return
                }
                if (frame.type == C_ERROR) {
                    if (!completed) {
                        completed = true
                        cont.resumeWithException(Exception("ASR server error code ${frame.errorCode}"))
                    }
                    return
                }
                if (frame.type in setOf(C_FULL_SERVER_RESPONSE, C_FRONT_END_RESULT_SERVER) && frame.payload.isNotEmpty()) {
                    val payload = try {
                        gson.fromJson(String(frame.decompressedPayload()), PlanAsrResultPayload::class.java)
                    } catch (e: Exception) {
                        if (!completed) {
                            completed = true
                            cont.resumeWithException(e)
                        }
                        return
                    }
                    val result = payload.result
                    if (!result?.text.isNullOrBlank()) text = result!!.text.toString()
                    val isFinal = result?.isFinal == true || (frame.flag and F_LAST_NO_SEQ != 0)
                    if (isFinal && !completed) {
                        completed = true
                        cont.resume(text)
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!completed) {
                    completed = true
                    cont.resumeWithException(t)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!completed) {
                    completed = true
                    if (text.isNotEmpty()) cont.resume(text) else
                        cont.resumeWithException(Exception("ASR connection closed before final result (code $code)"))
                }
            }
        }

        val ws = okHttpClient.newBuilder()
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
            .newWebSocket(request, listener)
        cont.invokeOnCancellation { ws.cancel() }
    }
}