package com.lingo.learn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lingo.learn.domain.model.QuizResult

@Entity(tableName = "quiz_result")
data class QuizResultEntity(
    @PrimaryKey val id: String,
    val quizType: String,
    val timestamp: Long,
    val score: Int,
    val totalScore: Int,
    val breakdownJson: String,
    val errorItemIds: String
) {
    fun toDomain() = QuizResult(
        id = id,
        quizType = quizType,
        timestamp = timestamp,
        score = score,
        totalScore = totalScore,
        breakdownJson = breakdownJson,
        errorItemIds = errorItemIds
    )

    companion object {
        fun fromDomain(domain: QuizResult) = QuizResultEntity(
            id = domain.id,
            quizType = domain.quizType,
            timestamp = domain.timestamp,
            score = domain.score,
            totalScore = domain.totalScore,
            breakdownJson = domain.breakdownJson,
            errorItemIds = domain.errorItemIds
        )
    }
}
