package org.akj.lingo.learn.domain.repository

import org.akj.lingo.learn.domain.model.LearningRecord

/**
 * Domain repository interface for managing learning session records,
 * calculating streak days, today's progress, and weak category statistics.
 */
interface LearningRecordRepository {
    /**
     * Save a completed daily learning session record to the database.
     */
    suspend fun saveSessionRecord(record: LearningRecord)

    /**
     * Get learning records from the past 7 days.
     */
    suspend fun getWeeklyRecords(): List<LearningRecord>

    /**
     * Get overall average accuracy % over the last 30 days.
     */
    suspend fun getMonthlyAccuracy(): Float

    /**
     * Get weak categories sorted by error frequency.
     */
    suspend fun getWeakCategories(): List<String>

    /**
     * Get current active daily learning streak count.
     */
    suspend fun getStreakDays(): Int

    /**
     * Get today's learning progress (0.0f to 1.0f).
     */
    suspend fun getTodayProgress(): Float
}
