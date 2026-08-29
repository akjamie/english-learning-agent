package org.akj.lingo.learn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.akj.lingo.learn.data.local.converter.AppTypeConverters
import org.akj.lingo.learn.data.local.dao.*
import org.akj.lingo.learn.data.local.entity.*

@Database(
    entities = [
        LearningRecordEntity::class,
        ErrorBookEntity::class,
        PlanEntity::class,
        TokenUsageLogEntity::class,
        TtsCacheEntity::class,
        AgentDecisionLogEntity::class,
        GamificationStateEntity::class,
        ConversationHistoryEntity::class,
        LlmTraceEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun learningRecordDao(): LearningRecordDao
    abstract fun errorBookDao(): ErrorBookDao
    abstract fun planDao(): PlanDao
    abstract fun tokenUsageLogDao(): TokenUsageLogDao
    abstract fun ttsCacheDao(): TtsCacheDao
    abstract fun agentDecisionLogDao(): AgentDecisionLogDao
    abstract fun gamificationStateDao(): GamificationStateDao
    abstract fun conversationHistoryDao(): ConversationHistoryDao
    abstract fun llmTraceDao(): LlmTraceDao
}
