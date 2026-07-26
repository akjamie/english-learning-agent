package com.lingo.learn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lingo.learn.domain.model.Plan

@Entity(tableName = "plan")
data class PlanEntity(
    @PrimaryKey val id: String,
    val type: String,
    val startDate: Long,
    val endDate: Long,
    val theme: String,
    val difficultyCoefficient: Float,
    val reviewRatio: Float,
    val speechTopics: String,
    val weeklyTarget: String,
    val snapshotData: String,
    val dialogueOutput: String?,
    val status: String,
    val lastModified: Long
) {
    fun toDomain() = Plan(
        id = id,
        type = type,
        startDate = startDate,
        endDate = endDate,
        theme = theme,
        difficultyCoefficient = difficultyCoefficient,
        reviewRatio = reviewRatio,
        speechTopics = speechTopics,
        weeklyTarget = weeklyTarget,
        snapshotData = snapshotData,
        dialogueOutput = dialogueOutput,
        status = status,
        lastModified = lastModified
    )

    companion object {
        fun fromDomain(domain: Plan) = PlanEntity(
            id = domain.id,
            type = domain.type,
            startDate = domain.startDate,
            endDate = domain.endDate,
            theme = domain.theme,
            difficultyCoefficient = domain.difficultyCoefficient,
            reviewRatio = domain.reviewRatio,
            speechTopics = domain.speechTopics,
            weeklyTarget = domain.weeklyTarget,
            snapshotData = domain.snapshotData,
            dialogueOutput = domain.dialogueOutput,
            status = domain.status,
            lastModified = domain.lastModified
        )
    }
}
