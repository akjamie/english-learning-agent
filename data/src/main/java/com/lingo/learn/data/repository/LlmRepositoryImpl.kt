package org.akj.lingo.learn.data.repository

import org.akj.lingo.learn.data.local.dao.TokenUsageLogDao
import org.akj.lingo.learn.data.local.entity.TokenUsageLogEntity
import org.akj.lingo.learn.data.prefs.SecureConfigPrefs
import org.akj.lingo.learn.data.remote.minimax.*
import org.akj.lingo.learn.domain.model.ChatMessage
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
        val minimaxMessages = messages.map { MinimaxMessage(role = it.role, content = it.content) }
        
        val primaryResult = runCatching {
            withTimeout(15000) {
                val request = MinimaxChatRequest(
                    model = primaryModel,
                    messages = minimaxMessages,
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
            withTimeout(15000) {
                val request = MinimaxChatRequest(
                    model = fallbackModel,
                    messages = minimaxMessages,
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
            "EXPLAIN" -> "Oops! You made a small mistake here. Remember to practice it a bit more!"
            "HINT" -> "Think about the first letter of the word, or look closely at the picture!"
            "REPORT" -> "Weekly learning successfully completed. All performance metrics met expectations. Suggest focused listening practice next week."
            "PING" -> "OK"
            "PLAN" -> """{
              "theme": "School Life",
              "difficulty_coefficient": 1.0,
              "days": [
                {
                  "day": 1,
                  "focus": "Vocabulary Introduction",
                  "target_words": ["apple", "school", "friend"],
                  "reference_sentence": "I eat an apple with my friend at school.",
                  "duration_minutes": 15
                },
                {
                  "day": 2,
                  "focus": "Grammar Practice",
                  "target_words": ["teacher", "book"],
                  "reference_sentence": "The teacher reads a book.",
                  "duration_minutes": 15
                },
                {
                  "day": 3,
                  "focus": "Listening & Speaking",
                  "target_words": ["hello", "goodbye"],
                  "reference_sentence": "Hello! How are you?",
                  "duration_minutes": 20
                },
                {
                  "day": 4,
                  "focus": "Reading Comprehension",
                  "target_words": ["play", "learn"],
                  "reference_sentence": "We play and learn together.",
                  "duration_minutes": 15
                },
                {
                  "day": 5,
                  "focus": "Consolidation",
                  "target_words": ["school", "friend", "play"],
                  "reference_sentence": "I play with my friend.",
                  "duration_minutes": 20
                },
                {
                  "day": 6,
                  "focus": "Weekly Quiz",
                  "target_words": [],
                  "reference_sentence": "Review week.",
                  "duration_minutes": 10
                },
                {
                  "day": 7,
                  "focus": "Rest",
                  "target_words": [],
                  "reference_sentence": "Take a break!",
                  "duration_minutes": 0
                }
              ]
            }"""
            "DIAGNOSIS" ->
                """[
                  {"id":1,"type":"LISTENING_EMOJI","title":"1. Listen and Choose","description":"Select the word you hear:","voicePrompt":"apple","options":["🍎 Apple","🍌 Banana","🐱 Cat"],"correctAnswer":"🍎 Apple"},
                  {"id":2,"type":"VOCABULARY","title":"2. Vocabulary","description":"Choose opposite of 'Hot':","options":["Cold","Warm","Big"],"correctAnswer":"Cold"},
                  {"id":3,"type":"PHONICS","title":"3. Letter Sound","description":"Which word starts with /p/?","options":["Pig","Big","Dig"],"correctAnswer":"Pig"},
                  {"id":4,"type":"SORT_WORDS","title":"4. Sentence Building","description":"Arrange words into a sentence:","wordsForSort":["like","apples","I"],"correctAnswer":"I like apples"},
                  {"id":5,"type":"VOCABULARY","title":"5. Grammar","description":"She ___ to school every day.","options":["walks","walked","walking"],"correctAnswer":"walks"},
                  {"id":6,"type":"LISTENING_EMOJI","title":"6. Listen and Choose","description":"Select the animal:","voicePrompt":"cat","options":["🐶 Dog","🐱 Cat","🐰 Rabbit"],"correctAnswer":"🐱 Cat"},
                  {"id":7,"type":"VOCABULARY","title":"7. Antonym","description":"The rabbit is fast, but the turtle is ___","options":["slow","quick","tall"],"correctAnswer":"slow"},
                  {"id":8,"type":"VOCABULARY","title":"8. Idiom","description":"What does 'A piece of cake' mean?","options":["Very easy","Delicious dessert","Hard problem"],"correctAnswer":"Very easy"},
                  {"id":9,"type":"SORT_WORDS","title":"9. Sentence Ordering","description":"Arrange into sentence:","wordsForSort":["play","on","We","football","Sunday"],"correctAnswer":"We play football on Sunday"},
                  {"id":10,"type":"SPEAK_ALOUD","title":"10. Read Aloud","description":"Read aloud:","voicePrompt":"Practice makes perfect every day."}
                ]"""
            else -> "Well done! Let's keep moving forward!"
        }
    }
}
