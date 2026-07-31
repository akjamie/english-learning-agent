package org.akj.lingo.learn.domain.usecase

import javax.inject.Inject

/**
 * Ebbinghaus spaced-repetition scheduler (Sprint 7 Phase A4).
 *
 * Review intervals follow the classic forgetting curve: 1 / 3 / 7 / 14 days.
 * Each time an error-book word is answered correctly, it advances to the next
 * review interval; each time it is answered wrong again, the schedule resets to
 * the shortest interval.
 *
 * Pure Kotlin / zero Android dependencies — fully unit-testable.
 */
class SpacedRepetitionScheduler @Inject constructor() {

    /**
     * The review intervals (in days) applied in sequence after each correct answer.
     */
    fun reviewIntervalsDays(): List<Int> = INTERVALS_DAYS

    /**
     * Computes the next review timestamp for a word answered correctly.
     *
     * @param nowMs             current wall-clock time
     * @param currentIntervalIndex index into [reviewIntervalsDays] already completed
     *                             (0 = first time correct, 1 = second, etc.)
     */
    fun nextReviewAfterCorrect(nowMs: Long, currentIntervalIndex: Int): Long {
        val intervalDays = INTERVALS_DAYS.getOrElse(currentIntervalIndex.coerceIn(0, INTERVALS_DAYS.size - 1)) {
            INTERVALS_DAYS.last()
        }
        return nowMs + intervalDays * DAY_MS
    }

    /**
     * Computes the next review timestamp after a repeated mistake.
     * A fresh mistake resets the schedule to the shortest (1 day) interval.
     */
    fun nextReviewAfterError(nowMs: Long): Long = nowMs + INTERVALS_DAYS.first() * DAY_MS

    /**
     * Derives the interval index that a word's stored [nextReviewTimestamp] maps to,
     * so that [nextReviewAfterCorrect] can advance the schedule correctly.
     */
    fun intervalIndexForTimestamp(nextReviewTimestamp: Long, nowMs: Long): Int {
        if (nextReviewTimestamp <= 0L) return 0
        val elapsedDays = ((nowMs - nextReviewTimestamp).coerceAtLeast(0L)) / DAY_MS
        return when {
            elapsedDays >= INTERVALS_DAYS[3] -> 3
            elapsedDays >= INTERVALS_DAYS[2] -> 2
            elapsedDays >= INTERVALS_DAYS[1] -> 1
            else -> 0
        }
    }

    /**
     * Whether a word is due for review (its scheduled timestamp has passed).
     */
    fun isDue(nextReviewTimestamp: Long, nowMs: Long): Boolean =
        nextReviewTimestamp <= 0L || nextReviewTimestamp <= nowMs

    companion object {
        /** Ebbinghaus review schedule in days: 1 / 3 / 7 / 14. */
        private val INTERVALS_DAYS = listOf(1, 3, 7, 14)
        private const val DAY_MS = 24 * 3600 * 1000L
    }
}
