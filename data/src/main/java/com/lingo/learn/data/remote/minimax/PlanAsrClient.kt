package org.akj.lingo.learn.data.remote.minimax

/**
 * Streaming ASR over the Agent Plan WebSocket channel (openspeech bigmodel_async).
 */
interface PlanAsrClient {
    /**
     * Transcribe raw PCM16 audio and return the final transcript text.
     * [pcm16] must be mono PCM16 at [sampleRate] Hz; it is chunked into
     * 200ms segments on the wire.
     */
    suspend fun transcribe(
        pcm16: ByteArray,
        sampleRate: Int,
        resourceId: String,
        apiKey: String,
        wsUrl: String
    ): String
}