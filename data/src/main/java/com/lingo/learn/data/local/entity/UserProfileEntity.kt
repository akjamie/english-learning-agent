package com.lingo.learn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lingo.learn.domain.model.UserProfile

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val grade: String,
    val textbookVersion: String,
    val diagnosticLevel: String,
    val dailyGoalDuration: Int,
    val eyeProtectionThreshold: Int,
    val notificationWindowStart: String,
    val notificationWindowEnd: String,
    val syncStatus: Int,
    val lastModified: Long
) {
    fun toDomain() = UserProfile(
        id = id,
        name = name,
        grade = grade,
        textbookVersion = textbookVersion,
        diagnosticLevel = diagnosticLevel,
        dailyGoalDuration = dailyGoalDuration,
        eyeProtectionThreshold = eyeProtectionThreshold,
        notificationWindowStart = notificationWindowStart,
        notificationWindowEnd = notificationWindowEnd,
        syncStatus = syncStatus,
        lastModified = lastModified
    )

    companion object {
        fun fromDomain(domain: UserProfile) = UserProfileEntity(
            id = domain.id,
            name = domain.name,
            grade = domain.grade,
            textbookVersion = domain.textbookVersion,
            diagnosticLevel = domain.diagnosticLevel,
            dailyGoalDuration = domain.dailyGoalDuration,
            eyeProtectionThreshold = domain.eyeProtectionThreshold,
            notificationWindowStart = domain.notificationWindowStart,
            notificationWindowEnd = domain.notificationWindowEnd,
            syncStatus = domain.syncStatus,
            lastModified = domain.lastModified
        )
    }
}
