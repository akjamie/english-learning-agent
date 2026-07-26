package com.lingo.learn.domain.model

data class DailyStreak(
    val date: String,                  // "yyyy-MM-dd"
    val completed: Boolean,
    val usedMakeupCard: Boolean,
    val currentStreak: Int
)
