package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.model.GradeBand
import javax.inject.Inject

/**
 * Adaptive difficulty engine (Sprint 7 Phase D1).
 *
 * Adjusts sentence length (in words) and CEFR level based on the child's most
 * recent quiz accuracy. The base configuration comes from the [GradeBand] so the
 * adjustment stays within band-appropriate bounds.
 *
 * Every adjustment is meant to be logged to `AgentDecisionLog` (Sprint 6 infra)
 * so parents can see "what Lingo adjusted this week".
 *
 * Pure Kotlin / zero Android dependencies — fully unit-testable.
 */
class AdaptiveDifficultyEngine @Inject constructor() {

    /**
     * Adjustment steps for sentence length, in words.
     *   EASIER: up to -3 words
     *   HARDER: up to +3 words
     */
    fun sentenceLengthAdjustment(lastQuizAccuracy: Float): Int = when {
        lastQuizAccuracy < 0.55f -> -3
        lastQuizAccuracy < 0.7f -> -1
        lastQuizAccuracy <= 0.85f -> 0
        lastQuizAccuracy <= 0.95f -> 2
        else -> 3
    }

    /**
     * Adjusted maximum words per sentence, clamped to [GradeBand] bounds.
     */
    fun adjustedMaxWordsPerSentence(gradeBand: GradeBand, lastQuizAccuracy: Float): Int {
        val base = gradeBand.maxWordsPerSentence
        return (base + sentenceLengthAdjustment(lastQuizAccuracy)).coerceIn(4, 22)
    }

    /**
     * CEFR level derived from accuracy, clamped within the band's declared range.
     * Returns a readable label used in prompts and decision logs.
     */
    fun cefrLevel(gradeBand: GradeBand, lastQuizAccuracy: Float): String {
        val base = gradeBand.defaultVocabularyRange // e.g. "CEFR A1-A2, daily life & school themes"
        return when {
            lastQuizAccuracy < 0.55f -> base.replace("A1-A2", "A1").replace("A2-B1", "A2").replace("B1-B2", "B1")
            lastQuizAccuracy <= 0.85f -> base
            else -> base
                .replace("A1-A2", "A2").replace("A2-B1", "B1").replace("B1-B2", "B2")
        }
    }

    /**
     * A short human-readable description of the adjustment for the decision log,
     * e.g. "Accuracy 62% -> sentence length -1 words, CEFR level held".
     */
    fun describeAdjustment(gradeBand: GradeBand, lastQuizAccuracy: Float): String {
        val delta = sentenceLengthAdjustment(lastQuizAccuracy)
        val direction = when {
            delta < 0 -> "eased"
            delta > 0 -> "tightened"
            else -> "held"
        }
        return "Accuracy ${(lastQuizAccuracy * 100).toInt()}%: sentence length $direction (${if (delta > 0) "+$delta" else delta} words), CEFR ${cefrLevel(gradeBand, lastQuizAccuracy)}"
    }
}
