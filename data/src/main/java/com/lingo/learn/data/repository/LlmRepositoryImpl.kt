package com.lingo.learn.data.repository

import com.lingo.learn.data.local.dao.TokenUsageLogDao
import com.lingo.learn.data.local.entity.TokenUsageLogEntity
import com.lingo.learn.data.prefs.SecureConfigPrefs
import com.lingo.learn.data.remote.minimax.*
import com.lingo.learn.domain.repository.LlmRepository
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
        val url = baseUrl.trimEnd('/') + "/v1/text/chatcompletion_v2"

        if (authToken.length < 10 || groupId.isEmpty()) {
            return Result.failure(Exception("MiniMax Auth Token or Group ID is not configured"))
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
                    messages = listOf(MinimaxMessage(role = "user", content = prompt))
                )
                val response = service.chatCompletion(url, apiKey, groupId, request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val content = body.choices.firstOrNull()?.message?.content
                    if (content != null) {
                        // Record usage
                        body.usage?.let {
                            tokenUsageLogDao.insertLog(
                                TokenUsageLogEntity(
                                    timestamp = System.currentTimeMillis(),
                                    taskType = taskType,
                                    model = "primary",
                                    inputTokens = it.inputTokens,
                                    outputTokens = it.outputTokens,
                                    totalTokens = it.totalTokens
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
                    messages = listOf(MinimaxMessage(role = "user", content = prompt))
                )
                val response = service.chatCompletion(url, apiKey, groupId, request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val content = body.choices.firstOrNull()?.message?.content
                    if (content != null) {
                        // Record usage
                        body.usage?.let {
                            tokenUsageLogDao.insertLog(
                                TokenUsageLogEntity(
                                    timestamp = System.currentTimeMillis(),
                                    taskType = taskType,
                                    model = "fallback",
                                    inputTokens = it.inputTokens,
                                    outputTokens = it.outputTokens,
                                    totalTokens = it.totalTokens
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

    private suspend fun isBudgetExceeded(): Boolean {
        val limit = prefs.getMonthlyTokenLimit()
        val calendar = java.util.Calendar.getInstance()
        // Calculate the start timestamp of the current month
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
            else -> "Well done! Let's keep moving forward!"
        }
    }
}
