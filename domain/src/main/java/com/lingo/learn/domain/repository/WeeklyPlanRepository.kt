package org.akj.lingo.learn.domain.repository

import org.akj.lingo.learn.domain.model.LearningSession
import org.akj.lingo.learn.domain.model.Plan
import org.akj.lingo.learn.domain.model.QuizQuestion

/**
 * Domain interface for generating, pre-caching, and retrieving 7-day weekly curriculum plans.
 */
interface WeeklyPlanRepository {
    suspend fun generateAndCacheWeeklyPlan(
        grade: String,
        accuracy: Int,
        weakCategories: List<String>,
        completedMilestones: List<String>,
        difficultyAdjustment: Float = 0f
    ): Result<Plan>

    suspend fun getLatestCachedPlan(): Plan?

    suspend fun getCachedLearningSession(
        dayIndex: Int,
        grade: String = "Grade 4",
        reviewQuestions: List<QuizQuestion> = emptyList(),
        sentenceLengthAdjustment: Int = 0
    ): LearningSession

    /**
     * Returns the task summary for a given plan day (today by default), so the
     * Dashboard can show real plan-derived targets instead of hardcoded values.
     */
    suspend fun getDayTaskSummary(dayIndex: Int): DayTaskSummary?
}

/**
 * Lightweight summary of a single plan day for the Dashboard task card.
 */
data class DayTaskSummary(
    val day: Int,
    val theme: String,
    val durationMinutes: Int,
    val targetWords: List<String>
)
