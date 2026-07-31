package org.akj.lingo.learn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.akj.lingo.learn.data.local.entity.AgentDecisionLogEntity

@Dao
interface AgentDecisionLogDao {
    @Query("SELECT * FROM agent_decision_log ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentDecisions(limit: Int = 50): List<AgentDecisionLogEntity>

    @Query("SELECT * FROM agent_decision_log WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getDecisionsSince(since: Long): List<AgentDecisionLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AgentDecisionLogEntity)
}
