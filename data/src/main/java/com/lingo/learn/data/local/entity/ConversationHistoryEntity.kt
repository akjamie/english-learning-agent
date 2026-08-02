package org.akj.lingo.learn.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.akj.lingo.learn.domain.model.ConversationMessage

@Entity(
    tableName = "conversation_history",
    indices = [Index(value = ["scenarioId", "timestamp"])]
)
data class ConversationHistoryEntity(
    @PrimaryKey val id: String,
    val scenarioId: String,
    val role: String,
    val content: String,
    val timestamp: Long
) {
    fun toDomain() = ConversationMessage(
        id = id,
        scenarioId = scenarioId,
        role = role,
        content = content,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(message: ConversationMessage) = ConversationHistoryEntity(
            id = message.id,
            scenarioId = message.scenarioId,
            role = message.role,
            content = message.content,
            timestamp = message.timestamp
        )
    }
}
