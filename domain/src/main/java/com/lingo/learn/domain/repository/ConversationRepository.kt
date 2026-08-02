package org.akj.lingo.learn.domain.repository

import org.akj.lingo.learn.domain.model.ConversationMessage

/**
 * Sprint 11 — Persists roleplay conversation history so Lingo can "remember"
 * what was discussed last time (agent presence).
 */
interface ConversationRepository {
    /** Inserts (or replaces by id) a conversation message. */
    suspend fun insertMessage(message: ConversationMessage)

    /** Returns the most recent messages for a scenario, oldest first. */
    suspend fun getRecentMessages(scenarioId: String, limit: Int): List<ConversationMessage>

    /** Clears history for a scenario (e.g. user starts fresh). */
    suspend fun clearScenario(scenarioId: String)
}
