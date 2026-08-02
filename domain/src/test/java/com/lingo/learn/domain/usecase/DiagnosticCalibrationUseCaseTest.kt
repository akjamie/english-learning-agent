package org.akj.lingo.learn.domain.usecase

import kotlinx.coroutines.runBlocking
import org.akj.lingo.learn.domain.model.LearningRecord
import org.akj.lingo.learn.domain.repository.LearningRecordRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DiagnosticCalibrationUseCase] — Sprint 8 Item 4a.
 *
 * Uses a simple in-process fake repository to avoid requiring Mockito/MockK
 * in the :domain module (which is a pure Kotlin module with no Android deps).
 *
 * Tests cover:
 *  - Stable performance (no prompt)
 *  - Significant improvement (prompt to level up)
 *  - Significant decline (prompt to level down)
 *  - Interval not yet reached (no prompt regardless of accuracy delta)
 *  - No records yet (no prompt)
 *  - Exact boundary conditions
 */
class DiagnosticCalibrationUseCaseTest {

    // -------------------------------------------------------------------------
    // Simple fake repository — configurable monthly accuracy
    // -------------------------------------------------------------------------

    private class FakeLearningRecordRepository(
        private val monthlyAccuracy: Float
    ) : LearningRecordRepository {
        override suspend fun saveSessionRecord(record: LearningRecord) = Unit
        override suspend fun getRecordsSince(timestamp: Long) = emptyList<LearningRecord>()
        override suspend fun getWeeklyRecords() = emptyList<LearningRecord>()
        override suspend fun getMonthlyAccuracy() = monthlyAccuracy
        override suspend fun getWeakCategories() = emptyList<String>()
        override suspend fun getStreakDays() = 0
        override suspend fun getTodayProgress() = 0f
    }

    private fun useCase(recentAccuracy: Float) = DiagnosticCalibrationUseCase(
        FakeLearningRecordRepository(recentAccuracy)
    )

    // -------------------------------------------------------------------------
    // Core behaviour tests
    // -------------------------------------------------------------------------

    @Test
    fun `stable accuracy - no recalibration suggested`() = runTest {
        val result = useCase(recentAccuracy = 73f).check(
            baselineAccuracy = 70f,
            activeDaysSinceLastCalibration = 14
        )
        assertFalse(result.shouldSuggestRecalibration,
            "A 3% delta is below the 10% threshold — no prompt expected")
        assertEquals(DiagnosticCalibrationUseCase.Direction.STABLE, result.direction)
    }

    @Test
    fun `significant improvement - suggests level up`() = runTest {
        val result = useCase(recentAccuracy = 85f).check(
            baselineAccuracy = 70f,
            activeDaysSinceLastCalibration = 14
        )
        assertTrue(result.shouldSuggestRecalibration)
        assertEquals(DiagnosticCalibrationUseCase.Direction.IMPROVING_SIGNIFICANTLY, result.direction)
        assertTrue(result.reason.contains("15"), "Reason should mention the delta: ${result.reason}")
    }

    @Test
    fun `significant decline - suggests level down`() = runTest {
        val result = useCase(recentAccuracy = 50f).check(
            baselineAccuracy = 70f,
            activeDaysSinceLastCalibration = 14
        )
        assertTrue(result.shouldSuggestRecalibration)
        assertEquals(DiagnosticCalibrationUseCase.Direction.DECLINING_SIGNIFICANTLY, result.direction)
        assertTrue(result.reason.contains("20"), "Reason should mention the delta: ${result.reason}")
    }

    @Test
    fun `interval not yet reached - no prompt regardless of accuracy delta`() = runTest {
        // Even if accuracy jumped 30%, if we haven't been studying for 14 days, no prompt
        val result = useCase(recentAccuracy = 100f).check(
            baselineAccuracy = 70f,
            activeDaysSinceLastCalibration = 10  // less than 14-day interval
        )
        assertFalse(result.shouldSuggestRecalibration,
            "Should not prompt before the calibration interval is reached")
    }

    @Test
    fun `exactly at interval boundary - check is run`() = runTest {
        val result = useCase(recentAccuracy = 85f).check(
            baselineAccuracy = 70f,
            activeDaysSinceLastCalibration = 14  // exactly at boundary
        )
        assertTrue(result.shouldSuggestRecalibration,
            "At exactly 14 days and 15% improvement, should prompt")
    }

    @Test
    fun `no learning records (zero accuracy) - no prompt`() = runTest {
        val result = useCase(recentAccuracy = 0f).check(
            baselineAccuracy = 70f,
            activeDaysSinceLastCalibration = 30
        )
        assertFalse(result.shouldSuggestRecalibration,
            "Zero accuracy means no records yet — should not prompt")
    }

    @Test
    fun `exactly at threshold boundary 10 percent improvement - prompts`() = runTest {
        val result = useCase(recentAccuracy = 80f).check(
            baselineAccuracy = 70f,
            activeDaysSinceLastCalibration = 14
        )
        assertTrue(result.shouldSuggestRecalibration,
            "Exactly 10% improvement should cross the threshold and prompt")
        assertEquals(DiagnosticCalibrationUseCase.Direction.IMPROVING_SIGNIFICANTLY, result.direction)
    }

    @Test
    fun `just below threshold 9 percent improvement - no prompt`() = runTest {
        val result = useCase(recentAccuracy = 79f).check(
            baselineAccuracy = 70f,
            activeDaysSinceLastCalibration = 14
        )
        assertFalse(result.shouldSuggestRecalibration,
            "9% improvement is below the 10% threshold — no prompt expected")
    }

    @Test
    fun `reason message is non-empty when recalibration is suggested`() = runTest {
        val result = useCase(recentAccuracy = 90f).check(
            baselineAccuracy = 70f,
            activeDaysSinceLastCalibration = 14
        )
        assertTrue(result.shouldSuggestRecalibration)
        assertTrue(result.reason.isNotBlank(), "Reason must be non-blank when prompt is shown")
    }

    @Test
    fun `custom calibration interval is respected`() = runTest {
        // With a 30-day interval, 20 days is not enough
        val result = useCase(recentAccuracy = 90f).check(
            baselineAccuracy = 70f,
            activeDaysSinceLastCalibration = 20,
            calibrationIntervalDays = 30
        )
        assertFalse(result.shouldSuggestRecalibration,
            "20 active days is below custom 30-day interval — no prompt")
    }

    @Test
    fun `custom delta threshold is respected`() = runTest {
        // With a 20% threshold, a 15% improvement should NOT trigger
        val result = useCase(recentAccuracy = 85f).check(
            baselineAccuracy = 70f,
            activeDaysSinceLastCalibration = 14,
            deltaThreshold = 20f
        )
        assertFalse(result.shouldSuggestRecalibration,
            "15% delta is below the custom 20% threshold — no prompt")
    }

    private fun runTest(block: suspend () -> Unit) {
        runBlocking { block() }
    }
}
