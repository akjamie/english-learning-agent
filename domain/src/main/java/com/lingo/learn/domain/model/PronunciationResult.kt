package org.akj.lingo.learn.domain.model

data class PronunciationResult(
    val overallScore: Int,
    val wordScores: List<WordScore>,
    val feedback: String,
    val isFromFallback: Boolean
)

data class WordScore(
    val word: String,
    val score: Int
)
