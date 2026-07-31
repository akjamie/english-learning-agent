package org.akj.lingo.learn.data.local.dao

import androidx.room.*
import org.akj.lingo.learn.data.local.entity.LearningRecordEntity

@Dao
interface LearningRecordDao {
    @Query("SELECT * FROM learning_record ORDER BY timestamp DESC")
    suspend fun getAllRecords(): List<LearningRecordEntity>

    @Query("SELECT * FROM learning_record WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getRecordsSince(sinceTimestamp: Long): List<LearningRecordEntity>

    @Query("SELECT * FROM learning_record WHERE taskId = :taskId LIMIT 1")
    suspend fun getRecordByTaskId(taskId: String): LearningRecordEntity?

    @Query("SELECT * FROM learning_record WHERE timestamp >= :startOfDayTimestamp AND taskType = 'DAILY_PRACTICE' LIMIT 1")
    suspend fun getTodayRecord(startOfDayTimestamp: Long): LearningRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: LearningRecordEntity)
}
