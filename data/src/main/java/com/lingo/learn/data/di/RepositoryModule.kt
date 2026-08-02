package org.akj.lingo.learn.data.di

import org.akj.lingo.learn.data.repository.*
import org.akj.lingo.learn.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLlmRepository(
        impl: LlmRepositoryImpl
    ): LlmRepository

    @Binds
    @Singleton
    abstract fun bindTtsRepository(
        impl: TtsRepositoryImpl
    ): TtsRepository

    @Binds
    @Singleton
    abstract fun bindAsrRepository(
        impl: AsrRepositoryImpl
    ): AsrRepository

    @Binds
    @Singleton
    abstract fun bindConfigRepository(
        impl: ConfigRepositoryImpl
    ): ConfigRepository

    @Binds
    @Singleton
    abstract fun bindWeeklyPlanRepository(
        impl: WeeklyPlanRepositoryImpl
    ): WeeklyPlanRepository

    @Binds
    @Singleton
    abstract fun bindLearningRecordRepository(
        impl: LearningRecordRepositoryImpl
    ): LearningRecordRepository

    @Binds
    @Singleton
    abstract fun bindErrorBookRepository(
        impl: ErrorBookRepositoryImpl
    ): ErrorBookRepository

    @Binds
    @Singleton
    abstract fun bindAgentDecisionLogRepository(
        impl: AgentDecisionLogRepositoryImpl
    ): AgentDecisionLogRepository

    @Binds
    @Singleton
    abstract fun bindGamificationRepository(
        impl: GamificationRepositoryImpl
    ): GamificationRepository

    @Binds
    @Singleton
    abstract fun bindConversationRepository(
        impl: ConversationRepositoryImpl
    ): ConversationRepository
}
