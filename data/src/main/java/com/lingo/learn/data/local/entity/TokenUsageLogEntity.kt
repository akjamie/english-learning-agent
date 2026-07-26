package com.lingo.learn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lingo.learn.domain.model.TokenUsageLog

@Entity(tableName = "token_usage_log")
data class TokenUsageLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val taskType: String,
    val model: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int
) {
    fun toDomain() = TokenUsageLog(
        id = id,
        timestamp = timestamp,
        taskType = taskType,
        model = model,
        inputTokens = inputTokens,
        outputTokens = outputTokens,
        totalTokens = totalTokens
    )

    companion object {
        fun fromDomain(domain: TokenUsageLog) = TokenUsageLogEntity(
            id = domain.id,
            timestamp = domain.timestamp,
            taskType = domain.taskType,
            model = domain.model,
            inputTokens = domain.inputTokens,
            outputTokens = domain.outputTokens,
            totalTokens = domain.totalTokens
        )
    }
}
