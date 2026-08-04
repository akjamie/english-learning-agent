package org.akj.lingo.learn.domain.usecase

import kotlinx.coroutines.test.runTest
import org.akj.lingo.learn.domain.repository.LlmRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DailyEncouragerUseCaseTest {

    private class CapturingLlmRepository(
        private val result: Result<String> = Result.success("Great job!")
    ) : LlmRepository {
        var lastPrompt: String? = null
        var lastTaskType: String? = null
        var lastMaxTokens: Int? = null

        override suspend fun complete(prompt: String, taskType: String, maxTokens: Int): Result<String> {
            lastPrompt = prompt
            lastTaskType = taskType
            lastMaxTokens = maxTokens
            return result
        }

        override suspend fun chat(messages: List<org.akj.lingo.learn.domain.model.ChatMessage>, taskType: String, maxTokens: Int): Result<String> =
            result
    }

    @Test
    fun `prompt includes student name`() = runTest {
        val fake = CapturingLlmRepository()
        val useCase = DailyEncouragerUseCase(fake)
        useCase.invoke(name = "Emma", streak = 5)
        assertTrue(fake.lastPrompt!!.contains("Emma"))
    }

    @Test
    fun `prompt includes streak count`() = runTest {
        val fake = CapturingLlmRepository()
        val useCase = DailyEncouragerUseCase(fake)
        useCase.invoke(name = "Tom", streak = 12)
        assertTrue(fake.lastPrompt!!.contains("12"))
        assertTrue(fake.lastPrompt!!.contains("streak"))
    }

    @Test
    fun `uses ENCOURAGEMENT task type`() = runTest {
        val fake = CapturingLlmRepository()
        val useCase = DailyEncouragerUseCase(fake)
        useCase.invoke(name = "Lily", streak = 3)
        assertEquals("ENCOURAGEMENT", fake.lastTaskType)
    }

    @Test
    fun `sets maxTokens to 60`() = runTest {
        val fake = CapturingLlmRepository()
        val useCase = DailyEncouragerUseCase(fake)
        useCase.invoke(name = "Jack", streak = 0)
        assertEquals(60, fake.lastMaxTokens)
    }

    @Test
    fun `prompt includes tone rules`() = runTest {
        val fake = CapturingLlmRepository()
        val useCase = DailyEncouragerUseCase(fake)
        useCase.invoke(name = "Mia", streak = 7)
        val prompt = fake.lastPrompt!!
        assertTrue(prompt.contains("Enthusiastic"))
        assertTrue(prompt.contains("40 characters"))
        assertTrue(prompt.contains("emoji"))
    }

    @Test
    fun `returns LLM success result`() = runTest {
        val fake = CapturingLlmRepository(result = Result.success("Keep going!"))
        val useCase = DailyEncouragerUseCase(fake)
        val result = useCase.invoke(name = "Sam", streak = 10)
        assertTrue(result.isSuccess)
        assertEquals("Keep going!", result.getOrNull())
    }

    @Test
    fun `returns LLM failure result`() = runTest {
        val fake = CapturingLlmRepository(result = Result.failure(Exception("Network error")))
        val useCase = DailyEncouragerUseCase(fake)
        val result = useCase.invoke(name = "Sam", streak = 10)
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()!!.message)
    }

    @Test
    fun `handles zero streak`() = runTest {
        val fake = CapturingLlmRepository()
        val useCase = DailyEncouragerUseCase(fake)
        useCase.invoke(name = "NewUser", streak = 0)
        assertTrue(fake.lastPrompt!!.contains("0"))
    }
}
