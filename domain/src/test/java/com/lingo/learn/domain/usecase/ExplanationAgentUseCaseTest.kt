package org.akj.lingo.learn.domain.usecase

import kotlinx.coroutines.test.runTest
import org.akj.lingo.learn.domain.repository.LlmRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExplanationAgentUseCaseTest {

    private class CapturingLlmRepository(
        private val result: Result<String> = Result.success("explanation text")
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
    fun `single-error invoke includes word, grade, and error type in prompt`() = runTest {
        val fake = CapturingLlmRepository()
        val useCase = ExplanationAgentUseCase(fake, AgentPromptRegistry())
        useCase.invoke(word = "classroom", errorType = "spelling", grade = "Grade 4")
        val prompt = fake.lastPrompt!!
        assertTrue(prompt.contains("classroom"))
        assertTrue(prompt.contains("Grade 4"))
        assertTrue(prompt.contains("spelling"))
    }

    @Test
    fun `single-error invoke uses EXPLAIN task type`() = runTest {
        val fake = CapturingLlmRepository()
        val useCase = ExplanationAgentUseCase(fake, AgentPromptRegistry())
        useCase.invoke(word = "hear", errorType = "listening", grade = "Grade 3")
        assertEquals("EXPLAIN", fake.lastTaskType)
    }

    @Test
    fun `single-error invoke sets maxTokens to 150`() = runTest {
        val fake = CapturingLlmRepository()
        val useCase = ExplanationAgentUseCase(fake, AgentPromptRegistry())
        useCase.invoke(word = "school", errorType = "pronunciation", grade = "Grade 5")
        assertEquals(150, fake.lastMaxTokens)
    }

    @Test
    fun `invoke with error history includes JSON in prompt`() = runTest {
        val fake = CapturingLlmRepository()
        val useCase = ExplanationAgentUseCase(fake, AgentPromptRegistry())
        val historyJson = """{"errorCount":3,"questionTypes":["listening","spelling"]}"""
        useCase.invoke(word = "library", errorType = "spelling", grade = "Grade 4", errorHistoryJson = historyJson)
        val prompt = fake.lastPrompt!!
        assertTrue(prompt.contains(historyJson))
        assertTrue(prompt.contains("library"))
    }

    @Test
    fun `invoke without history uses simple error description`() = runTest {
        val fake = CapturingLlmRepository()
        val useCase = ExplanationAgentUseCase(fake, AgentPromptRegistry())
        useCase.invoke(word = "book", errorType = "reading", grade = "Grade 2")
        val prompt = fake.lastPrompt!!
        assertTrue(prompt.contains("The child got this word wrong in a quiz due to: reading"))
    }

    @Test
    fun `invoke returns LLM success result`() = runTest {
        val fake = CapturingLlmRepository(result = Result.success("Great explanation!"))
        val useCase = ExplanationAgentUseCase(fake, AgentPromptRegistry())
        val result = useCase.invoke(word = "test", errorType = "spelling", grade = "Grade 4")
        assertTrue(result.isSuccess)
        assertEquals("Great explanation!", result.getOrNull())
    }

    @Test
    fun `invoke returns LLM failure result`() = runTest {
        val fake = CapturingLlmRepository(result = Result.failure(Exception("LLM unavailable")))
        val useCase = ExplanationAgentUseCase(fake, AgentPromptRegistry())
        val result = useCase.invoke(word = "test", errorType = "spelling", grade = "Grade 4")
        assertTrue(result.isFailure)
        assertEquals("LLM unavailable", result.exceptionOrNull()!!.message)
    }

    @Test
    fun `prompt includes guidelines section`() = runTest {
        val fake = CapturingLlmRepository()
        val useCase = ExplanationAgentUseCase(fake, AgentPromptRegistry())
        useCase.invoke(word = "write", errorType = "grammar", grade = "Grade 6")
        val prompt = fake.lastPrompt!!
        assertTrue(prompt.contains("encouraging"))
        assertTrue(prompt.contains("mnemonic"))
        assertTrue(prompt.contains("100 words"))
    }
}
