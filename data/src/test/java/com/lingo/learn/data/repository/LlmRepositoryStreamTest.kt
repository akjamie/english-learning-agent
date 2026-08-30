package org.akj.lingo.learn.data.repository

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.akj.lingo.learn.data.local.dao.LlmTraceDao
import org.akj.lingo.learn.data.local.dao.TokenUsageLogDao
import org.akj.lingo.learn.data.prefs.SecureConfigPrefs
import org.akj.lingo.learn.data.remote.minimax.MinimaxChatResponse
import org.akj.lingo.learn.data.remote.minimax.MinimaxChoice
import org.akj.lingo.learn.data.remote.minimax.MinimaxMessage
import org.akj.lingo.learn.data.remote.minimax.MinimaxService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import retrofit2.Response

class LlmRepositoryStreamTest {

    private lateinit var service: MinimaxService
    private lateinit var prefs: SecureConfigPrefs
    private lateinit var tokenUsageLogDao: TokenUsageLogDao
    private lateinit var llmTraceDao: LlmTraceDao
    private lateinit var repository: LlmRepositoryImpl

    private val baseUrl = "https://ark.cn-beijing.volces.com"
    private val authToken = "valid-token-12345"

    @BeforeEach
    fun setup() {
        service = Mockito.mock(MinimaxService::class.java)
        prefs = Mockito.mock(SecureConfigPrefs::class.java)
        tokenUsageLogDao = Mockito.mock(TokenUsageLogDao::class.java)
        llmTraceDao = Mockito.mock(LlmTraceDao::class.java)
        repository = LlmRepositoryImpl(service, prefs, tokenUsageLogDao, llmTraceDao)

        whenever(prefs.getBaseUrl()).thenReturn(baseUrl)
        whenever(prefs.getAuthToken()).thenReturn(authToken)
        whenever(prefs.getGroupId()).thenReturn("group-1")
        whenever(prefs.getPrimaryModel()).thenReturn("model-primary")
        whenever(prefs.getFallbackModel()).thenReturn("model-fallback")
        whenever(prefs.getMonthlyTokenLimit()).thenReturn(1_000_000)

        runBlocking {
            whenever(tokenUsageLogDao.getMonthlyTotalTokens(any())).thenReturn(0)
            whenever(tokenUsageLogDao.insertLog(any())).thenReturn(Unit)
            whenever(llmTraceDao.insertTrace(any())).thenReturn(Unit)
        }
    }

    @Test
    fun `completeStream parses SSE chunks and emits deltas`() = runBlocking {
        val sseBody = """
            data: {"choices":[{"delta":{"content":"Hello"}}]}
            
            data: {"choices":[{"delta":{"content":" world"}}]}
            
            data: [DONE]
        """.trimIndent().toResponseBody("text/event-stream".toMediaTypeOrNull())

        whenever(service.chatCompletionStream(any(), any(), any(), any()))
            .thenReturn(Response.success(sseBody))

        val chunks = repository.completeStream("prompt", "HINT").toList()
        assertEquals(listOf("Hello", " world"), chunks)
    }

    @Test
    fun `completeStream falls back to complete when stream returns error`() = runBlocking {
        whenever(service.chatCompletionStream(any(), any(), any(), any()))
            .thenReturn(Response.error(500, "Server error".toResponseBody("text/plain".toMediaTypeOrNull())))

        val singleResponse = MinimaxChatResponse(
            choices = listOf(
                MinimaxChoice(
                    message = MinimaxMessage(role = "assistant", content = "Fallback answer"),
                    finishReason = "stop"
                )
            ),
            content = null,
            usage = null
        )
        whenever(service.chatCompletion(any(), any(), any(), any()))
            .thenReturn(Response.success(singleResponse))

        val chunks = repository.completeStream("prompt", "HINT").toList()
        assertEquals(listOf("Fallback answer"), chunks)
    }
}
