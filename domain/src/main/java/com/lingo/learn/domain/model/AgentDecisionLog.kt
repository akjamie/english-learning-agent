package org.akj.lingo.learn.domain.model

data class AgentDecisionLog(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val decisionType: String,
    val title: String,
    val description: String,
    val metadata: String = "{}",
    val confidence: Float = 1.0f,
    val lastModified: Long = System.currentTimeMillis()
)
