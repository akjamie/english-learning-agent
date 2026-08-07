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
    private val isEnglishVoiceAvailable = AtomicBoolean(false)
    private var pendingSpeakRequest: Pair<String, Float>? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                var result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    result = tts?.setLanguage(Locale.ENGLISH)
                }
                // Sprint 20: only mark the engine usable when an English
                // voice actually loaded — otherwise speak() must return
                // false so callers can surface a warning instead of
                // silently producing no sound.
                isEnglishVoiceAvailable.set(
                    result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE
                )
                isReady.set(true)
                // Flush any pending speak request queued during async init
                pendingSpeakRequest?.let { (text, rate) ->
                    speak(text, rate)
                    pendingSpeakRequest = null
                }
            } else {
                // Engine failed to initialize at all — mark ready so the
                // pending queue does not wait forever; speak() returns false.
                isEnglishVoiceAvailable.set(false)
                isReady.set(true)
            }
        }
    }

    /**
     * Speaks the given text. Queues the request if TTS is still initializing.
     * Returns true when audio will actually be produced, false when no
     * English voice is installed (caller should surface a warning).
     */
    fun speak(text: String, rate: Float = 1.0f): Boolean {
        if (text.isBlank()) return false

        if (!isEnglishVoiceAvailable.get()) return false

        if (isReady.get()) {
            tts?.apply {
                stop()
                setSpeechRate(rate)
                speak(text, TextToSpeech.QUEUE_FLUSH, null, "lingo_tts_${System.currentTimeMillis()}")
            }
            return true
        } else {
            pendingSpeakRequest = Pair(text, rate)
            return true
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
