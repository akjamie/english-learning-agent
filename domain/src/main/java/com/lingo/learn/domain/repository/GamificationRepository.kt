package org.akj.lingo.learn.domain.repository

import org.akj.lingo.learn.domain.model.GamificationState

/**
 * Sprint 10 — Domain repository interface for persisted gamification state
 * (XP / level / daily goals / makeup cards).
 *
 * Previously this state lived in ad-hoc SharedPreferences inside the ViewModel.
 * Centralising it behind a repository keeps the domain model pure and lets any
 * UI surface (Dashboard, TaskComplete, Widget) read consistent progress.
 */
interface GamificationRepository {
    /**
     * Loads the persisted gamification state (single row).
     */
    suspend fun getState(): GamificationState

    /**
     * Persists the full gamification state (single row, upsert).
     */
    suspend fun saveState(state: GamificationState)

    /**
     * Resets the daily-goal snapshot fields, keeping cumulative XP / makeup
     * state intact. Called when a new calendar day starts.
     */
    suspend fun resetDailyGoals(todayIsoDate: String, nowMs: Long)
}
