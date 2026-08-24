package org.akj.lingo.learn.domain.usecase

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AgentPromptRegistryTest {

    private val registry = AgentPromptRegistry()

    @Test
    fun `renders plan with all tokens substituted`() {
        val rendered = registry.render(
            "PLAN",
            mapOf(
                "grade" to "Grade 4",
                "accuracy" to "75",
                "weak_categories" to "Vocabulary, Listening",
                "milestones" to "3-day streak",
                "coefficient" to "1.2",
                "duration" to "15"
            )
        )
        assertTrue(rendered.contains("Grade 4"))
        assertTrue(rendered.contains("Accuracy: 75%"))
        assertTrue(rendered.contains("Vocabulary, Listening"))
        assertTrue(rendered.contains("difficulty_coefficient\": 1.2"))
        // No placeholder must survive substitution.
        assertTrue(!rendered.contains("{grade}"))
        assertTrue(!rendered.contains("{coefficient}"))
    }

    @Test
    fun `renders diagnosis with band tokens`() {
        val rendered = registry.render(
            "DIAGNOSIS",
            mapOf(
                "band" to "Primary",
                "grade" to "Grade 3",
                "coefficient" to "1.0",
                "vocabulary_range" to "300-500",
                "max_words" to "8"
            )
        )
        assertTrue(rendered.contains("Primary students"))
        assertTrue(rendered.contains("Grade 3"))
        assertTrue(rendered.contains("Max words per sentence: 8"))
        assertTrue(rendered.contains("LISTEN_AND_TYPE"))
    }

    @Test
    fun `renders free-text task without placeholders`() {
        val rendered = registry.render("PING", emptyMap())
        assertTrue(rendered.contains("reply 'OK'"))
    }

    @Test
    fun `missing value throws with placeholder names`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            registry.render("PLAN", mapOf("grade" to "Grade 4"))
        }
        assertTrue(ex.message!!.contains("PLAN"))
        assertTrue(ex.message!!.contains("coefficient"))
    }

    @Test
    fun `unexpected value throws`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            registry.render("PING", mapOf("grade" to "Grade 4"))
        }
        assertTrue(ex.message!!.contains("grade"))
    }

    @Test
    fun `unknown task type throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            registry.render("UNKNOWN_TASK", emptyMap())
        }
    }

    @Test
    fun `every structured task type has a version`() {
        listOf("PLAN", "DIAGNOSIS", "EXPLAIN", "ENCOURAGEMENT", "LINGO_LETTER", "ROLEPLAY_SCENARIO", "HINT", "PING")
            .forEach { taskType ->
                val version = registry.version(taskType)
                assertTrue(version != null && version >= 1, "$taskType should have a version")
            }
    }
}
