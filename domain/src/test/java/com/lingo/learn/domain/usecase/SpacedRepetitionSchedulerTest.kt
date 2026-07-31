package org.akj.lingo.learn.domain.usecase

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SpacedRepetitionSchedulerTest {

    private val scheduler = SpacedRepetitionScheduler()
    private val dayMs = 24 * 3600 * 1000L

    @Test
    fun `intervals follow the Ebbinghaus 1 3 7 14 schedule`() {
        assertEquals(listOf(1, 3, 7, 14), scheduler.reviewIntervalsDays())
    }

    @Test
    fun `first correct answer schedules review after 1 day`() {
        val now = 1_000_000L
        assertEquals(now + dayMs, scheduler.nextReviewAfterCorrect(now, 0))
    }

    @Test
    fun `second correct answer schedules review after 3 days`() {
        val now = 1_000_000L
        assertEquals(now + 3 * dayMs, scheduler.nextReviewAfterCorrect(now, 1))
    }

    @Test
    fun `fourth correct answer schedules review after 14 days`() {
        val now = 1_000_000L
        assertEquals(now + 14 * dayMs, scheduler.nextReviewAfterCorrect(now, 3))
    }

    @Test
    fun `interval index beyond schedule caps at longest interval`() {
        val now = 1_000_000L
        assertEquals(now + 14 * dayMs, scheduler.nextReviewAfterCorrect(now, 99))
    }

    @Test
    fun `wrong answer resets to the 1 day interval`() {
        val now = 1_000_000L
        assertEquals(now + dayMs, scheduler.nextReviewAfterError(now))
    }

    @Test
    fun `zero or negative timestamp maps to first interval`() {
        assertEquals(0, scheduler.intervalIndexForTimestamp(0L, 1_000_000L))
        assertEquals(0, scheduler.intervalIndexForTimestamp(-5L, 1_000_000L))
    }

    @Test
    fun `elapsed days map to correct interval index`() {
        val now = 1_785_542_400_000L // Aug 2026
        assertEquals(0, scheduler.intervalIndexForTimestamp(now - 2 * dayMs, now))  // < 3 days
        assertEquals(1, scheduler.intervalIndexForTimestamp(now - 5 * dayMs, now))  // >= 3 days
        assertEquals(2, scheduler.intervalIndexForTimestamp(now - 10 * dayMs, now)) // >= 7 days
        assertEquals(3, scheduler.intervalIndexForTimestamp(now - 15 * dayMs, now)) // >= 14 days
    }

    @Test
    fun `word is due when timestamp has passed or is unset`() {
        val now = 1_000_000L
        assertTrue(scheduler.isDue(0L, now))
        assertTrue(scheduler.isDue(now - 1, now))
        assertFalse(scheduler.isDue(now + 1000, now))
    }
}
