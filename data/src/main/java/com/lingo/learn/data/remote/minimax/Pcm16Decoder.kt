package org.akj.lingo.learn.data.remote.minimax

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decodes an m4a/AAC recording into raw PCM16 mono at 16 kHz, downmixing
 * stereo and linearly interpolating the sample rate as needed. Used to feed
 * the plan ASR WebSocket channel, which expects pcm/raw/16000.
 */
object Pcm16Decoder {

    private const val TARGET_RATE = 16000
    private const val TARGET_CHANNELS = 1
    private const val TIMEOUT_US = 10_000L

    fun decode(file: File): ByteArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(file.absolutePath)
        try {
            var trackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    trackIndex = i
                    break
                }
            }
            check(trackIndex >= 0) { "no audio track in ${file.name}" }
            extractor.selectTrack(trackIndex)
            val trackFormat = extractor.getTrackFormat(trackIndex)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME)
                ?: error("audio track has no MIME in ${file.name}")
            val codec = MediaCodec.createDecoderByType(mime)
            try {
                codec.configure(trackFormat, null, null, 0)
                codec.start()
                val audioOut = ByteArrayOutputStream()
                val bufferInfo = MediaCodec.BufferInfo()
                var inputDone = false
                var outputDone = false
                while (!outputDone) {
                    if (!inputDone) {
                        val inIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                        if (inIndex >= 0) {
                            val inBuffer = codec.getInputBuffer(inIndex)!!
                            val sampleSize = extractor.readSampleData(inBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                    val outIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    when {
                        outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                        outIndex >= 0 -> {
                            val outBuffer = codec.getOutputBuffer(outIndex) ?: continue
                            outBuffer.position(bufferInfo.offset)
                            outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            val chunk = ByteArray(bufferInfo.size)
                            outBuffer.get(chunk)
                            audioOut.write(chunk)
                            codec.releaseOutputBuffer(outIndex, false)
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                        }
                    }
                }
                val pcm = audioOut.toByteArray()
                val srcRate = getIntFormat(codec, MediaFormat.KEY_SAMPLE_RATE, TARGET_RATE)
                val srcChannels = getIntFormat(codec, MediaFormat.KEY_CHANNEL_COUNT, TARGET_CHANNELS)
                return resample(pcm, srcRate, srcChannels, TARGET_RATE, TARGET_CHANNELS)
            } finally {
                codec.stop()
                codec.release()
            }
        } finally {
            extractor.release()
        }
    }

    private fun getIntFormat(codec: MediaCodec, key: String, fallback: Int): Int {
        return try {
            codec.outputFormat.getInteger(key)
        } catch (e: Exception) {
            fallback
        }
    }

    /** Downmix to mono and linearly interpolate to [targetRate]; little-endian PCM16. */
    internal fun resample(pcm: ByteArray, srcRate: Int, srcChannels: Int, targetRate: Int, targetChannels: Int): ByteArray {
        var samples = toShorts(pcm)
        if (srcChannels > 1) {
            val mono = ShortArray(samples.size / srcChannels)
            for (i in mono.indices) {
                var sum = 0
                for (c in 0 until srcChannels) sum += samples[i * srcChannels + c]
                mono[i] = (sum / srcChannels).toShort()
            }
            samples = mono
        }
        if (srcRate != targetRate && samples.isNotEmpty()) {
            val outLen = (samples.size.toLong() * targetRate / srcRate).toInt().coerceAtLeast(1)
            val out = ShortArray(outLen)
            for (i in out.indices) {
                val pos = i.toDouble() * (samples.size - 1) / (outLen - 1)
                val lo = pos.toInt()
                val hi = (lo + 1).coerceAtMost(samples.size - 1)
                val frac = pos - lo.toDouble()
                out[i] = (samples[lo] * (1 - frac) + samples[hi] * frac).toInt().toShort()
            }
            samples = out
        }
        return toBytes(samples)
    }

    private fun toShorts(bytes: ByteArray): ShortArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val shorts = ShortArray(bytes.size / 2)
        buffer.asShortBuffer().get(shorts)
        return shorts
    }

    private fun toBytes(shorts: ShortArray): ByteArray {
        val buffer = ByteBuffer.allocate(shorts.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        buffer.asShortBuffer().put(shorts)
        return buffer.array()
    }
}