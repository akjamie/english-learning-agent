package org.akj.lingo.learn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.akj.lingo.learn.domain.model.ErrorBookEntry

@Entity(tableName = "error_book")
data class ErrorBookEntity(
    @PrimaryKey val id: String,
    val vocabId: String,
    val errorCount: Int,
    val errorType: String,
    val lastErrorTimestamp: Long,
    val questionType: String,
    val priorityScore: Float,
    val status: String,
    val consecutiveCorrectCount: Int,
    val graduationCheckTimestamp: Long,
    val historyJson: String,
    val lastModified: Long
) {
    fun toDomain() = ErrorBookEntry(
        id = id,
        vocabId = vocabId,
        errorCount = errorCount,
        errorType = errorType,
        lastErrorTimestamp = lastErrorTimestamp,
        questionType = questionType,
        priorityScore = priorityScore,
        status = status,
        consecutiveCorrectCount = consecutiveCorrectCount,
        graduationCheckTimestamp = graduationCheckTimestamp,
        historyJson = historyJson,
        lastModified = lastModified
    )

    companion object {
        fun fromDomain(domain: ErrorBookEntry) = ErrorBookEntity(
            id = domain.id,
            vocabId = domain.vocabId,
            errorCount = domain.errorCount,
            errorType = domain.errorType,
            lastErrorTimestamp = domain.lastErrorTimestamp,
            questionType = domain.questionType,
            priorityScore = domain.priorityScore,
            status = domain.status,
            consecutiveCorrectCount = domain.consecutiveCorrectCount,
            graduationCheckTimestamp = domain.graduationCheckTimestamp,
            historyJson = domain.historyJson,
            lastModified = domain.lastModified
        )
    }
}
