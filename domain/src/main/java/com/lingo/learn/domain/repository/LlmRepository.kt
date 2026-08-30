package org.akj.lingo.learn.domain.repository

import kotlinx.coroutines.flow.Flow
import org.akj.lingo.learn.domain.model.ChatMessage

interface LlmRepository {
    suspend fun complete(
        prompt: String,
        taskType: String, // PLAN / REPORT / ENCOURAGEMENT / EXPLAIN / HINT
        maxTokens: Int = 500
    ): Result<String>

    suspend fun chat(
        messages: List<ChatMessage>,
        taskType: String,
        maxTokens: Int = 500
    ): Result<String>

    fun completeStream(
        prompt: String,
        taskType: String,
        maxTokens: Int = 500
    ): Flow<String>
}

