package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.usecase.DiagnoseAnomalyUseCase.Category
import org.akj.lingo.learn.domain.usecase.DiagnoseAnomalyUseCase.LearningSummaryInput
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DiagnoseAnomalyUseCaseTest {

    private val useCase = DiagnoseAnomalyUseCase()

    private fun input(
        lastWeekAccuracy: Float = 0.7f,
        previousWeekAccuracy: Float = 0.7f,
        sessionsLastWeek: Int = 5,
        sessionsPreviousWeek: Int = 5,
        avgSessionMinutesLastWeek: Int = 15,
        avgSessionMinutesPreviousWeek: Int = 15,
        isExamPeriod: Boolean = false
    ) = LearningSummaryInput(
        lastWeekAccuracy = lastWeekAccuracy,
        previousWeekAccuracy = previousWeekAccuracy,
        sessionsLastWeek = sessionsLastWeek,
        sessionsPreviousWeek = sessionsPreviousWeek,
        avgSessionMinutesLastWeek = avgSessionMinutesLastWeek,
        avgSessionMinutesPreviousWeek = avgSessionMinutesPreviousWeek,
        isExamPeriod = isExamPeriod
    )

    @Test
    fun `exam period with accuracy drop is flagged as EXAM_PRESSURE`() {
        val result = useCase.diagnose(input(lastWeekAccuracy = 0.5f, previousWeekAccuracy = 0.85f, isExamPeriod = true))
        assertEquals(Category.EXAM_PRESSURE, result.category)
        assertFalse(result.defersToParent)
    }

    @Test
    fun `exam period without accuracy drop defers to parent`() {
        val result = useCase.diagnose(input(lastWeekAccuracy = 0.8f, previousWeekAccuracy = 0.8f, isExamPeriod = true))
        assertEquals(Category.UNCERTAIN, result.category)
        assertTrue(result.defersToParent)
    }

    @Test
    fun `session collapse is flagged as SCHEDULE_CHANGE`() {
        val result = useCase.diagnose(input(sessionsLastWeek = 2, sessionsPreviousWeek = 6))
        assertEquals(Category.SCHEDULE_CHANGE, result.category)
    }

    @Test
    fun `effort drop with healthy accuracy is flagged as MOTIVATION_DECLINE`() {
        val result = useCase.diagnose(
            input(
                avgSessionMinutesLastWeek = 5,
                avgSessionMinutesPreviousWeek = 20,
                lastWeekAccuracy = 0.75f
            )
        )
        assertEquals(Category.MOTIVATION_DECLINE, result.category)
    }

    @Test
    fun `persistently low accuracy with steady volume is DIFFICULTY_MISMATCH`() {
        val result = useCase.diagnose(
            input(
                lastWeekAccuracy = 0.45f,
                previousWeekAccuracy = 0.5f,
                sessionsLastWeek = 6,
                sessionsPreviousWeek = 5
            )
        )
        assertEquals(Category.DIFFICULTY_MISMATCH, result.category)
    }

    @Test
    fun `healthy steady learning is UNCERTAIN and defers`() {
        val result = useCase.diagnose(input())
        assertEquals(Category.UNCERTAIN, result.category)
        assertTrue(result.defersToParent)
    }

    @Test
    fun `diagnosis includes a human readable reason`() {
        val result = useCase.diagnose(input(lastWeekAccuracy = 0.5f, previousWeekAccuracy = 0.85f, isExamPeriod = true))
        assertTrue(result.reason.isNotBlank())
    }
}
