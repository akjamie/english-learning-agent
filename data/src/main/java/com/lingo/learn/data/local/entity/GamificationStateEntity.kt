package org.akj.lingo.learn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.akj.lingo.learn.domain.model.GamificationState

@Entity(tableName = "gamification_state")
data class GamificationStateEntity(
    @PrimaryKey val id: Int = 1,
    val totalXp: Int,
    val makeupMonthKey: String?,
    val makeupCardsUsed: Int,
    val dailyGoalsDate: String?,
    val sessionGoalAchieved: Boolean,
    val accuracyGoalAchieved: Boolean,
    val wordsGoalAchieved: Boolean,
    val lastModified: Long
) {
    fun toDomain() = GamificationState(
        totalXp = totalXp,
        makeupMonthKey = makeupMonthKey,
        makeupCardsUsed = makeupCardsUsed,
        dailyGoalsDate = dailyGoalsDate,
        sessionGoalAchieved = sessionGoalAchieved,
        accuracyGoalAchieved = accuracyGoalAchieved,
        wordsGoalAchieved = wordsGoalAchieved,
        lastModified = lastModified
    )

    companion object {
        fun fromDomain(domain: GamificationState) = GamificationStateEntity(
            id = 1,
            totalXp = domain.totalXp,
            makeupMonthKey = domain.makeupMonthKey,
            makeupCardsUsed = domain.makeupCardsUsed,
            dailyGoalsDate = domain.dailyGoalsDate,
            sessionGoalAchieved = domain.sessionGoalAchieved,
            accuracyGoalAchieved = domain.accuracyGoalAchieved,
            wordsGoalAchieved = domain.wordsGoalAchieved,
            lastModified = domain.lastModified
        )
    }
}
