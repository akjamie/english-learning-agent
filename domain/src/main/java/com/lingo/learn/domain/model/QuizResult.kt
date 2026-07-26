package com.lingo.learn.domain.model

data class QuizResult(
    val id: String,
    val quizType: String,              // MICRO_QUIZ / WEEKLY_QUIZ / MONTHLY_QUIZ / DIAGNOSTIC
    val timestamp: Long,
    val score: Int,
    val totalScore: Int,
    val breakdownJson: String,         // Score breakdown by dimension in JSON format
    val errorItemIds: String           // List of wrong word IDs (comma-separated)
)
