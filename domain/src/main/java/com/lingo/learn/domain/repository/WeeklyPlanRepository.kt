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
        completedMilestones: List<String>
    ): Result<Plan>

    suspend fun getLatestCachedPlan(): Plan?

    suspend fun getCachedLearningSession(
        dayIndex: Int,
        grade: String = "Grade 4",
        reviewQuestions: List<QuizQuestion> = emptyList(),
        sentenceLengthAdjustment: Int = 0
    ): LearningSession
}
