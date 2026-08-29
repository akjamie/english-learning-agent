package org.akj.lingo.learn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.akj.lingo.learn.domain.model.LlmTrace

@Entity(tableName = "llm_trace")
data class LlmTraceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val taskType: String,
    val model: String,
    val success: Boolean,
    val durationMs: Long,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val detail: String
) {
    fun toDomain() = LlmTrace(
        id = id,
        timestamp = timestamp,
        taskType = taskType,
        model = model,
        success = success,
        durationMs = durationMs,
        inputTokens = inputTokens,
        outputTokens = outputTokens,
        totalTokens = totalTokens,
        detail = detail
    )
}
