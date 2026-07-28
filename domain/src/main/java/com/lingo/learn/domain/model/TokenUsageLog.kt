package org.akj.lingo.learn.domain.model

data class TokenUsageLog(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val taskType: String,              // PLAN / REPORT / ENCOURAGEMENT ...
    val model: String,                 // primary / fallback
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int
)
