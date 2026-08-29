package org.akj.lingo.learn.data.repository

import org.akj.lingo.learn.data.local.dao.LlmTraceDao
import org.akj.lingo.learn.domain.model.LlmTrace
import org.akj.lingo.learn.domain.repository.LlmTraceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmTraceRepositoryImpl @Inject constructor(
    private val llmTraceDao: LlmTraceDao
) : LlmTraceRepository {

    override suspend fun getRecentTraces(limit: Int): List<LlmTrace> =
        llmTraceDao.getRecentTraces(limit).map { it.toDomain() }
}
