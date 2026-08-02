package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.model.LearningRecord
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for the Sprint 11 FAST_ANSWER observation rule in [ObservationTriggerEngine].
 */
class FastAnswerObservationTest {

    private val engine = ObservationTriggerEngine()

    private fun record(seconds: Long) = LearningRecord(
        id = "r-${seconds}",
        taskId = "word-x",
        timestamp = System.currentTimeMillis(),
        taskType = "QUIZ",
        accuracy = 1.0f,
        duration = seconds,
        score = 100,
        streakDays = 1
    )

    @Test
    fun `fast correct answer triggers FAST_ANSWER with no history baseline`() {
        val result = engine.checkObservation(
            currentWord = "hello",
            currentScore = 100,
            questionType = "QUIZ",
            attemptCount = 1,
            recentRecords = emptyList(),
            responseTimeMs = 3000L // well under 10s default baseline
        )
        assertNotNull(result)
        assertEquals(ObservationType.FAST_ANSWER, result!!.type)
    }

    @Test
    fun `slow answer does not trigger FAST_ANSWER`() {
        val result = engine.checkObservation(
            currentWord = "hello",
            currentScore = 100,
            questionType = "QUIZ",
            attemptCount = 1,
            recentRecords = emptyList(),
            responseTimeMs = 9000L // above 0.5 * 10s = 5s threshold
        )
        assertNull(result)
    }

    @Test
    fun `fast answer below personal average triggers`() {
        val history = listOf(record(20), record(22)) // avg 21s -> baseline 21000ms
        val result = engine.checkObservation(
            currentWord = "hello",
            currentScore = 100,
            questionType = "QUIZ",
            attemptCount = 1,
            recentRecords = history,
            responseTimeMs = 5000L // < 10500ms
        )
        assertNotNull(result)
        assertEquals(ObservationType.FAST_ANSWER, result!!.type)
    }

    @Test
    fun `wrong answer never triggers FAST_ANSWER`() {
        val result = engine.checkObservation(
            currentWord = "hello",
            currentScore = 0,
            questionType = "QUIZ",
            attemptCount = 1,
            recentRecords = emptyList(),
            responseTimeMs = 1000L
        )
        assertNull(result)
    }

    @Test
    fun `null response time does not trigger FAST_ANSWER`() {
        val result = engine.checkObservation(
            currentWord = "hello",
            currentScore = 100,
            questionType = "QUIZ",
            attemptCount = 1,
            recentRecords = emptyList(),
            responseTimeMs = null
        )
        assertNull(result)
    }
}
