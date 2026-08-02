package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.model.ChatMessage
import org.akj.lingo.learn.domain.model.RoleplayScenario
import org.akj.lingo.learn.domain.repository.LlmRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sprint 11 — Roleplay scenario bank.
 *
 * Provides curated offline-safe scenarios (zoo / restaurant / school / travel)
 * plus optional LLM enrichment. The curated scripts always work, so Roleplay
 * never depends on a configured model.
 */
@Singleton
class RoleplayScenarioBank @Inject constructor(
    private val llmRepository: LlmRepository
) {

    /** All curated scenarios, in display order. */
    fun curatedScenarios(): List<RoleplayScenario> = listOf(
        zoo(),
        restaurant(),
        school(),
        travel()
    )

    /** Returns the curated scenario for an id, falling back to the zoo one. */
    fun getScenario(id: String?): RoleplayScenario =
        curatedScenarios().firstOrNull { it.id == id } ?: zoo()

    /**
     * Optionally asks the LLM for an enriched system prompt + opening line for a
     * scenario. On any failure (no model / offline / parse error) returns null so
     * the caller keeps the curated offline-safe script.
     */
    suspend fun enrichWithLlm(scenario: RoleplayScenario, grade: String): RoleplayScenario? {
        val prompt = """
            You are Lingo, a friendly fox tutor running an English roleplay session with a child
            (grade $grade).

            --- Scenario ---
            Setting: ${scenario.title} (${scenario.id})
            Today's vocabulary: ${scenario.targetWords.joinToString()}

            --- Output Format ---
            Output a raw JSON object ONLY, no markdown:
            {
              "system_prompt": "full system prompt for the scenario (short, playful, max 5 lines)",
              "opening_line": "Lingo's first greeting line (max 8 words)"
            }
        """.trimIndent()

        val result = llmRepository.complete(prompt, taskType = "ROLEPLAY_SCENARIO", maxTokens = 300)
        val json = result.getOrNull() ?: return null
        return try {
            val obj = org.json.JSONObject(json)
            val systemPrompt = obj.optString("system_prompt").takeIf { it.isNotBlank() }
            val openingLine = obj.optString("opening_line").takeIf { it.isNotBlank() }
            if (systemPrompt == null || openingLine == null) null
            else scenario.copy(systemPrompt = systemPrompt, openingLine = openingLine)
        } catch (_: Exception) {
            null
        }
    }

    /** Builds the ChatMessage list for a scenario (system + assistant opening). */
    fun initialMessages(scenario: RoleplayScenario): List<ChatMessage> = listOf(
        ChatMessage(role = "system", content = scenario.systemPrompt),
        ChatMessage(role = "assistant", content = scenario.openingLine)
    )

    private fun zoo() = RoleplayScenario(
        id = "zoo",
        title = "Zoo Visit",
        emoji = "🦁",
        description = "Meet the animals at the zoo!",
        targetWords = listOf("lion", "elephant", "monkey", "big", "see"),
        systemPrompt = """
            You are Lingo Fox at a zoo. The child is visiting today.
            Keep answers very short (1-2 sentences), simple English for a 4th grader.
            Guide the child to say animal words like: lion, elephant, monkey.
            Always be encouraging and cheerful!
        """.trimIndent(),
        openingLine = "Welcome to the zoo! What animal do you see?"
    )

    private fun restaurant() = RoleplayScenario(
        id = "restaurant",
        title = "At the Restaurant",
        emoji = "🍔",
        description = "Order food like a pro!",
        targetWords = listOf("menu", "please", "food", "delicious", "thank you"),
        systemPrompt = """
            You are Lingo Fox, a waiter at a restaurant. The child is ordering food.
            Keep answers very short (1-2 sentences), simple English for a 4th grader.
            Guide the child to use: menu, please, delicious, thank you.
            Always be encouraging and cheerful!
        """.trimIndent(),
        openingLine = "Hello! Here is the menu. What would you like?"
    )

    private fun school() = RoleplayScenario(
        id = "school",
        title = "School Day",
        emoji = "🏫",
        description = "Talk about your school!",
        targetWords = listOf("teacher", "friend", "class", "book", "learn"),
        systemPrompt = """
            You are Lingo Fox, a new student at the child's school.
            Keep answers very short (1-2 sentences), simple English for a 4th grader.
            Guide the child to talk about: teacher, friend, class, book, learn.
            Always be encouraging and cheerful!
        """.trimIndent(),
        openingLine = "Hi! I'm new here. What is your favorite class?"
    )

    private fun travel() = RoleplayScenario(
        id = "travel",
        title = "Travel Adventure",
        emoji = "✈️",
        description = "Plan a fun trip!",
        targetWords = listOf("ticket", "plane", "map", "hotel", "fun"),
        systemPrompt = """
            You are Lingo Fox going on a trip with the child.
            Keep answers very short (1-2 sentences), simple English for a 4th grader.
            Guide the child to use: ticket, plane, map, hotel, fun.
            Always be encouraging and cheerful!
        """.trimIndent(),
        openingLine = "Let's go on a trip! Where should we fly?"
    )
}
