package com.lingo.learn.data.di

import com.lingo.learn.data.repository.*
import com.lingo.learn.domain.repository.*
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
}
