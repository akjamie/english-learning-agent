package com.lingo.learn.domain.model

data class UserProfile(
    val id: String,
    val name: String,
    val grade: String,                  // e.g., "Grade 4" / "Senior 1"
    val textbookVersion: String,        // e.g., "PEP" / "New Target"
    val diagnosticLevel: String,        // e.g., "A" / "B" / "C"
    val dailyGoalDuration: Int = 18,    // Daily goal in minutes, defaults to 18
    val eyeProtectionThreshold: Int = 30,// Eye protection limit in minutes, defaults to 30
    val notificationWindowStart: String = "17:30", // Notification push start time
    val notificationWindowEnd: String = "20:00",   // Notification push end time
    val syncStatus: Int = 0,            // 0 = local, 1 = synced
    val lastModified: Long = System.currentTimeMillis()
)
