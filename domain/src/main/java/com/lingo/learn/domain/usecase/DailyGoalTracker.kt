package org.akj.lingo.learn.domain.usecase

import javax.inject.Inject

/**
 * Daily 3-goal system (Sprint 7 Phase C2).
 *
 * Each day the child is asked to hit three goals:
 *   1. Complete a full learning session
 *   2. Reach 80%+ quiz accuracy
 *   3. Learn 5 new words
 *
 * Every goal, when achieved, unlocks a small badge. Goals reset on the next
 * calendar day. Pure Kotlin so the goal rules can be unit-tested independently
 * of the persistence layer.
 */
class DailyGoalTracker @Inject constructor() {

    data class Goal(
        val id: String,
        val label: String,
        val achieved: Boolean,
        val badge: String
    )

    data class DailyGoals(
        val goals: List<Goal>,
        val allAchieved: Boolean
    )

    /**
     * Evaluates the day's goals from raw session inputs.
     *
     * @param sessionCompleted  true if a full session finished today
     * @param quizAccuracy      0.0 .. 1.0 quiz accuracy (0 if no quiz yet)
     * @param newWordsLearned   count of new words introduced today
     */
    fun evaluate(sessionCompleted: Boolean, quizAccuracy: Float, newWordsLearned: Int): DailyGoals {
        val sessionGoal = Goal(
            id = "session",
            label = "Finish 1 learning session",
            achieved = sessionCompleted,
            badge = "🎯"
        )
        val accuracyGoal = Goal(
            id = "accuracy",
            label = "Reach 80%+ accuracy",
            achieved = sessionCompleted && quizAccuracy >= ACCURACY_THRESHOLD,
            badge = "⭐"
        )
        val wordsGoal = Goal(
            id = "words",
            label = "Learn 5 new words",
            achieved = newWordsLearned >= WORDS_THRESHOLD,
            badge = "📖"
        )
        return DailyGoals(
            goals = listOf(sessionGoal, accuracyGoal, wordsGoal),
            allAchieved = sessionGoal.achieved && accuracyGoal.achieved && wordsGoal.achieved
        )
    }

    companion object {
        const val ACCURACY_THRESHOLD = 0.8f
        const val WORDS_THRESHOLD = 5
    }
}
