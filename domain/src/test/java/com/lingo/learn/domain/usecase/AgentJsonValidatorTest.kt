package org.akj.lingo.learn.domain.usecase

import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AgentJsonValidatorTest {

    private val validator = AgentJsonValidator()

    @Test
    fun `valid plan passes`() {
        val json = """
            {
              "theme": "School Life",
              "difficulty_coefficient": 1.2,
              "days": [
                {
                  "day": 1,
                  "focus": "Vocabulary",
                  "target_words": ["apple", "book"],
                  "reference_sentence": "I see an apple.",
                  "duration_minutes": 15,
                  "rationale": "Builds on last week's listening focus."
                }
              ]
            }
        """.trimIndent()
        assertInstanceOf(AgentJsonValidator.ValidationResult.Valid::class.java, validator.validate("PLAN", json))
    }

    @Test
    fun `plan missing theme is invalid`() {
        val json = """{"days": [{"day": 1, "focus": "V", "target_words": ["a"], "reference_sentence": "s", "duration_minutes": 15, "rationale": "r"}]}"""
        val result = validator.validate("PLAN", json)
        assertInstanceOf(AgentJsonValidator.ValidationResult.Invalid::class.java, result)
        assertTrue((result as AgentJsonValidator.ValidationResult.Invalid).reason.contains("theme"))
    }

    @Test
    fun `plan day missing target_words is invalid`() {
        val json = """{"theme": "T", "days": [{"day": 1, "focus": "V", "reference_sentence": "s", "duration_minutes": 15, "rationale": "r"}]}"""
        val result = validator.validate("PLAN", json)
        assertInstanceOf(AgentJsonValidator.ValidationResult.Invalid::class.java, result)
        assertTrue((result as AgentJsonValidator.ValidationResult.Invalid).reason.contains("target_words"))
    }

    @Test
    fun `plan empty days array is invalid`() {
        val json = """{"theme": "T", "days": []}"""
        assertInstanceOf(AgentJsonValidator.ValidationResult.Invalid::class.java, validator.validate("PLAN", json))
    }

    @Test
    fun `plan not json is invalid`() {
        val result = validator.validate("PLAN", "not json at all")
        assertInstanceOf(AgentJsonValidator.ValidationResult.Invalid::class.java, result)
    }

    @Test
    fun `valid diagnosis passes`() {
        val json = """
            [
              {
                "id": 1,
                "type": "VOCABULARY",
                "title": "1. Choose",
                "description": "Pick the word",
                "options": ["apple", "book"],
                "correctAnswer": "apple"
              }
            ]
        """.trimIndent()
        assertInstanceOf(AgentJsonValidator.ValidationResult.Valid::class.java, validator.validate("DIAGNOSIS", json))
    }

    @Test
    fun `diagnosis question missing type is invalid`() {
        val json = """[{"id": 1, "title": "t", "description": "d", "correctAnswer": "a"}]"""
        val result = validator.validate("DIAGNOSIS", json)
        assertInstanceOf(AgentJsonValidator.ValidationResult.Invalid::class.java, result)
        assertTrue((result as AgentJsonValidator.ValidationResult.Invalid).reason.contains("type"))
    }

    @Test
    fun `diagnosis empty array is invalid`() {
        val json = "[]"
        assertInstanceOf(AgentJsonValidator.ValidationResult.Invalid::class.java, validator.validate("DIAGNOSIS", json))
    }

    @Test
    fun `valid roleplay passes`() {
        val json = """{"system_prompt": "You are Lingo at a zoo.", "opening_line": "Welcome!"}"""
        assertInstanceOf(AgentJsonValidator.ValidationResult.Valid::class.java, validator.validate("ROLEPLAY_SCENARIO", json))
    }

    @Test
    fun `roleplay missing opening_line is invalid`() {
        val json = """{"system_prompt": "You are Lingo at a zoo."}"""
        val result = validator.validate("ROLEPLAY_SCENARIO", json)
        assertInstanceOf(AgentJsonValidator.ValidationResult.Invalid::class.java, result)
        assertTrue((result as AgentJsonValidator.ValidationResult.Invalid).reason.contains("opening_line"))
    }

    @Test
    fun `free-text task types are always valid`() {
        assertInstanceOf(
            AgentJsonValidator.ValidationResult.Valid::class.java,
            validator.validate("ENCOURAGEMENT", "Great job!")
        )
    }
}
