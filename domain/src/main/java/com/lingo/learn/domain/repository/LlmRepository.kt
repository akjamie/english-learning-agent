package org.akj.lingo.learn.domain.repository

interface LlmRepository {
    suspend fun complete(
        prompt: String,
        taskType: String, // PLAN / REPORT / ENCOURAGEMENT / EXPLAIN
        maxTokens: Int = 500
    ): Result<String>
}
