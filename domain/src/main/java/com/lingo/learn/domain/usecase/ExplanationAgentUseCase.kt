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
    private val llmRepository: LlmRepository,
    private val promptRegistry: AgentPromptRegistry,
    private val contentGuard: ContentGuard = ContentGuard()
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
        val prompt = promptRegistry.render(
            "EXPLAIN",
            mapOf(
                "word" to word,
                "grade" to grade,
                "history_section" to historySection
            )
        )

        val result = llmRepository.complete(prompt = prompt, taskType = "EXPLAIN", maxTokens = 150)
        return result.mapCatching { rawText ->
            when (val check = contentGuard.check(rawText, "EXPLAIN")) {
                is ContentGuard.GuardResult.Safe -> check.sanitizedText
                is ContentGuard.GuardResult.Unsafe -> throw Exception("Explanation blocked by safety guard: ${check.reason}")
            }
        }
    }
}
