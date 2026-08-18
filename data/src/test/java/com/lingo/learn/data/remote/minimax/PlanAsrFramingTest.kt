package org.akj.lingo.learn.data.remote.minimax

import com.google.gson.Gson
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class PlanAsrFramingTest {

    @Test
    fun `init request frame round-trips type flag sequence and payload`() {
        val payload = gzip("""{"user":{"uid":"abc"},"audio":{"format":"pcm"}}""".toByteArray())
        val bytes = marshalAsrFrame(C_FULL_CLIENT_REQUEST, F_POSITIVE_SEQ, sequence = 1, payload = payload)

        val frame = unmarshalAsrFrame(bytes)

        assertEquals(C_FULL_CLIENT_REQUEST, frame.type)
        assertEquals(F_POSITIVE_SEQ, frame.flag)
        assertEquals(1, frame.sequence)
        assertEquals(CMP_GZIP, frame.compression)
        assertArrayEquals(payload, frame.payload)
        assertTrue(frame.decompressedPayload().decodeToString().contains("format"))
    }

    @Test
    fun `last audio chunk round-trips negative sequence`() {
        val pcm = ByteArray(3200) { (it % 128).toByte() }
        val bytes = marshalAsrFrame(C_AUDIO_ONLY_CLIENT, F_NEGATIVE_SEQ, sequence = -5, payload = gzip(pcm))

        val frame = unmarshalAsrFrame(bytes)

        assertEquals(C_AUDIO_ONLY_CLIENT, frame.type)
        assertEquals(F_NEGATIVE_SEQ, frame.flag)
        assertEquals(-5, frame.sequence)
        assertArrayEquals(pcm, frame.decompressedPayload())
    }

    @Test
    fun `error frame round-trips event session id and error code`() {
        val sid = "sid-123".toByteArray()
        val buf = ByteBuffer.allocate(4 + 4 + 4 + sid.size + 4 + 4)
        buf.put(
            byteArrayOf(
                ((1 shl 4) or 1).toByte(),
                ((C_ERROR shl 4) or F_WITH_EVENT).toByte(),
                ((S_JSON shl 4) or CMP_NONE).toByte(),
                0x00
            )
        )
        buf.putInt(47010)
        buf.putInt(sid.size)
        buf.put(sid)
        buf.putInt(47100)
        buf.putInt(0)

        val frame = unmarshalAsrFrame(buf.array())

        assertEquals(C_ERROR, frame.type)
        assertEquals(F_WITH_EVENT, frame.flag)
        assertEquals("sid-123", frame.sessionId)
        assertEquals(47100, frame.errorCode)
    }

    @Test
    fun `front end result frame with event carries session id and gzipped payload`() {
        val text = """{"result":{"text":"hello world","is_final":true}}"""
        val payload = gzip(text.toByteArray())
        val sid = "session-9".toByteArray()
        val buf = ByteBuffer.allocate(4 + 4 + 4 + sid.size + 4 + payload.size)
        buf.put(
            byteArrayOf(
                ((1 shl 4) or 1).toByte(),
                ((C_FRONT_END_RESULT_SERVER shl 4) or F_WITH_EVENT).toByte(),
                ((S_JSON shl 4) or CMP_GZIP).toByte(),
                0x00
            )
        )
        buf.putInt(20000)
        buf.putInt(sid.size)
        buf.put(sid)
        buf.putInt(payload.size)
        buf.put(payload)

        val frame = unmarshalAsrFrame(buf.array())

        assertEquals("session-9", frame.sessionId)
        val parsed = Gson().fromJson(String(frame.decompressedPayload()), PlanAsrResultPayload::class.java)
        assertEquals("hello world", parsed.result?.text)
        assertEquals(true, parsed.result?.isFinal)
    }

    @Test
    fun `no-seq frame omits sequence field and round-trips payload`() {
        val payload = "raw bytes".toByteArray()
        val bytes = marshalAsrFrame(C_FRONT_END_RESULT_SERVER, F_LAST_NO_SEQ, sequence = 0, payload = payload)

        val frame = unmarshalAsrFrame(bytes)

        assertEquals(C_FRONT_END_RESULT_SERVER, frame.type)
        assertEquals(F_LAST_NO_SEQ, frame.flag)
        assertEquals(0, frame.sequence)
        assertArrayEquals(payload, frame.payload)
    }
}