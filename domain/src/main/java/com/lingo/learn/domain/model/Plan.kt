package org.akj.lingo.learn.domain.model

data class Plan(
    val id: String,
    val type: String,                  // WEEKLY / MONTHLY
    val startDate: Long,
    val endDate: Long,
    val theme: String,                 // Unit theme, e.g., "Weather"
    val difficultyCoefficient: Float,
    val reviewRatio: Float,
    val speechTopics: String,          // Conversation practice topics
    val weeklyTarget: String,          // Vocabulary and capacity target
    val snapshotData: String,          // Learning status snapshot used as generation basis
    val dialogueOutput: String?,       // Agent-generated high frequency word dialogue JSON
    val rationaleSnapshot: String? = null, // Per-day rationale JSON from LLM, used for "Why this arrangement?" annotation
    val status: String,                // NOT_STARTED / IN_PROGRESS / COMPLETED
    val lastModified: Long = System.currentTimeMillis()
)
