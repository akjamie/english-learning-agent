package org.akj.lingo.learn.ui.learning

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Android system [TextToSpeech] as a zero-cost offline fallback for word and
 * sentence pronunciation. Handles asynchronous initialization with a pending speak queue.
 */
@Singleton
class SystemTtsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private val isReady = AtomicBoolean(false)
    private var pendingSpeakRequest: Pair<String, Float>? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
                isReady.set(true)
                // Flush any pending speak request queued during async init
                pendingSpeakRequest?.let { (text, rate) ->
                    speak(text, rate)
                    pendingSpeakRequest = null
                }
            }
        }
    }

    /**
     * Speaks the given text. Queues the request if TTS is still initializing.
     */
    fun speak(text: String, rate: Float = 1.0f) {
        if (text.isBlank()) return

        if (isReady.get()) {
            tts?.apply {
                stop()
                setSpeechRate(rate)
                speak(text, TextToSpeech.QUEUE_FLUSH, null, "lingo_tts_${System.currentTimeMillis()}")
            }
        } else {
            pendingSpeakRequest = Pair(text, rate)
        }
    }

    /**
     * Releases TTS resources.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
