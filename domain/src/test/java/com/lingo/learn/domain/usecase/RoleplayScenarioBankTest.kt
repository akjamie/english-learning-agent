package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.repository.LlmRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for [RoleplayScenarioBank] — Sprint 11 scenario bank.
 */
class RoleplayScenarioBankTest {

    private val bank = RoleplayScenarioBank(
        StructuredLlmUseCase(
            llmRepository = object : LlmRepository {
                override suspend fun complete(prompt: String, taskType: String, maxTokens: Int): Result<String> =
                    Result.failure(Exception("not configured"))

                override suspend fun chat(messages: List<org.akj.lingo.learn.domain.model.ChatMessage>, taskType: String, maxTokens: Int): Result<String> =
                    Result.failure(Exception("not configured"))

                override fun completeStream(prompt: String, taskType: String, maxTokens: Int): kotlinx.coroutines.flow.Flow<String> =
                    kotlinx.coroutines.flow.emptyFlow()
            },
            jsonValidator = AgentJsonValidator()
        ),
        AgentPromptRegistry(),
        ContentGuard()
    )

    @Test
    fun `curated scenarios includes all nine scenes`() {
        val scenarios = bank.curatedScenarios()
        assertEquals(9, scenarios.size)
        val ids = scenarios.map { it.id }.toSet()
        assertTrue(ids.containsAll(listOf("zoo", "restaurant", "school", "travel", "doctor", "supermarket", "birthday", "library", "weather")))
    }

    @Test
    fun `getScenario returns matching scenario`() {
        val school = bank.getScenario("school")
        assertEquals("school", school.id)
        assertEquals("School Day", school.title)
    }

    @Test
    fun `getScenario falls back to zoo for unknown id`() {
        val fallback = bank.getScenario("unknown")
        assertEquals("zoo", fallback.id)
    }

    @Test
    fun `each scenario has offline-safe prompt and opening line`() {
        bank.curatedScenarios().forEach { s ->
            assertTrue(s.systemPrompt.isNotBlank(), "${s.id} system prompt blank")
            assertTrue(s.openingLine.isNotBlank(), "${s.id} opening line blank")
            assertTrue(s.targetWords.isNotEmpty(), "${s.id} target words empty")
        }
    }

    @Test
    fun `initial messages contains system and assistant opening`() {
        val messages = bank.initialMessages(bank.getScenario("restaurant"))
        assertEquals(2, messages.size)
        assertEquals("system", messages[0].role)
        assertEquals("assistant", messages[1].role)
        assertEquals(bank.getScenario("restaurant").openingLine, messages[1].content)
    }
}
