package org.akj.lingo.learn.data.remote.minimax

import com.google.gson.annotations.SerializedName

// === LLM API Models (OpenAI-compatible + Claude native) ===

data class MinimaxChatRequest(
    val model: String,
    val messages: List<MinimaxMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 1024
)

data class MinimaxMessage(
    val role: String,
    val content: String
)

// OpenAI-compatible response format
data class MinimaxChatResponse(
    val choices: List<MinimaxChoice>?,
    val content: List<ClaudeContentBlock>?,
    val usage: MinimaxUsage?
)

data class MinimaxChoice(
    val message: MinimaxMessage,
    @SerializedName("finish_reason") val finishReason: String?
)

// Claude Messages API native response format
data class ClaudeMessagesResponse(
    val id: String?,
    val type: String?,
    val role: String?,
    val content: List<ClaudeContentBlock>?,
    val model: String?,
    val usage: ClaudeUsage?
)

data class ClaudeContentBlock(
    val type: String?,
    val text: String?
)

data class ClaudeUsage(
    @SerializedName("input_tokens") val inputTokens: Int?,
    @SerializedName("output_tokens") val outputTokens: Int?
)

// Shared usage model (works for both formats)
data class MinimaxUsage(
    @SerializedName("input_tokens") val inputTokens: Int? = null,
    @SerializedName("output_tokens") val outputTokens: Int? = null,
    @SerializedName("total_tokens") val totalTokens: Int? = null,
    @SerializedName("prompt_tokens") val promptTokens: Int? = null,
    @SerializedName("completion_tokens") val completionTokens: Int? = null
)

// === TTS API Models (Ark /audio/tts OpenAI-compatible shape) ===

data class MinimaxTtsRequest(
    val model: String = "seed-tts-2.0",
    val input: String,
    val voice: String,
    val speed: Float = 1.0f,
    @SerializedName("response_format") val responseFormat: String = "mp3"
)

// === Agent Plan TTS Models (openspeech unidirectional NDJSON shape) ===

data class PlanTtsUser(
    val uid: String = "lingo-android"
)

data class PlanTtsAudioParams(
    val format: String = "mp3",
    @SerializedName("sample_rate") val sampleRate: Int = 24000
)

data class PlanTtsReqParams(
    val text: String,
    val speaker: String,
    @SerializedName("audio_params") val audioParams: PlanTtsAudioParams = PlanTtsAudioParams()
)

data class PlanTtsRequest(
    val user: PlanTtsUser = PlanTtsUser(),
    @SerializedName("req_params") val reqParams: PlanTtsReqParams
)

/** One NDJSON line of the plan TTS response; [data] holds a base64 MP3 chunk. */
data class PlanTtsChunk(
    val code: Int = 0,
    val message: String? = null,
    val data: String? = null
)

// === ASR API Models ===

data class MinimaxAsrResponse(
    val text: String,
    @SerializedName("detailed_info") val detailedInfo: MinimaxAsrDetailedInfo?,
    @SerializedName("base_resp") val baseResp: MinimaxBaseResp?
)

data class MinimaxAsrDetailedInfo(
    val words: List<MinimaxAsrWord>?
)

data class MinimaxAsrWord(
    val word: String,
    @SerializedName("start_time") val startTime: Int,
    @SerializedName("end_time") val endTime: Int
)

data class MinimaxBaseResp(
    @SerializedName("status_code") val statusCode: Int,
    @SerializedName("status_msg") val statusMsg: String
)
