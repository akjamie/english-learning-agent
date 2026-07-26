package com.lingo.learn.data.local.dao

import androidx.room.*
import com.lingo.learn.data.local.entity.LearningRecordEntity

@Dao
interface LearningRecordDao {
    @Query("SELECT * FROM learning_record ORDER BY timestamp DESC")
    suspend fun getAllRecords(): List<LearningRecordEntity>

    @Query("SELECT * FROM learning_record WHERE taskId = :taskId LIMIT 1")
    suspend fun getRecordByTaskId(taskId: String): LearningRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: LearningRecordEntity)
}
