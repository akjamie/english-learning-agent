package org.akj.lingo.learn.ui.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.akj.lingo.learn.ui.learning.AudioPlaybackEngine
import org.akj.lingo.learn.ui.learning.MediaPlayerAudioEngine
import javax.inject.Singleton

/**
 * Binds the [AudioPlaybackEngine] abstraction to its production [MediaPlayerAudioEngine]
 * implementation. Tests inject a fake engine directly into [AudioPlayerController].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UiModule {

    @Binds
    @Singleton
    abstract fun bindAudioPlaybackEngine(
        impl: MediaPlayerAudioEngine
    ): AudioPlaybackEngine
}
