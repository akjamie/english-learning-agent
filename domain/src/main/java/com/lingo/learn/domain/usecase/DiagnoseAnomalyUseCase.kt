package org.akj.lingo.learn.domain.usecase

import javax.inject.Inject

/**
 * Bounded-autonomy learning anomaly diagnosis (Sprint 7 Phase B1).
 *
 * Takes a structured summary of the child's recent learning behaviour and maps it
 * to one of five predefined categories:
 *  - EXAM_PRESSURE       : accuracy dropped around exam/assessment periods
 *  - SCHEDULE_CHANGE     : study frequency or streak collapsed abruptly
 *  - MOTIVATION_DECLINE  : sessions completed but effort/engagement falling
 *  - DIFFICULTY_MISMATCH : accuracy persistently low without volume decline
 *  - UNCERTAIN           : signals are mixed / insufficient
 *
 * The agent is *bounded*: it only reports a confident judgement when the evidence
 * strongly supports one category. A confidence below [CONFIDENCE_DEFER_THRESHOLD]
 * means the agent defers to the parent instead of acting on its own.
 *
 * Pure Kotlin / zero Android dependencies — fully unit-testable.
 */
class DiagnoseAnomalyUseCase @Inject constructor() {

    /**
     * Structured inputs summarizing recent behaviour. All values are normalized
     * so the rule engine does not depend on any particular UI or storage layer.
     */
    data class LearningSummaryInput(
        val lastWeekAccuracy: Float,        // 0.0 .. 1.0
        val previousWeekAccuracy: Float,    // 0.0 .. 1.0
        val sessionsLastWeek: Int,
        val sessionsPreviousWeek: Int,
        val avgSessionMinutesLastWeek: Int,
        val avgSessionMinutesPreviousWeek: Int,
        val isExamPeriod: Boolean = false,
        val daysSinceStreakBroken: Int? = null,
        val retryRate: Float = 0.0f         // 0.0 .. 1.0 fraction of questions retried
    )

    data class Diagnosis(
        val category: Category,
        val confidence: Float,              // 0.0 .. 1.0
        val reason: String,
        val defersToParent: Boolean = false
    )

    enum class Category {
        EXAM_PRESSURE,
        SCHEDULE_CHANGE,
        MOTIVATION_DECLINE,
        DIFFICULTY_MISMATCH,
        UNCERTAIN
    }

    fun diagnose(input: LearningSummaryInput): Diagnosis {
        val signals = mutableListOf<Pair<Category, Float>>()

        if (input.isExamPeriod && input.lastWeekAccuracy < input.previousWeekAccuracy - 0.08f) {
            signals.add(Category.EXAM_PRESSURE to 0.82f)
        }

        if (input.sessionsPreviousWeek > 0) {
            val sessionDrop = 1f - input.sessionsLastWeek.toFloat() / input.sessionsPreviousWeek
            if (sessionDrop >= 0.5f) {
                signals.add(Category.SCHEDULE_CHANGE to 0.8f)
            }
        }

        val effortDrop = input.avgSessionMinutesLastWeek < input.avgSessionMinutesPreviousWeek * 0.6f
        val accuracyOk = input.lastWeekAccuracy >= 0.65f
        if (effortDrop && accuracyOk && input.sessionsLastWeek > 0) {
            signals.add(Category.MOTIVATION_DECLINE to 0.75f)
        }

        val persistentLowAccuracy = input.lastWeekAccuracy < 0.55f &&
            input.previousWeekAccuracy < 0.6f &&
            input.sessionsLastWeek >= input.sessionsPreviousWeek
        if (persistentLowAccuracy) {
            signals.add(Category.DIFFICULTY_MISMATCH to 0.78f)
        }

        val best = signals.maxByOrNull { it.second }
        if (best == null || best.second < 0.6f) {
            return Diagnosis(
                category = Category.UNCERTAIN,
                confidence = best?.second ?: 0.4f,
                reason = "Signals are mixed or insufficient for a confident judgement.",
                defersToParent = true
            )
        }

        val defers = best.second < CONFIDENCE_DEFER_THRESHOLD
        return Diagnosis(
            category = best.first,
            confidence = best.second,
            reason = describe(best.first, input),
            defersToParent = defers
        )
    }

    private fun describe(category: Category, input: LearningSummaryInput): String = when (category) {
        Category.EXAM_PRESSURE -> "Accuracy dropped sharply during an exam period, suggesting test-pressure related stress."
        Category.SCHEDULE_CHANGE -> "Session frequency collapsed versus the previous week, pointing to a schedule disruption."
        Category.MOTIVATION_DECLINE -> "Sessions continue but effort is fading — short, frequent sessions with healthy accuracy."
        Category.DIFFICULTY_MISMATCH -> "Accuracy stays persistently low even though study volume is steady; content may be too hard."
        Category.UNCERTAIN -> "Signals are mixed or insufficient for a confident judgement."
    }

    companion object {
        /** Below this confidence the agent must defer the decision to a parent. */
        const val CONFIDENCE_DEFER_THRESHOLD = 0.6f
    }
}
