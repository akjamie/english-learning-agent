package org.akj.lingo.learn.data.repository

import org.akj.lingo.learn.data.local.dao.ConversationHistoryDao
import org.akj.lingo.learn.data.local.entity.ConversationHistoryEntity
import org.akj.lingo.learn.domain.model.ConversationMessage
import org.akj.lingo.learn.domain.repository.ConversationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val dao: ConversationHistoryDao
) : ConversationRepository {

    override suspend fun insertMessage(message: ConversationMessage) {
        dao.insert(ConversationHistoryEntity.fromDomain(message))
    }

    override suspend fun getRecentMessages(scenarioId: String, limit: Int): List<ConversationMessage> {
        return dao.getRecent(scenarioId, limit).reversed().map { it.toDomain() }
    }

    override suspend fun clearScenario(scenarioId: String) {
        dao.clearScenario(scenarioId)
    }
}
