package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.model.LearningRecord
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ObservationTriggerEngineTest {

    private val engine = ObservationTriggerEngine()

    private fun record(
        taskId: String,
        taskType: String,
        accuracy: Float
    ) = LearningRecord(
        id = "id-$taskId-$taskType-$accuracy",
        taskId = taskId,
        timestamp = 0L,
        taskType = taskType,
        accuracy = accuracy,
        duration = 0L,
        score = 0,
        streakDays = 0,
        lastModified = 0L
    )

    @Test
    fun `word wrong last week then correct now triggers observation`() {
        val history = listOf(record("classroom", "QUIZ", 0.0f))
        val result = engine.checkObservation("classroom", 100, "LISTEN_CHOOSE_WORD", 1, history)

        assertNotNull(result)
        assertEquals(ObservationType.WORD_PREVIOUSLY_WRONG_NOW_CORRECT, result!!.type)
        assertTrue(result.message.contains("classroom"))
    }

    @Test
    fun `word correct last week does not trigger observation`() {
        val history = listOf(record("classroom", "QUIZ", 1.0f))
        val result = engine.checkObservation("classroom", 100, "LISTEN_CHOOSE_WORD", 1, history)

        assertNull(result)
    }

    @Test
    fun `pronunciation improved over same sentence triggers observation`() {
        val history = listOf(record("I like apples", "SPEAKING", 0.6f))
        val result = engine.checkObservation("I like apples", 85, "SPEAKING", 1, history)

        assertNotNull(result)
        assertEquals(ObservationType.PRONUNCIATION_IMPROVED, result!!.type)
    }

    @Test
    fun `pronunciation worse than before does not trigger observation`() {
        val history = listOf(record("I like apples", "SPEAKING", 0.9f))
        val result = engine.checkObservation("I like apples", 60, "SPEAKING", 1, history)

        assertNull(result)
    }

    @Test
    fun `retry then correct triggers encouragement observation`() {
        val result = engine.checkObservation("banana", 90, "QUIZ", 3, emptyList())

        assertNotNull(result)
        assertEquals(ObservationType.RETRY_THEN_CORRECT, result!!.type)
    }

    @Test
    fun `single attempt success without history triggers no observation`() {
        val result = engine.checkObservation("banana", 90, "QUIZ", 1, emptyList())

        assertNull(result)
    }

    @Test
    fun `daily practice records are ignored by the engine`() {
        val history = listOf(record("DAILY_123", "DAILY_PRACTICE", 0.0f))
        val result = engine.checkObservation("classroom", 100, "QUIZ", 1, history)

        assertNull(result)
    }

    @Test
    fun `word matching is case insensitive`() {
        val history = listOf(record("Classroom", "QUIZ", 0.2f))
        val result = engine.checkObservation("classroom", 100, "QUIZ", 1, history)

        assertNotNull(result)
    }
}
