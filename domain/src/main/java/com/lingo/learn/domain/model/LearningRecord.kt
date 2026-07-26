package com.lingo.learn.domain.model

data class LearningRecord(
    val id: String,
    val taskId: String,
    val timestamp: Long,
    val taskType: String,              // IMMERSION / SPEAKING / GAME / QUIZ
    val accuracy: Float,
    val duration: Long,                // Learning duration in seconds
    val score: Int,
    val streakDays: Int,
    val lastModified: Long = System.currentTimeMillis()
)
