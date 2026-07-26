package com.lingo.learn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lingo.learn.data.local.converter.AppTypeConverters
import com.lingo.learn.data.local.dao.*
import com.lingo.learn.data.local.entity.*

@Database(
    entities = [
        UserProfileEntity::class,
        LearningRecordEntity::class,
        VocabItemEntity::class,
        ErrorBookEntity::class,
        PlanEntity::class,
        QuizResultEntity::class,
        TokenUsageLogEntity::class,
        TtsCacheEntity::class,
        DailyStreakEntity::class,
        ThemeUnitEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun learningRecordDao(): LearningRecordDao
    abstract fun vocabItemDao(): VocabItemDao
    abstract fun errorBookDao(): ErrorBookDao
    abstract fun planDao(): PlanDao
    abstract fun quizResultDao(): QuizResultDao
    abstract fun tokenUsageLogDao(): TokenUsageLogDao
    abstract fun ttsCacheDao(): TtsCacheDao
    abstract fun dailyStreakDao(): DailyStreakDao
    abstract fun themeUnitDao(): ThemeUnitDao
}
