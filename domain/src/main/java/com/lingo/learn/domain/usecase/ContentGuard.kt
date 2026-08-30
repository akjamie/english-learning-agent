package org.akj.lingo.learn.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight output safety guardrail for children's English learning (Sprint 21 Phase C).
 *
 * Validates LLM responses before presenting them in UI:
 * 1. Checks length limits based on task type.
 * 2. Blocks profanity, inappropriate topics, and prompt leak/injection phrases.
 * 3. Sanitizes or flags output for offline fallback.
 */
@Singleton
class ContentGuard @Inject constructor() {

    sealed class GuardResult {
        data class Safe(val sanitizedText: String) : GuardResult()
        data class Unsafe(val reason: String) : GuardResult()
    }

    /** Maximum allowed character lengths per task type to prevent layout breaks or runaway model output. */
    private fun maxLengthFor(taskType: String): Int = when (taskType) {
        "HINT" -> 200
        "EXPLAIN" -> 450
        "ROLEPLAY_SCENARIO" -> 400
        "ENCOURAGEMENT" -> 250
        else -> 2000
    }

    /**
     * Forbidden word and pattern list tailored for young learners.
     * Contains offensive words, inappropriate themes, and prompt injection patterns.
     */
    private val forbiddenPatterns = listOf(
        // Prompt leaks / instruction overrides
        Regex("(?i)ignore (all )?previous instructions"),
        Regex("(?i)system prompt"),
        Regex("(?i)as an ai language model"),
        Regex("(?i)as a language model"),
        Regex("(?i)openai"),
        Regex("(?i)anthropic"),
        Regex("(?i)minimax"),
        // Inappropriate / violent / adult / offensive vocabulary
        Regex("(?i)\\b(fuck|shit|bitch|damn|hell|asshole|bastard|crap)\\b"),
        Regex("(?i)\\b(kill|murder|suicide|weapon|gun|knife|blood|death|die)\\b"),
        Regex("(?i)\\b(porn|sex|sexy|nude|naked|erotic|drugs|alcohol|cigarette)\\b"),
        Regex("(?i)\\b(idiot|stupid|dumb|loser|hate you|shut up)\\b")
    )

    /**
     * Inspects [text] against length and safety rules for [taskType].
     */
    fun check(text: String?, taskType: String): GuardResult {
        if (text.isNullOrBlank()) {
            return GuardResult.Unsafe("Output is empty")
        }

        val trimmed = text.trim()
        val maxLen = maxLengthFor(taskType)
        if (trimmed.length > maxLen) {
            return GuardResult.Unsafe("Output length (${trimmed.length}) exceeds max $maxLen for $taskType")
        }

        for (pattern in forbiddenPatterns) {
            if (pattern.containsMatchIn(trimmed)) {
                return GuardResult.Unsafe("Output triggered safety pattern: ${pattern.pattern}")
            }
        }

        return GuardResult.Safe(trimmed)
    }

    /**
     * Returns true if [text] passes the safety checks for [taskType].
     */
    fun isSafe(text: String?, taskType: String): Boolean {
        return check(text, taskType) is GuardResult.Safe
    }
}
