package org.akj.lingo.learn.domain.usecase

import kotlinx.coroutines.test.runTest
import org.akj.lingo.learn.domain.model.ChatMessage
import org.akj.lingo.learn.domain.repository.LlmRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StructuredLlmUseCaseTest {

    private val validator = AgentJsonValidator()

    /** Fake LLM that serves queued responses and records every prompt it received. */
    private class FakeLlm(private val responses: MutableList<Result<String>>) : LlmRepository {
        val receivedPrompts = mutableListOf<String>()

        override suspend fun complete(prompt: String, taskType: String, maxTokens: Int): Result<String> {
            receivedPrompts.add(prompt)
            return responses.removeAt(0)
        }

        override suspend fun chat(messages: List<ChatMessage>, taskType: String, maxTokens: Int): Result<String> {
            throw UnsupportedOperationException("not used")
        }

        override fun completeStream(prompt: String, taskType: String, maxTokens: Int): kotlinx.coroutines.flow.Flow<String> =
            kotlinx.coroutines.flow.emptyFlow()
    }

    @Test
    fun `returns valid response on first attempt`() = runTest {
        val validPlan = """{"theme":"T","days":[{"day":1,"focus":"V","target_words":["a"],"reference_sentence":"s","duration_minutes":15,"rationale":"r"}]}"""
        val llm = FakeLlm(mutableListOf(Result.success(validPlan)))
        val useCase = StructuredLlmUseCase(llm, validator)

        val result = useCase.completeJson("prompt", "PLAN", maxTokens = 1500)

        assertTrue(result.isSuccess)
        assertEquals(validPlan, result.getOrThrow())
        assertEquals(1, llm.receivedPrompts.size)
    }

    @Test
    fun `repairs invalid json with feedback then succeeds`() = runTest {
        val invalid = """{"theme":"T"}"""
        val valid = """{"theme":"T","days":[{"day":1,"focus":"V","target_words":["a"],"reference_sentence":"s","duration_minutes":15,"rationale":"r"}]}"""
        val llm = FakeLlm(mutableListOf(Result.success(invalid), Result.success(valid)))
        val useCase = StructuredLlmUseCase(llm, validator)

        val result = useCase.completeJson("original", "PLAN", maxTokens = 1500)

        assertTrue(result.isSuccess)
        assertEquals(valid, result.getOrThrow())
        assertEquals(2, llm.receivedPrompts.size)
        // The repair call must carry the validation reason so the model can fix its output.
        assertTrue(llm.receivedPrompts[1].contains("original"))
        assertTrue(llm.receivedPrompts[1].contains("days"))
    }

    @Test
    fun `fails after repair attempts exhausted`() = runTest {
        val invalid = """{"theme":"T"}"""
        val llm = FakeLlm(mutableListOf(Result.success(invalid), Result.success(invalid)))
        val useCase = StructuredLlmUseCase(llm, validator)

        val result = useCase.completeJson("original", "PLAN", maxTokens = 1500)

        assertTrue(result.isFailure)
        assertEquals(2, llm.receivedPrompts.size)
        assertTrue(result.exceptionOrNull()?.message?.contains("days") == true)
    }

    @Test
    fun `propagates llm failure without repair retry`() = runTest {
        val llm = FakeLlm(mutableListOf(Result.failure(Exception("model down"))))
        val useCase = StructuredLlmUseCase(llm, validator)

        val result = useCase.completeJson("prompt", "PLAN", maxTokens = 1500)

        assertTrue(result.isFailure)
        assertEquals("model down", result.exceptionOrNull()?.message)
        assertEquals(1, llm.receivedPrompts.size)
    }

    @Test
    fun `honors custom max repair attempts`() = runTest {
        val invalid = """{"theme":"T"}"""
        val llm = FakeLlm(
            mutableListOf(Result.success(invalid), Result.success(invalid), Result.success(invalid))
        )
        val useCase = StructuredLlmUseCase(llm, validator)

        val result = useCase.completeJson("prompt", "PLAN", maxTokens = 1500, maxRepairAttempts = 3)

        assertTrue(result.isFailure)
        assertEquals(3, llm.receivedPrompts.size)
    }
}
