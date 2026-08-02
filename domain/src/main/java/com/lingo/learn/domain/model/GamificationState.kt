package org.akj.lingo.learn.domain.model

/**
 * Sprint 10 — Persisted gamification state.
 *
 * Previously XP / level / daily-goal / makeup-card state was computed in the
 * ViewModel layer and stored ad-hoc in `lingo_xp_prefs` SharedPreferences,
 * with no domain entity and no repository. This model centralises that state
 * so progress survives re-installs and can be surfaced anywhere in the UI.
 */
data class GamificationState(
    /** Cumulative lifetime XP. Level is a pure function of this value. */
    val totalXp: Int = 0,
    /** Calendar month key (e.g. "2026-08") that the makeup-card allowance applies to. */
    val makeupMonthKey: String? = null,
    /** Number of makeup cards already used in the current month. */
    val makeupCardsUsed: Int = 0,
    /** ISO date (yyyy-MM-dd) of the last daily-goal snapshot. */
    val dailyGoalsDate: String? = null,
    /** Whether the "finish a session" goal was achieved on [dailyGoalsDate]. */
    val sessionGoalAchieved: Boolean = false,
    /** Whether the "80%+ accuracy" goal was achieved on [dailyGoalsDate]. */
    val accuracyGoalAchieved: Boolean = false,
    /** Whether the "learn 5 new words" goal was achieved on [dailyGoalsDate]. */
    val wordsGoalAchieved: Boolean = false,
    /** Last modification timestamp (epoch millis). */
    val lastModified: Long = 0L
)
