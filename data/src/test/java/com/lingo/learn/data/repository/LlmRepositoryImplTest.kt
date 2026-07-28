package org.akj.lingo.learn.data.repository

import org.akj.lingo.learn.data.local.dao.TokenUsageLogDao
import org.akj.lingo.learn.data.prefs.SecureConfigPrefs
import org.akj.lingo.learn.data.remote.minimax.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

class LlmRepositoryImplTest {

    private lateinit var service: MinimaxService
    private lateinit var prefs: SecureConfigPrefs
    private lateinit var tokenUsageLogDao: TokenUsageLogDao
    private lateinit var repository: LlmRepositoryImpl

    private val baseUrl = "https://ark.cn-beijing.volces.com"
    private val llmEndpoint = "/v1/chat/completions"
    private val fullUrl = "$baseUrl$llmEndpoint"
    private val authToken = "valid-token-12345"
    private val apiKey = "Bearer $authToken"
    private val groupId = "group-123"
    private val primaryModel = "claude-3-5-sonnet"
    private val fallbackModel = "deepseek-v4-flash"

    @BeforeEach
    fun setup() {
        service = Mockito.mock(MinimaxService::class.java)
        prefs = Mockito.mock(SecureConfigPrefs::class.java)
        tokenUsageLogDao = Mockito.mock(TokenUsageLogDao::class.java)
        repository = LlmRepositoryImpl(service, prefs, tokenUsageLogDao)

        whenever(prefs.getBaseUrl()).thenReturn(baseUrl)
        whenever(prefs.getLlmEndpoint()).thenReturn(llmEndpoint)
        whenever(prefs.getAuthToken()).thenReturn(authToken)
        whenever(prefs.getGroupId()).thenReturn(groupId)
        whenever(prefs.getPrimaryModel()).thenReturn(primaryModel)
        whenever(prefs.getFallbackModel()).thenReturn(fallbackModel)
        whenever(prefs.getMonthlyTokenLimit()).thenReturn(1_000_000)
        runBlocking {
            whenever(tokenUsageLogDao.getMonthlyTotalTokens(any())).thenReturn(0)
            whenever(tokenUsageLogDao.insertLog(any())).thenReturn(Unit)
        }
    }

    @Test
    fun `complete returns failure when auth token too short`() {
        whenever(prefs.getAuthToken()).thenReturn("short")

        val result = runBlocking { repository.complete("Hello", "PLAN") }

        assertTrue(result.isFailure)
    }

    @Test
    fun `complete returns failure when group id empty`() {
        whenever(prefs.getGroupId()).thenReturn("")

        val result = runBlocking { repository.complete("Hello", "PLAN") }

        assertTrue(result.isFailure)
    }

