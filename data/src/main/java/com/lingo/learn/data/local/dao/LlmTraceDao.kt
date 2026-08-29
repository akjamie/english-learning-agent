package org.akj.lingo.learn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.akj.lingo.learn.data.local.entity.LlmTraceEntity

@Dao
interface LlmTraceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrace(trace: LlmTraceEntity)

    @Query("SELECT * FROM llm_trace ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentTraces(limit: Int): List<LlmTraceEntity>
}
