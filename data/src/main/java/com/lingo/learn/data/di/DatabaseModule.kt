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
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "english_learning_agent_db"
        ).build()
    }

    @Provides
    fun provideUserProfileDao(db: AppDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    fun provideLearningRecordDao(db: AppDatabase): LearningRecordDao = db.learningRecordDao()

    @Provides
    fun provideVocabItemDao(db: AppDatabase): VocabItemDao = db.vocabItemDao()

    @Provides
    fun provideErrorBookDao(db: AppDatabase): ErrorBookDao = db.errorBookDao()

    @Provides
    fun providePlanDao(db: AppDatabase): PlanDao = db.planDao()

    @Provides
    fun provideQuizResultDao(db: AppDatabase): QuizResultDao = db.quizResultDao()

    @Provides
    fun provideTokenUsageLogDao(db: AppDatabase): TokenUsageLogDao = db.tokenUsageLogDao()

    @Provides
    fun provideTtsCacheDao(db: AppDatabase): TtsCacheDao = db.ttsCacheDao()

    @Provides
    fun provideDailyStreakDao(db: AppDatabase): DailyStreakDao = db.dailyStreakDao()

    @Provides
    fun provideThemeUnitDao(db: AppDatabase): ThemeUnitDao = db.themeUnitDao()
}
