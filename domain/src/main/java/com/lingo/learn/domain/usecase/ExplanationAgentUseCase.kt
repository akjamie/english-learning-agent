package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.repository.LlmRepository
import javax.inject.Inject

/**
 * Error Book explanation agent (Sprint 7 Phase B2).
 *
 * Answers the child's "why can't I remember this word?" question using the word's
 * full error history — not just the latest mistake. The error history is passed
 * as structured JSON so the LLM can reference how many times the child missed the
 * word and under which question types (listening vs spelling vs speaking).
 */
class ExplanationAgentUseCase @Inject constructor(
    private val llmRepository: LlmRepository
) {
    suspend operator fun invoke(word: String, errorType: String, grade: String): Result<String> {
        return invoke(word = word, errorType = errorType, grade = grade, errorHistoryJson = null)
    }

    /**
     * @param errorHistoryJson structured JSON of the word's error history
     *                         (errorCount, question types, most recent errors).
     *                         When null, falls back to a single-error explanation.
     */
    suspend fun invoke(
        word: String,
        errorType: String,
        grade: String,
        errorHistoryJson: String?
    ): Result<String> {
        val historySection = if (errorHistoryJson.isNullOrBlank()) {
            "The child got this word wrong in a quiz due to: $errorType."
        } else {
            """
                The child keeps having trouble remembering this word. Here is the word's
                full error history (as JSON): $errorHistoryJson
                Notice the pattern: which question type the child most often misses,
                and address that specific weak spot in your explanation.
            """.trimIndent()
        }
        val prompt = """
            You are Lingo, a friendly fox tutor. The child asks: "Why can't I remember the word "$word"?"
            Explain the word "$word" to a $grade child in a way that makes it stick.
            $historySection

            --- Guidelines ---
            1. Use encouraging, warm, and simple English.
            2. Provide one clear example sentence suitable for children.
            3. Highlight a quick mnemonic trick or spelling tip (e.g., "hear has an 'ear' inside it").
            4. If the child's history shows a specific weak skill (e.g., listening vs spelling),
               give one targeted tip for that skill.
            5. Keep the explanation under 100 words. Do not be overly academic.
        """.trimIndent()

        return llmRepository.complete(prompt = prompt, taskType = "EXPLAIN", maxTokens = 150)
    }
}
