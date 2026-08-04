package org.akj.lingo.learn.data.di

import android.content.Context
import androidx.room.Room
import org.akj.lingo.learn.data.local.AppDatabase
import org.akj.lingo.learn.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE error_book ADD COLUMN nextReviewTimestamp INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plan ADD COLUMN rationaleSnapshot TEXT DEFAULT NULL")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `agent_decision_log` (
                        `id` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `decisionType` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `metadata` TEXT NOT NULL DEFAULT '{}',
                        `confidence` REAL NOT NULL DEFAULT 1.0,
                        `lastModified` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """)
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `gamification_state` (
                        `id` INTEGER NOT NULL,
                        `totalXp` INTEGER NOT NULL,
                        `makeupMonthKey` TEXT,
                        `makeupCardsUsed` INTEGER NOT NULL,
                        `dailyGoalsDate` TEXT,
                        `sessionGoalAchieved` INTEGER NOT NULL,
                        `accuracyGoalAchieved` INTEGER NOT NULL,
                        `wordsGoalAchieved` INTEGER NOT NULL,
                        `lastModified` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """)
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `conversation_history` (
                        `id` TEXT NOT NULL,
                        `scenarioId` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """)
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_conversation_history_scenarioId_timestamp` " +
                        "ON `conversation_history` (`scenarioId`, `timestamp`)"
                )
            }
        }

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "english_learning_agent_db"
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideLearningRecordDao(db: AppDatabase): LearningRecordDao = db.learningRecordDao()

    @Provides
    fun provideErrorBookDao(db: AppDatabase): ErrorBookDao = db.errorBookDao()

    @Provides
    fun providePlanDao(db: AppDatabase): PlanDao = db.planDao()

    @Provides
    fun provideTokenUsageLogDao(db: AppDatabase): TokenUsageLogDao = db.tokenUsageLogDao()

    @Provides
    fun provideTtsCacheDao(db: AppDatabase): TtsCacheDao = db.ttsCacheDao()

    @Provides
    fun provideAgentDecisionLogDao(db: AppDatabase): AgentDecisionLogDao = db.agentDecisionLogDao()

    @Provides
    fun provideGamificationStateDao(db: AppDatabase): GamificationStateDao = db.gamificationStateDao()

    @Provides
    fun provideConversationHistoryDao(db: AppDatabase): ConversationHistoryDao = db.conversationHistoryDao()
}
