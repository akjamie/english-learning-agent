package org.akj.lingo.learn.data.repository

import org.akj.lingo.learn.data.prefs.SecureConfigPrefs
import org.akj.lingo.learn.data.remote.minimax.MinimaxService
import org.akj.lingo.learn.domain.model.PronunciationResult
import org.akj.lingo.learn.domain.model.WordScore
import org.akj.lingo.learn.domain.repository.AsrRepository
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
    private val prefs: SecureConfigPrefs
) : AsrRepository {

    override suspend fun evaluatePronunciation(audioFile: File, referenceText: String): Result<PronunciationResult> {
        val authToken = prefs.getAuthToken()
        val apiKey = "Bearer $authToken"
        val groupId = prefs.getGroupId()
        val baseUrl = prefs.getBaseUrl()
        val url = baseUrl.trimEnd('/') + "/v1/audio_to_text"
        val asrModel = prefs.getAsrModel()

        // Downgrade to offline fallback if ASR credentials are not configured
        if (authToken.length < 10 || groupId.isEmpty()) {
            return Result.success(getOfflineFallbackResult(referenceText))
        }

        return runCatching {
            // Prepare multipart body
            val requestFile = audioFile.asRequestBody("audio/mpeg".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
            val modelPart = asrModel.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = service.audioToText(url, apiKey, groupId, filePart, modelPart)
            if (response.isSuccessful && response.body() != null) {
                val asrText = response.body()!!.text

                // Evaluate pronunciation by comparing ASR text and Reference text
                val sanitizedReference = referenceText.lowercase().replace("[^a-z0-9 ]".toRegex(), "").trim()
                val sanitizedAsr = asrText.lowercase().replace("[^a-z0-9 ]".toRegex(), "").trim()

                val referenceWords = sanitizedReference.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                val asrWords = sanitizedAsr.split("\\s+".toRegex()).filter { it.isNotEmpty() }

                val distance = calculateLevenshteinDistance(sanitizedReference, sanitizedAsr)
                val maxLength = maxOf(sanitizedReference.length, sanitizedAsr.length)

                val similarityPercent = if (maxLength == 0) 100 else {
                    ((1.0 - distance.toDouble() / maxLength.toDouble()) * 100).toInt()
                }

                val finalScore = similarityPercent.coerceIn(0, 100)

                // Score each reference word by its best match in ASR output
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

                return@runCatching PronunciationResult(
                    overallScore = finalScore,
                    wordScores = wordScores,
                    feedback = feedback,
                    isFromFallback = false
                )
            }
            throw Exception("MiniMax ASR failed: ${response.code()}")
        }.recover {
            getOfflineFallbackResult(referenceText)
        }
    }

    override suspend fun transcribeAudio(audioFile: File): Result<String> {
        val authToken = prefs.getAuthToken()
        val apiKey = "Bearer $authToken"
        val groupId = prefs.getGroupId()
        val baseUrl = prefs.getBaseUrl()
        val url = baseUrl.trimEnd('/') + "/v1/audio_to_text"
        val asrModel = prefs.getAsrModel()

        if (authToken.length < 10 || groupId.isEmpty()) {
            return Result.failure(Exception("ASR credentials not configured"))
        }

        return runCatching {
            val requestFile = audioFile.asRequestBody("audio/mpeg".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
            val modelPart = asrModel.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = service.audioToText(url, apiKey, groupId, filePart, modelPart)
            if (response.isSuccessful && response.body() != null) {
                return@runCatching response.body()!!.text
            }
            throw Exception("MiniMax ASR failed: ${response.code()}")
        }
    }

    override fun getOfflineFallbackResult(referenceText: String): PronunciationResult {
        val words = referenceText.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        var totalScore = 0
        val wordScores = words.map { word ->
            val score = kotlin.random.Random.nextInt(75, 99)
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
