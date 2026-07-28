package org.akj.lingo.learn.data.local.dao

import androidx.room.*
import org.akj.lingo.learn.data.local.entity.DailyStreakEntity

@Dao
interface DailyStreakDao {
    @Query("SELECT * FROM daily_streak ORDER BY date DESC")
    suspend fun getAllStreaks(): List<DailyStreakEntity>

    @Query("SELECT * FROM daily_streak ORDER BY date DESC LIMIT 1")
    suspend fun getLatestStreak(): DailyStreakEntity?

    @Query("SELECT * FROM daily_streak WHERE date = :date LIMIT 1")
    suspend fun getStreakByDate(date: String): DailyStreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(streak: DailyStreakEntity)
}
