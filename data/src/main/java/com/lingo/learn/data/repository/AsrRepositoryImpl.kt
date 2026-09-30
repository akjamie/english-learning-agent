package org.akj.lingo.learn.data.repository

import org.akj.lingo.learn.data.prefs.SecureConfigPrefs
import org.akj.lingo.learn.data.remote.minimax.MinimaxService
import org.akj.lingo.learn.data.remote.minimax.Pcm16Decoder
import org.akj.lingo.learn.data.remote.minimax.PlanAsrClient
import org.akj.lingo.learn.domain.model.PronunciationResult
import org.akj.lingo.learn.domain.model.WordScore
import org.akj.lingo.learn.domain.provider.ProviderEndpoints
import org.akj.lingo.learn.domain.repository.AsrRepository
import org.akj.lingo.learn.domain.usecase.PhonemeHintEngine
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AsrRepositoryImpl @Inject constructor(
    private val service: MinimaxService,
    private val prefs: SecureConfigPrefs,
    private val planAsrClient: PlanAsrClient
) : AsrRepository {

    private val phonemeHintEngine = PhonemeHintEngine()

    override suspend fun evaluatePronunciation(audioFile: File, referenceText: String): Result<PronunciationResult> {
        val authToken = prefs.getAuthToken()
        val wsUrl = prefs.getAsrWsUrl()
        val resourceId = prefs.getAsrResourceId()

        if (authToken.length < 10) {
            return Result.success(getOfflineFallbackResult(referenceText))
        }

        return runCatching {
            val asrText = if (!wsUrl.isNullOrBlank()) {
                val pcm16 = Pcm16Decoder.decode(audioFile)
                android.util.Log.i("LingoAsr", "decode ok file=${audioFile.name} pcmBytes=${pcm16.size}")
                planAsrClient.transcribe(pcm16, 16000, resourceId, authToken, wsUrl)
            } else {
                transcribeLegacy(audioFile)
            }
            return@runCatching scoreAgainstReference(referenceText, asrText)
        }
    }

    private suspend fun transcribeLegacy(audioFile: File): String {
        val apiKey = "Bearer ${prefs.getAuthToken()}"
        val groupId = prefs.getGroupId()
        val baseUrl = prefs.getBaseUrl()
        val url = ProviderEndpoints.asrUrl(baseUrl)
        val asrModel = prefs.getAsrModel()
        val resourceId = prefs.getAsrResourceId()
        val groupIdParam = groupId.takeIf { it.isNotBlank() }
        val requestFile = audioFile.asRequestBody(audioContentType(audioFile))
        val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
        val modelPart = asrModel.toRequestBody("text/plain".toMediaTypeOrNull())
        val response = service.audioToText(url, apiKey, resourceId, groupIdParam, filePart, modelPart)
        if (!response.isSuccessful || response.body() == null) {
            throw Exception("MiniMax ASR failed: ${response.code()}")
        }
        return response.body()!!.text
    }

    private fun scoreAgainstReference(referenceText: String, asrText: String): PronunciationResult {
        val sanitizedReference = referenceText.lowercase().replace("[^a-z0-9 ]".toRegex(), "").trim()
        val sanitizedAsr = asrText.lowercase().replace("[^a-z0-9 ]".toRegex(), "").trim()

        val referenceWords = sanitizedReference.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val asrWords = sanitizedAsr.split("\\s+".toRegex()).filter { it.isNotEmpty() }

        // The mic can capture silence or unintelligible speech; the ASR transcript
        // then comes back empty. Score 0 instead of crashing (minBy on an empty list).
        if (asrWords.isEmpty()) {
            return PronunciationResult(
                overallScore = 0,
                wordScores = referenceWords.map { WordScore(word = it, score = 0) },
                feedback = "We couldn't hear you clearly. Speak a little louder and try again!",
                isFromFallback = false,
                phonemeHints = phonemeHintEngine.detectHints(referenceText, asrText)
            )
        }

        val distance = calculateLevenshteinDistance(sanitizedReference, sanitizedAsr)
        val maxLength = maxOf(sanitizedReference.length, sanitizedAsr.length)

        val similarityPercent = if (maxLength == 0) 100 else {
            ((1.0 - distance.toDouble() / maxLength.toDouble()) * 100).toInt()
        }

        val finalScore = similarityPercent.coerceIn(0, 100)

        val wordScores = referenceWords.map { refWord ->
            val bestMatch = asrWords.minBy { calculateLevenshteinDistance(refWord, it) }
            val dist = calculateLevenshteinDistance(refWord, bestMatch)
            val maxLen = maxOf(refWord.length, bestMatch.length)
            val wordScore = if (maxLen == 0) 100
            else ((1.0 - dist.toDouble() / maxLen) * 100).toInt().coerceIn(0, 100)
            WordScore(word = refWord, score = wordScore)
        }

        val feedback = if (finalScore >= 80) {
            "Brilliant pronunciation! You matched the sentence almost perfectly."
        } else {
            "Good try! Pay attention to the clarity of each word and try again."
        }

        val phonemeHints = phonemeHintEngine.detectHints(referenceText, asrText)

        return PronunciationResult(
            overallScore = finalScore,
            wordScores = wordScores,
            feedback = feedback,
            isFromFallback = false,
            phonemeHints = phonemeHints
        )
    }

    /**
     * Maps the recorded file's extension to a valid multipart content type.
     * VoiceRecorder produces `.m4a` (MPEG-4/AAC); mislabelling it as `audio/mpeg`
     * (MP3) makes the ASR endpoint reject the upload and silently route to the
     * offline fallback.
     */
    private fun audioContentType(audioFile: File): okhttp3.MediaType? {
        return when (audioFile.extension.lowercase()) {
            "m4a", "mp4", "aac" -> "audio/mp4".toMediaTypeOrNull()
            "wav" -> "audio/wav".toMediaTypeOrNull()
            "mp3", "mpeg" -> "audio/mpeg".toMediaTypeOrNull()
            else -> "application/octet-stream".toMediaTypeOrNull()
        }
    }

    override suspend fun transcribeAudio(audioFile: File): Result<String> {
        val authToken = prefs.getAuthToken()
        if (authToken.length < 10) {
            return Result.failure(Exception("ASR credentials not configured"))
        }
        val wsUrl = prefs.getAsrWsUrl()
        val resourceId = prefs.getAsrResourceId()

        return runCatching {
            if (!wsUrl.isNullOrBlank()) {
                val pcm16 = Pcm16Decoder.decode(audioFile)
                planAsrClient.transcribe(pcm16, 16000, resourceId, authToken, wsUrl)
            } else {
                transcribeLegacy(audioFile)
            }
        }
    }

    override fun getOfflineFallbackResult(referenceText: String): PronunciationResult {
        val words = referenceText.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        var totalScore = 0
        val wordScores = words.map { word ->
            val score = 85
            totalScore += score
            WordScore(word = word.replace("[^a-zA-Z]".toRegex(), ""), score = score)
        }
        val averageScore = if (words.isNotEmpty()) totalScore / words.size else 85
        return PronunciationResult(
            overallScore = averageScore,
            wordScores = wordScores,
            feedback = if (averageScore >= 85) "Offline Practice: Recording succeeded. Your pronunciation sounds great!" else "Offline Practice: Good try! Keep practicing.",
            isFromFallback = true
        )
    }

    private fun calculateLevenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
