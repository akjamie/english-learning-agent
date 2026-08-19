package org.akj.lingo.learn.data.repository

import org.akj.lingo.learn.data.local.dao.TokenUsageLogDao
import org.akj.lingo.learn.data.local.entity.TokenUsageLogEntity
import org.akj.lingo.learn.data.prefs.SecureConfigPrefs
import org.akj.lingo.learn.data.remote.minimax.*
import org.akj.lingo.learn.domain.model.ChatMessage
import org.akj.lingo.learn.domain.provider.ProviderEndpoints
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
        return chat(listOf(ChatMessage(role = "user", content = prompt)), taskType, maxTokens)
    }

    override suspend fun chat(messages: List<ChatMessage>, taskType: String, maxTokens: Int): Result<String> {
        val authToken = prefs.getAuthToken()
        val apiKey = "Bearer $authToken"
        val groupId = prefs.getGroupId()
        val baseUrl = prefs.getBaseUrl()
        val url = ProviderEndpoints.chatUrl(baseUrl)

        if (authToken.length < 10) {
            return Result.failure(Exception("Auth Token is not configured"))
        }

        // 1. Check token budget
        if (isBudgetExceeded()) {
            // PLAN/DIAGNOSIS are core AI curriculum content — never silently
            // substitute canned templates for them. Surface the failure so the
            // UI can explain the budget state and offer retry.
            return if (taskType == "PING" || taskType in STRICT_CONTENT_TASK_TYPES) {
                Result.failure(Exception("Monthly token budget exceeded"))
            } else {
                Result.success(getFallbackTemplate(taskType))
            }
        }

        // 2. Try primary model
        val primaryModel = prefs.getPrimaryModel()
        val minimaxMessages = messages.map { MinimaxMessage(role = it.role, content = it.content) }
        val groupIdParam = groupId.takeIf { it.isNotBlank() }

        val primaryResult = runCatching {
            withTimeout(timeoutFor(taskType)) {
                val request = MinimaxChatRequest(
                    model = primaryModel,
                    messages = minimaxMessages,
                    maxTokens = maxTokens
                )
                val response = service.chatCompletion(url, apiKey, groupIdParam, request)
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

        // For PING/connection-test requests, don't attempt fallback.
        // Surface the primary error immediately so the user knows exactly which
        // model/endpoint is failing, rather than getting a misleading
        // "Fallback model request failed" error that hides the real cause.
        if (taskType == "PING") {
            val primaryError = primaryResult.exceptionOrNull()?.message ?: "Primary model request failed"
            return Result.failure(Exception(primaryError))
        }
        val fallbackModel = prefs.getFallbackModel()
        val fallbackResult = runCatching {
            withTimeout(timeoutFor(taskType)) {
                val request = MinimaxChatRequest(
                    model = fallbackModel,
                    messages = minimaxMessages,
                    maxTokens = maxTokens
                )
                val response = service.chatCompletion(url, apiKey, groupIdParam, request)
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

        // 4. Fallback model also failed. PLAN/DIAGNOSIS are the child's actual
        // curriculum — never serve canned templates that look AI-generated.
        return if (taskType == "PING" || taskType in STRICT_CONTENT_TASK_TYPES) {
            val error = fallbackResult.exceptionOrNull()?.message
                ?: primaryResult.exceptionOrNull()?.message
                ?: "Connection failed"
            Result.failure(Exception("Connection failed: $error"))
        } else {
            Result.success(getFallbackTemplate(taskType))
        }
    }

    /** Task types whose output is the child's curriculum; must never degrade to canned content. */
    private val STRICT_CONTENT_TASK_TYPES = setOf("PLAN", "DIAGNOSIS")

    // PLAN/DIAGNOSIS requests ask the model for large JSON payloads (7-day plan with
    // per-day rationale, 10-question diagnosis). Reasoning-heavy models regularly take
    // 20-40s on these. The generic 15s budget was producing spurious timeouts that the
    // UI surfaced as AI failures; the shortened budget below only applies to lightweight
    // chat-style tasks.
    private fun timeoutFor(taskType: String): Long = when (taskType) {
        "PLAN", "DIAGNOSIS" -> 90_000L
        else -> 15_000L
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
            "EXPLAIN" -> "Oops! You made a small mistake here. Remember to practice it a bit more!"
            "HINT" -> "Think about the first letter of the word, or look closely at the picture!"
            "REPORT" -> "Weekly learning successfully completed. All performance metrics met expectations. Suggest focused listening practice next week."
            "PING" -> "OK"
            "ROLEPLAY_SCENARIO" -> """{"system_prompt":"You are Lingo Fox, a friendly tutor. Keep answers short and simple.","opening_line":"Hi there! Let's talk!"}"""
            "LINGO_LETTER" -> "Great week! Your child made steady progress and kept the habit alive. One gentle focus: review the words from the error book together."
            else -> "Well done! Let's keep moving forward!"
        }
    }
}
