package com.lingo.learn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lingo.learn.domain.model.LearningRecord

@Entity(tableName = "learning_record")
data class LearningRecordEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val timestamp: Long,
    val taskType: String,
    val accuracy: Float,
    val duration: Long,
    val score: Int,
    val streakDays: Int,
    val lastModified: Long
) {
    fun toDomain() = LearningRecord(
        id = id,
        taskId = taskId,
        timestamp = timestamp,
        taskType = taskType,
        accuracy = accuracy,
        duration = duration,
        score = score,
        streakDays = streakDays,
        lastModified = lastModified
    )

    companion object {
        fun fromDomain(domain: LearningRecord) = LearningRecordEntity(
            id = domain.id,
            taskId = domain.taskId,
            timestamp = domain.timestamp,
            taskType = domain.taskType,
            accuracy = domain.accuracy,
            duration = domain.duration,
            score = domain.score,
            streakDays = domain.streakDays,
            lastModified = domain.lastModified
        )
    }
}
