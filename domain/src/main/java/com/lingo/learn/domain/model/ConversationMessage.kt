package org.akj.lingo.learn.domain.model

/**
 * Sprint 11 — A single persisted roleplay conversation message.
 */
data class ConversationMessage(
    val id: String,
    val scenarioId: String,
    val role: String,          // "system" / "user" / "assistant"
    val content: String,
    val timestamp: Long
)
