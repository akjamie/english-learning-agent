package org.akj.lingo.learn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.akj.lingo.learn.domain.model.DailyStreak

@Entity(tableName = "daily_streak")
data class DailyStreakEntity(
    @PrimaryKey val date: String,
    val completed: Boolean,
    val usedMakeupCard: Boolean,
    val currentStreak: Int
) {
    fun toDomain() = DailyStreak(
        date = date,
        completed = completed,
        usedMakeupCard = usedMakeupCard,
        currentStreak = currentStreak
    )

    companion object {
        fun fromDomain(domain: DailyStreak) = DailyStreakEntity(
            date = domain.date,
            completed = domain.completed,
            usedMakeupCard = domain.usedMakeupCard,
            currentStreak = domain.currentStreak
        )
    }
}
