package org.akj.lingo.learn.domain.model

data class ChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)
