package com.lingo.learn.data.remote.minimax

import com.google.gson.annotations.SerializedName

// === LLM API Models ===

data class MinimaxChatRequest(
    val model: String,
    val messages: List<MinimaxMessage>
)

data class MinimaxMessage(
    val role: String,
    val content: String
)

data class MinimaxChatResponse(
    val choices: List<MinimaxChoice>,
    val usage: MinimaxUsage?
)

data class MinimaxChoice(
    val message: MinimaxMessage,
    @SerializedName("finish_reason") val finishReason: String
)

data class MinimaxUsage(
    @SerializedName("input_tokens") val inputTokens: Int,
    @SerializedName("output_tokens") val outputTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)

// === TTS API Models ===

data class MinimaxTtsRequest(
    val model: String = "speech-01",
    val text: String,
    val stream: Boolean = false,
    @SerializedName("voice_setting") val voiceSetting: MinimaxVoiceSetting,
    @SerializedName("audio_setting") val audioSetting: MinimaxAudioSetting = MinimaxAudioSetting()
)

data class MinimaxVoiceSetting(
    @SerializedName("voice_id") val voiceId: String,
    val speed: Float = 1.0f
)

data class MinimaxAudioSetting(
    @SerializedName("sample_rate") val sampleRate: Int = 24000,
    val bitrate: Int = 128000,
    val format: String = "mp3"
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
