package org.akj.lingo.learn.domain.repository

import org.akj.lingo.learn.domain.model.AgentDecisionLog

interface AgentDecisionLogRepository {
    suspend fun insert(log: AgentDecisionLog)
    suspend fun getRecentDecisions(limit: Int = 50): List<AgentDecisionLog>
    suspend fun getDecisionsSince(timestamp: Long): List<AgentDecisionLog>
}
