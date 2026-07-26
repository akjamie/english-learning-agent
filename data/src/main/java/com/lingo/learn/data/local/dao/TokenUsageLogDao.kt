package com.lingo.learn.data.local.dao

import androidx.room.*
import com.lingo.learn.data.local.entity.TokenUsageLogEntity

@Dao
interface TokenUsageLogDao {
    @Query("SELECT SUM(totalTokens) FROM token_usage_log WHERE timestamp >= :startOfMonth")
    suspend fun getMonthlyTotalTokens(startOfMonth: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TokenUsageLogEntity)

    @Query("SELECT * FROM token_usage_log ORDER BY timestamp DESC LIMIT 100")
    suspend fun getRecentLogs(): List<TokenUsageLogEntity>
}
