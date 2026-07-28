package org.akj.lingo.learn.data.repository

import org.akj.lingo.learn.data.local.dao.TokenUsageLogDao
import org.akj.lingo.learn.data.local.entity.TokenUsageLogEntity
import org.akj.lingo.learn.data.prefs.SecureConfigPrefs
import org.akj.lingo.learn.data.remote.minimax.*
import org.akj.lingo.learn.domain.repository.LlmRepository
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmRepositoryImpl @Inject constructor(
    private val service: MinimaxService,
    private val prefs: SecureConfigPrefs,
    private val tokenUsageLogDao: TokenUsageLogDao
) : LlmRepository {

    override suspend fun complete(prompt: String, taskType: String, maxTokens: Int): Result<String> {
        val authToken = prefs.getAuthToken()
        val apiKey = "Bearer $authToken"
        val groupId = prefs.getGroupId()
        val baseUrl = prefs.getBaseUrl()
        val endpoint = prefs.getLlmEndpoint()
        val url = baseUrl.trimEnd('/') + endpoint

        if (authToken.length < 10 || groupId.isEmpty()) {
            return Result.failure(Exception("Auth Token or Group ID is not configured"))
        }

        // 1. Check token budget
        if (isBudgetExceeded()) {
            return Result.success(getFallbackTemplate(taskType))
        }

        // 2. Try primary model
        val primaryModel = prefs.getPrimaryModel()
        val primaryResult = runCatching {
            withTimeout(8000) {
                val request = MinimaxChatRequest(
                    model = primaryModel,
                    messages = listOf(MinimaxMessage(role = "user", content = prompt)),
                    maxTokens = maxTokens
                )
                val response = service.chatCompletion(url, apiKey, groupId, request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val content = tryParseResponse(body)
                    if (content != null) {
                        // Record usage - handle both OpenAI and Claude usage formats
                        val usage = body.usage
                        val totalTokens = usage?.totalTokens
                            ?: (usage?.inputTokens ?: usage?.promptTokens ?: 0) +
                                (usage?.outputTokens ?: usage?.completionTokens ?: 0)
                        if (totalTokens > 0) {
                            tokenUsageLogDao.insertLog(
                                TokenUsageLogEntity(
                                    timestamp = System.currentTimeMillis(),
                                    taskType = taskType,
                                    model = "primary",
                                    inputTokens = usage?.inputTokens ?: usage?.promptTokens ?: 0,
                                    outputTokens = usage?.outputTokens ?: usage?.completionTokens ?: 0,
                                    totalTokens = totalTokens
                                )
                            )
                        }
                        return@withTimeout content
                    }
                }
                throw Exception("Primary model request failed: ${response.code()}")
            }
        }

        if (primaryResult.isSuccess) {
            return Result.success(primaryResult.getOrThrow())
        }

        // 3. Primary model failed -> Try fallback model
        val fallbackModel = prefs.getFallbackModel()
        val fallbackResult = runCatching {
            withTimeout(8000) {
                val request = MinimaxChatRequest(
                    model = fallbackModel,
                    messages = listOf(MinimaxMessage(role = "user", content = prompt)),
                    maxTokens = maxTokens
                )
                val response = service.chatCompletion(url, apiKey, groupId, request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val content = tryParseResponse(body)
                    if (content != null) {
                        val usage = body.usage
                        val totalTokens = usage?.totalTokens
                            ?: (usage?.inputTokens ?: usage?.promptTokens ?: 0) +
                                (usage?.outputTokens ?: usage?.completionTokens ?: 0)
                        if (totalTokens > 0) {
                            tokenUsageLogDao.insertLog(
                                TokenUsageLogEntity(
                                    timestamp = System.currentTimeMillis(),
                                    taskType = taskType,
                                    model = "fallback",
                                    inputTokens = usage?.inputTokens ?: usage?.promptTokens ?: 0,
                                    outputTokens = usage?.outputTokens ?: usage?.completionTokens ?: 0,
                                    totalTokens = totalTokens
                                )
                            )
                        }
                        return@withTimeout content
                    }
                }
                throw Exception("Fallback model request failed: ${response.code()}")
            }
        }

        if (fallbackResult.isSuccess) {
            return Result.success(fallbackResult.getOrThrow())
        }

        // 4. Fallback model also failed -> Use local template fallback
        return Result.success(getFallbackTemplate(taskType))
    }

    /**
     * Tries to extract response text from both OpenAI-compatible and Claude Messages API formats.
     */
    private fun tryParseResponse(response: MinimaxChatResponse): String? {
        val openaiText = response.choices?.firstOrNull()?.message?.content
        if (!openaiText.isNullOrBlank()) return openaiText

        val claudeText = response.content?.firstOrNull()?.text
        if (!claudeText.isNullOrBlank()) return claudeText

        return null
    }

    private suspend fun isBudgetExceeded(): Boolean {
        val limit = prefs.getMonthlyTokenLimit()
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis
        val currentUsage = tokenUsageLogDao.getMonthlyTotalTokens(startOfMonth) ?: 0
        return currentUsage >= limit
    }

    private fun getFallbackTemplate(taskType: String): String {
        return when (taskType) {
            "ENCOURAGEMENT" -> "Great job! You made a solid step forward today, let's keep it up tomorrow!"
            "REPORT" -> "Weekly learning successfully completed. All performance metrics met expectations. Suggest focused listening practice next week."
            "PING" -> "OK"
            else -> "Well done! Let's keep moving forward!"
        }
    }
}