    @Test
    fun `complete returns fallback template when budget exceeded`() = runBlocking {
        whenever(prefs.getMonthlyTokenLimit()).thenReturn(0)

        val result = repository.complete("Hello", "PLAN")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().contains("Well done"))
    }

    @Test
    fun `complete parses OpenAI-compatible response format`() = runBlocking {
        val responseBody = MinimaxChatResponse(
            choices = listOf(MinimaxChoice(
                message = MinimaxMessage(role = "assistant", content = "{\"theme\": \"School Life\", \"days\": []}"),
                finishReason = "stop"
            )),
            content = null,
            usage = MinimaxUsage(inputTokens = 10, outputTokens = 20, totalTokens = 30)
        )
        stubPrimaryCall("Generate plan", responseBody)

        val result = repository.complete("Generate plan", "PLAN")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().contains("School Life"))
    }

    @Test
    fun `complete parses Claude native response format via Gson tree`() = runBlocking {
        val claudeJson = """{"content":[{"type":"text","text":"{\"theme\": \"Animals\", \"days\": []}"}],"usage":{"input_tokens":5,"output_tokens":10}}"""
        val gson = com.google.gson.Gson()
        val responseBody = gson.fromJson(claudeJson, MinimaxChatResponse::class.java)
        stubPrimaryCall("Generate plan", responseBody)

        val result = repository.complete("Generate plan", "PLAN")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().contains("Animals"))
    }

    @Test
    fun `complete falls back to secondary model when primary fails`() = runBlocking {
        val errorResponse: Response<MinimaxChatResponse> = Response.error(500, okhttp3.ResponseBody.create(null, "Server Error"))
        val successResponse = Response.success(MinimaxChatResponse(
            choices = listOf(MinimaxChoice(
                message = MinimaxMessage(role = "assistant", content = "Fallback response"),
                finishReason = "stop"
            )),
            content = null,
            usage = null
        ))
        whenever(service.chatCompletion(any(), any(), any(), any()))
            .thenReturn(errorResponse)
            .thenReturn(successResponse)

        val result = repository.complete("Hello", "PING")

        assertTrue(result.isSuccess)
        assertEquals("Fallback response", result.getOrThrow())
    }

    @Test
    fun `complete returns local template when both models fail`() = runBlocking {
        val error1: Response<MinimaxChatResponse> = Response.error(500, okhttp3.ResponseBody.create(null, "Error"))
        val error2: Response<MinimaxChatResponse> = Response.error(503, okhttp3.ResponseBody.create(null, "Unavailable"))
        whenever(service.chatCompletion(any(), any(), any(), any()))
            .thenReturn(error1)
            .thenReturn(error2)

        val result = repository.complete("Hello", "PING")

        assertTrue(result.isSuccess)
        assertEquals("OK", result.getOrThrow())
    }

    @Test
    fun `complete handles empty choices gracefully`() = runBlocking {
        val responseBody = MinimaxChatResponse(choices = emptyList(), content = null, usage = null)
        stubPrimaryCall("Hello", responseBody)

        val result = repository.complete("Hello", "PING")

        assertTrue(result.isSuccess)
        assertEquals("OK", result.getOrThrow())
    }

    @Test
    fun `complete records token usage for primary model`() = runBlocking {
        val responseBody = MinimaxChatResponse(
            choices = listOf(MinimaxChoice(
                message = MinimaxMessage(role = "assistant", content = "Response"),
                finishReason = "stop"
            )),
            content = null,
            usage = MinimaxUsage(inputTokens = 10, outputTokens = 20, totalTokens = 30)
        )
        stubPrimaryCall("Hello", responseBody)

        repository.complete("Hello", "PLAN")

        verify(tokenUsageLogDao).insertLog(argThat {
            model == "primary" && inputTokens == 10 && outputTokens == 20 && totalTokens == 30
        })
    }

    @Test
    fun `complete records token usage for fallback model`() = runBlocking {
        val errorResponse: Response<MinimaxChatResponse> = Response.error(500, okhttp3.ResponseBody.create(null, "Error"))
        val successResponse = Response.success(MinimaxChatResponse(
            choices = listOf(MinimaxChoice(
                message = MinimaxMessage(role = "assistant", content = "Fallback"),
                finishReason = "stop"
            )),
            content = null,
            usage = MinimaxUsage(promptTokens = 5, completionTokens = 15, totalTokens = 20)
        ))
        whenever(service.chatCompletion(any(), any(), any(), any()))
            .thenReturn(errorResponse)
            .thenReturn(successResponse)

        repository.complete("Hello", "PLAN")

        verify(tokenUsageLogDao).insertLog(argThat {
            model == "fallback" && inputTokens == 5 && outputTokens == 15 && totalTokens == 20
        })
    }

    @Test
    fun `complete handles Claude usage format with prompt_tokens and completion_tokens`() = runBlocking {
        val claudeJson = """{"content":[{"type":"text","text":"Hello"}],"usage":{"prompt_tokens":7,"completion_tokens":13,"total_tokens":20}}"""
        val gson = com.google.gson.Gson()
        val responseBody = gson.fromJson(claudeJson, MinimaxChatResponse::class.java)
        stubPrimaryCall("Hello", responseBody)

        repository.complete("Hello", "PLAN")

        verify(tokenUsageLogDao).insertLog(argThat {
            inputTokens == 7 && outputTokens == 13 && totalTokens == 20
        })
    }

    @Test
    fun `complete returns ENCOURAGEMENT template for that task type`() = runBlocking {
        whenever(prefs.getMonthlyTokenLimit()).thenReturn(0)

        val result = repository.complete("You did great!", "ENCOURAGEMENT")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().contains("Great job"))
    }

    @Test
    fun `complete returns REPORT template for that task type`() = runBlocking {
        whenever(prefs.getMonthlyTokenLimit()).thenReturn(0)

        val result = repository.complete("Generate report", "REPORT")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().contains("Weekly learning"))
    }

    // --- Helpers ---

    private suspend fun stubPrimaryCall(prompt: String, responseBody: MinimaxChatResponse) {
        whenever(service.chatCompletion(any(), any(), any(), any())).thenReturn(Response.success(responseBody))
    }

    private suspend fun stubPrimaryError(prompt: String, code: Int, body: String) {
        whenever(service.chatCompletion(any(), any(), any(), any()))
            .thenReturn(Response.error(code, okhttp3.ResponseBody.create(null, body)))
    }
}
