package com.lingo.learn.ui.learning

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Android system [TextToSpeech] as a zero-cost offline fallback for word and
 * sentence pronunciation. Used when the MiniMax TTS API is not configured or fails.
 *
 * Per design doc section 3.4 (TTS degradation strategy): system TTS quality is lower
 * but guarantees the learning flow remains functional without network or API keys.
 */
@Singleton
class SystemTtsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private val isReady = AtomicBoolean(false)

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isReady.set(true)
            }
        }
    }

    /**
     * Speaks the given text. If TTS is not yet ready, the call is silently ignored.
     */
    fun speak(text: String, rate: Float = 1.0f) {
        if (isReady.get()) {
            tts?.apply {
                setSpeechRate(rate)
                speak(text, TextToSpeech.QUEUE_FLUSH, null, "lingo_tts_${System.currentTimeMillis()}")
            }
        }
    }

    /**
     * Releases TTS resources. Call when the learning session is destroyed.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
