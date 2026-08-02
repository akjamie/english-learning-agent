package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.repository.LearningRecordRepository
import javax.inject.Inject

/**
 * Sprint 8 — Diagnostic Calibration Engine
 *
 * Every 14 days of active study, compares the student's recent accuracy trend against
 * the baseline established at the initial diagnosis. If the delta is significant
 * (>= 10 percentage points up or down), prompts the user to re-run a short 5-question
 * diagnostic so the grade/level assignment can be updated.
 *
 * This addresses the reality that a child's English level changes over time:
 * - A student who started at Grade 2 level but has been excelling at Grade 4 content
 *   for 2 weeks should be bumped up, not kept on easy content.
 * - A student who was assessed at Grade 5 but is consistently scoring < 50% should
 *   be stepped back to Grade 3-4 content to rebuild confidence.
 *
 * All decisions are suggested, never forced — the parent/student always sees an
 * "Update Level?" prompt and can dismiss it.
 */
class DiagnosticCalibrationUseCase @Inject constructor(
    private val learningRecordRepository: LearningRecordRepository
) {
    data class CalibrationCheckResult(
        /** True when a re-diagnosis should be suggested to the user. */
        val shouldSuggestRecalibration: Boolean,
        /** Human-readable reason string (used in the prompt dialog). */
        val reason: String = "",
        /** Direction of change detected. */
        val direction: Direction = Direction.STABLE
    )

    enum class Direction { IMPROVING_SIGNIFICANTLY, DECLINING_SIGNIFICANTLY, STABLE }

    /**
     * Checks whether the student is due for a calibration diagnostic.
     *
     * @param baselineAccuracy  accuracy % recorded at initial diagnosis (0–100)
     * @param activeDaysSinceLastCalibration  how many days since last calibration or start
     * @param calibrationIntervalDays  check period (default: 14 days)
     * @param deltaThreshold  minimum accuracy shift (%) to trigger recalibration (default: 10%)
     */
    suspend fun check(
        baselineAccuracy: Float,
        activeDaysSinceLastCalibration: Int = CALIBRATION_INTERVAL_DAYS,
        calibrationIntervalDays: Int = CALIBRATION_INTERVAL_DAYS,
        deltaThreshold: Float = DELTA_THRESHOLD_PERCENT
    ): CalibrationCheckResult {

        if (activeDaysSinceLastCalibration < calibrationIntervalDays) {
            return CalibrationCheckResult(shouldSuggestRecalibration = false)
        }

        val recentAccuracy = learningRecordRepository.getMonthlyAccuracy()

        // No records yet — skip
        if (recentAccuracy == 0f) {
            return CalibrationCheckResult(shouldSuggestRecalibration = false)
        }

        val delta = recentAccuracy - baselineAccuracy

        return when {
            delta >= deltaThreshold -> CalibrationCheckResult(
                shouldSuggestRecalibration = true,
                direction = Direction.IMPROVING_SIGNIFICANTLY,
                reason = "🌟 Your accuracy improved by ${delta.toInt()}% over the past $calibrationIntervalDays days! " +
                        "Would you like to try a harder level?"
            )
            delta <= -deltaThreshold -> CalibrationCheckResult(
                shouldSuggestRecalibration = true,
                direction = Direction.DECLINING_SIGNIFICANTLY,
                reason = "🤔 Your accuracy dropped by ${(-delta).toInt()}% recently. " +
                        "Would you like to try an easier level for a while?"
            )
            else -> CalibrationCheckResult(shouldSuggestRecalibration = false)
        }
    }

    companion object {
        const val CALIBRATION_INTERVAL_DAYS = 14
        const val DELTA_THRESHOLD_PERCENT = 10f
    }
}
