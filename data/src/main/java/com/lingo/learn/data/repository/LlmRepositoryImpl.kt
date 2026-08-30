package org.akj.lingo.learn.data.repository

import org.akj.lingo.learn.data.local.dao.LlmTraceDao
import org.akj.lingo.learn.data.local.dao.TokenUsageLogDao
import org.akj.lingo.learn.data.local.entity.LlmTraceEntity
import org.akj.lingo.learn.data.local.entity.TokenUsageLogEntity
import org.akj.lingo.learn.data.prefs.SecureConfigPrefs
import org.akj.lingo.learn.data.remote.minimax.*
import org.akj.lingo.learn.domain.model.ChatMessage
import org.akj.lingo.learn.domain.provider.ProviderEndpoints
import org.akj.lingo.learn.domain.repository.LlmRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmRepositoryImpl @Inject constructor(
    private val service: MinimaxService,
    private val prefs: SecureConfigPrefs,
    private val tokenUsageLogDao: TokenUsageLogDao,
    private val llmTraceDao: LlmTraceDao
) : LlmRepository {
    private val gson = Gson()

    override suspend fun complete(prompt: String, taskType: String, maxTokens: Int): Result<String> {
        return chat(listOf(ChatMessage(role = "user", content = prompt)), taskType, maxTokens)
    }

    override fun completeStream(prompt: String, taskType: String, maxTokens: Int): Flow<String> = flow {
        val authToken = prefs.getAuthToken()
        val apiKey = "Bearer $authToken"
        val groupId = prefs.getGroupId()
        val baseUrl = prefs.getBaseUrl()
        val url = ProviderEndpoints.chatUrl(baseUrl)

        if (authToken.length < 10) {
            throw Exception("Auth Token is not configured")
        }

        if (isBudgetExceeded()) {
            if (taskType in STRICT_CONTENT_TASK_TYPES) {
                throw Exception("Monthly token budget exceeded")
            } else {
                emit(getFallbackTemplate(taskType))
                return@flow
            }
        }

        val primaryModel = prefs.getPrimaryModel()
        val messages = listOf(MinimaxMessage(role = "user", content = prompt))
        val groupIdParam = groupId.takeIf { it.isNotBlank() }

        val startedAt = System.currentTimeMillis()
        var streamSucceeded = false
        val accumulated = java.lang.StringBuilder()

        runCatching {
            withTimeout(timeoutFor(taskType)) {
                val request = MinimaxChatRequest(
                    model = primaryModel,
                    messages = messages,
                    maxTokens = maxTokens,
                    stream = true
                )
                val response = service.chatCompletionStream(url, apiKey, groupIdParam, request)
                if (response.isSuccessful && response.body() != null) {
                    val reader = response.body()!!.byteStream().bufferedReader()
                    reader.useLines { lines ->
                        for (line in lines) {
                            val trimmed = line.trim()
                            if (trimmed.startsWith("data:")) {
                                val data = trimmed.removePrefix("data:").trim()
                                if (data == "[DONE]") break
                                try {
                                    val chunk = gson.fromJson(data, MinimaxStreamChunk::class.java)
                                    val delta = chunk?.choices?.firstOrNull()?.delta?.content
                                    if (!delta.isNullOrEmpty()) {
                                        accumulated.append(delta)
                                        emit(delta)
                                    }
                                } catch (_: Exception) {
                                }
                            }
                        }
                    }
                    if (accumulated.isNotEmpty()) {
                        streamSucceeded = true
                    }
                }
            }
        }

        val durationMs = System.currentTimeMillis() - startedAt

        if (streamSucceeded) {
            llmTraceDao.insertTrace(
                LlmTraceEntity(
                    timestamp = startedAt,
                    taskType = taskType,
                    model = "primary-stream",
                    success = true,
                    durationMs = durationMs,
                    inputTokens = 0,
                    outputTokens = 0,
                    totalTokens = 0,
                    detail = accumulated.toString().hashCode().toString()
                )
            )
            return@flow
        }

        // Streaming was unavailable or failed; fall back to non-streaming complete()
        val completeResult = complete(prompt, taskType, maxTokens)
        if (completeResult.isSuccess) {
            emit(completeResult.getOrThrow())
        } else {
            throw completeResult.exceptionOrNull() ?: Exception("Streaming completion failed")
        }
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

        val primaryResult = attemptModel(
            url, apiKey, groupIdParam, primaryModel, "primary", minimaxMessages, maxTokens, taskType
        )

        if (primaryResult.isSuccess) {
            return Result.success(primaryResult.getOrThrow().content)
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
        val fallbackResult = attemptModel(
            url, apiKey, groupIdParam, fallbackModel, "fallback", minimaxMessages, maxTokens, taskType
        )

        if (fallbackResult.isSuccess) {
            return Result.success(fallbackResult.getOrThrow().content)
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

    /** Outcome of one model attempt (content + token counts for the trace). */
    private data class ModelAttempt(
        val content: String,
        val inputTokens: Int,
        val outputTokens: Int,
        val totalTokens: Int
    )

    /**
     * Runs one model call with the task timeout, records the aggregate token
     * usage (existing behavior) and a per-call [LlmTraceEntity] for the Settings
     * debug panel: latency, success, tokens, and a result fingerprint.
     */
    private suspend fun attemptModel(
        url: String,
        apiKey: String,
        groupIdParam: String?,
        model: String,
        modelLabel: String,
        minimaxMessages: List<MinimaxMessage>,
        maxTokens: Int,
        taskType: String
    ): Result<ModelAttempt> {
        val startedAt = System.currentTimeMillis()
        val result = runCatching {
            withTimeout(timeoutFor(taskType)) {
                val request = MinimaxChatRequest(
                    model = model,
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
                        val inputTokens = usage?.inputTokens ?: usage?.promptTokens ?: 0
                        val outputTokens = usage?.outputTokens ?: usage?.completionTokens ?: 0
                        val totalTokens = usage?.totalTokens ?: (inputTokens + outputTokens)
                        return@withTimeout ModelAttempt(content, inputTokens, outputTokens, totalTokens)
                    }
                }
                throw Exception("${modelLabel.replaceFirstChar { it.uppercase() }} model request failed: ${response.code()}")
            }
        }

        val durationMs = System.currentTimeMillis() - startedAt
        val attempt = result.getOrNull()

        if (attempt != null && attempt.totalTokens > 0) {
            tokenUsageLogDao.insertLog(
                TokenUsageLogEntity(
                    timestamp = startedAt,
                    taskType = taskType,
                    model = modelLabel,
                    inputTokens = attempt.inputTokens,
                    outputTokens = attempt.outputTokens,
                    totalTokens = attempt.totalTokens
                )
            )
        }

        llmTraceDao.insertTrace(
            LlmTraceEntity(
                timestamp = startedAt,
                taskType = taskType,
                model = modelLabel,
                success = result.isSuccess,
                durationMs = durationMs,
                inputTokens = attempt?.inputTokens ?: 0,
                outputTokens = attempt?.outputTokens ?: 0,
                totalTokens = attempt?.totalTokens ?: 0,
                detail = if (result.isSuccess) attempt!!.content.hashCode().toString()
                else (result.exceptionOrNull()?.message ?: "").take(200)
            )
        )
        return result
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
