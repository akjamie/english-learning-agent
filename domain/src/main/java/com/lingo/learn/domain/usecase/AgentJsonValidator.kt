package org.akj.lingo.learn.domain.usecase

import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * Validates LLM JSON output against the required shape per task type (Sprint 21).
 *
 * Each structured agent task (PLAN / DIAGNOSIS / ROLEPLAY_SCENARIO) has a fixed
 * JSON contract. This validator mirrors the shapes the callers actually parse,
 * so a response can be rejected *before* the caller tries to consume it — the
 * caller (via [StructuredLlmUseCase]) can then ask the model to fix it.
 *
 * Pure Kotlin + org.json; no Android dependencies, fully unit-testable.
 */
class AgentJsonValidator @Inject constructor() {

    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }

    /**
     * Validates [json] against the contract for [taskType]. Task types without a
     * structured contract return [ValidationResult.Valid] (free-text output such as
     * encouragement/explanation/hint is not schema-checked).
     */
    fun validate(taskType: String, json: String): ValidationResult {
        return when (taskType) {
            "PLAN" -> validatePlan(json)
            "DIAGNOSIS" -> validateDiagnosis(json)
            "ROLEPLAY_SCENARIO" -> validateRoleplay(json)
            else -> ValidationResult.Valid
        }
    }

    private fun validatePlan(json: String): ValidationResult {
        val root = try {
            JSONObject(json.trim())
        } catch (e: Exception) {
            return ValidationResult.Invalid("Not a JSON object: ${e.message}")
        }
        if (root.optString("theme").isBlank()) {
            return ValidationResult.Invalid("Missing 'theme'")
        }
        val days = root.optJSONArray("days")
            ?: return ValidationResult.Invalid("Missing 'days' array")
        if (days.length() == 0) {
            return ValidationResult.Invalid("'days' array is empty")
        }
        for (i in 0 until days.length()) {
            val day = days.optJSONObject(i)
                ?: return ValidationResult.Invalid("days[$i] is not an object")
            val missing = listOf("day", "focus", "target_words", "reference_sentence", "duration_minutes", "rationale")
                .filter { !day.has(it) }
            if (missing.isNotEmpty()) {
                return ValidationResult.Invalid("days[$i] missing: ${missing.joinToString(", ")}")
            }
            if (day.optJSONArray("target_words")?.length() ?: 0 == 0) {
                return ValidationResult.Invalid("days[$i] 'target_words' is empty")
            }
        }
        return ValidationResult.Valid
    }

    private fun validateDiagnosis(json: String): ValidationResult {
        val array = try {
            JSONArray(json.trim())
        } catch (e: Exception) {
            return ValidationResult.Invalid("Not a JSON array: ${e.message}")
        }
        if (array.length() == 0) {
            return ValidationResult.Invalid("Question array is empty")
        }
        for (i in 0 until array.length()) {
            val q = array.optJSONObject(i)
                ?: return ValidationResult.Invalid("questions[$i] is not an object")
            val missing = listOf("id", "type", "title", "description", "correctAnswer")
                .filter { !q.has(it) }
            if (missing.isNotEmpty()) {
                return ValidationResult.Invalid("questions[$i] missing: ${missing.joinToString(", ")}")
            }
        }
        return ValidationResult.Valid
    }

    private fun validateRoleplay(json: String): ValidationResult {
        val root = try {
            JSONObject(json.trim())
        } catch (e: Exception) {
            return ValidationResult.Invalid("Not a JSON object: ${e.message}")
        }
        if (root.optString("system_prompt").isBlank()) {
            return ValidationResult.Invalid("Missing 'system_prompt'")
        }
        if (root.optString("opening_line").isBlank()) {
            return ValidationResult.Invalid("Missing 'opening_line'")
        }
        return ValidationResult.Valid
    }
}
