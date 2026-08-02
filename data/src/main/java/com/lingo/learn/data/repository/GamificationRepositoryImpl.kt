package org.akj.lingo.learn.data.repository

import org.akj.lingo.learn.data.local.dao.GamificationStateDao
import org.akj.lingo.learn.data.local.entity.GamificationStateEntity
import org.akj.lingo.learn.domain.model.GamificationState
import org.akj.lingo.learn.domain.repository.GamificationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sprint 10 — Room-backed gamification state repository.
 */
@Singleton
class GamificationRepositoryImpl @Inject constructor(
    private val dao: GamificationStateDao
) : GamificationRepository {

    override suspend fun getState(): GamificationState {
        return dao.getState()?.toDomain() ?: GamificationState()
    }

    override suspend fun saveState(state: GamificationState) {
        dao.saveState(GamificationStateEntity.fromDomain(state))
    }

    override suspend fun resetDailyGoals(todayIsoDate: String, nowMs: Long) {
        val current = getState()
        dao.saveState(
            GamificationStateEntity.fromDomain(
                current.copy(
                    dailyGoalsDate = todayIsoDate,
                    sessionGoalAchieved = false,
                    accuracyGoalAchieved = false,
                    wordsGoalAchieved = false,
                    lastModified = nowMs
                )
            )
        )
    }
}
