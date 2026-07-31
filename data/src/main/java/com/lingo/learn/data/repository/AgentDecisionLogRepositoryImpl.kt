package org.akj.lingo.learn.data.repository

import org.akj.lingo.learn.data.local.dao.AgentDecisionLogDao
import org.akj.lingo.learn.data.local.entity.AgentDecisionLogEntity
import org.akj.lingo.learn.domain.model.AgentDecisionLog
import org.akj.lingo.learn.domain.repository.AgentDecisionLogRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentDecisionLogRepositoryImpl @Inject constructor(
    private val agentDecisionLogDao: AgentDecisionLogDao
) : AgentDecisionLogRepository {

    override suspend fun insert(log: AgentDecisionLog) {
        agentDecisionLogDao.insert(AgentDecisionLogEntity.fromDomain(log))
    }

    override suspend fun getRecentDecisions(limit: Int): List<AgentDecisionLog> {
        return agentDecisionLogDao.getRecentDecisions(limit).map { it.toDomain() }
    }

    override suspend fun getDecisionsSince(timestamp: Long): List<AgentDecisionLog> {
        return agentDecisionLogDao.getDecisionsSince(timestamp).map { it.toDomain() }
    }
}
