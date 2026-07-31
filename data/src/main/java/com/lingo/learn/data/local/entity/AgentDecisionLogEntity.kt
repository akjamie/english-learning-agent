package org.akj.lingo.learn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.akj.lingo.learn.domain.model.AgentDecisionLog

@Entity(tableName = "agent_decision_log")
data class AgentDecisionLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val decisionType: String,
    val title: String,
    val description: String,
    val metadata: String,
    val confidence: Float,
    val lastModified: Long
) {
    fun toDomain() = AgentDecisionLog(
        id = id,
        timestamp = timestamp,
        decisionType = decisionType,
        title = title,
        description = description,
        metadata = metadata,
        confidence = confidence,
        lastModified = lastModified
    )

    companion object {
        fun fromDomain(domain: AgentDecisionLog) = AgentDecisionLogEntity(
            id = domain.id,
            timestamp = domain.timestamp,
            decisionType = domain.decisionType,
            title = domain.title,
            description = domain.description,
            metadata = domain.metadata,
            confidence = domain.confidence,
            lastModified = domain.lastModified
        )
    }
}
