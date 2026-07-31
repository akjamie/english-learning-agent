package org.akj.lingo.learn.domain.usecase

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DailyGoalTrackerTest {

    private val tracker = DailyGoalTracker()

    @Test
    fun `completed session with good accuracy and words unlocks all goals`() {
        val result = tracker.evaluate(sessionCompleted = true, quizAccuracy = 0.9f, newWordsLearned = 6)
        assertTrue(result.allAchieved)
        assertEquals(3, result.goals.count { it.achieved })
    }

    @Test
    fun `accuracy below threshold keeps accuracy goal locked`() {
        val result = tracker.evaluate(sessionCompleted = true, quizAccuracy = 0.6f, newWordsLearned = 6)
        assertFalse(result.allAchieved)
        assertFalse(result.goals.first { it.id == "accuracy" }.achieved)
    }

    @Test
    fun `no session means accuracy goal stays locked even with high accuracy`() {
        val result = tracker.evaluate(sessionCompleted = false, quizAccuracy = 0.9f, newWordsLearned = 0)
        assertFalse(result.goals.first { it.id == "accuracy" }.achieved)
    }

    @Test
    fun `fewer than five words keeps words goal locked`() {
        val result = tracker.evaluate(sessionCompleted = true, quizAccuracy = 0.9f, newWordsLearned = 3)
        assertFalse(result.goals.first { it.id == "words" }.achieved)
    }

    @Test
    fun `nothing done means no goals achieved`() {
        val result = tracker.evaluate(sessionCompleted = false, quizAccuracy = 0f, newWordsLearned = 0)
        assertFalse(result.allAchieved)
        assertEquals(0, result.goals.count { it.achieved })
    }

    @Test
    fun `each goal carries a badge`() {
        val result = tracker.evaluate(sessionCompleted = true, quizAccuracy = 0.9f, newWordsLearned = 6)
        assertTrue(result.goals.all { it.badge.isNotBlank() })
    }
}
