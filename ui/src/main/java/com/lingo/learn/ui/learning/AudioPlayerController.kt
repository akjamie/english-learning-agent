package org.akj.lingo.learn.ui.learning

import org.akj.lingo.learn.domain.model.SubtitleLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages audio playback state for the immersive audio stage.
 *
 * In MVP mode (no real TTS audio files available), playback is simulated with a
 * coroutine timer that advances [positionMs] at the configured [speed]. This keeps
 * subtitle highlighting, new-word popups, and progress bar fully functional for
 * demo purposes. When a real audio file path is provided, it can be extended to
 * use Android [android.media.MediaPlayer].
 */
class AudioPlayerController(
    private val scope: CoroutineScope
) {
    data class AudioState(
        val isPlaying: Boolean = false,
        val positionMs: Long = 0L,
        val durationMs: Long = 20_000L,
        val speed: Float = 1.0f,
        val currentSubtitleIndex: Int = -1,
        val poppedNewWords: List<String> = emptyList(),
        val isFinished: Boolean = false
    )

    private val _state = MutableStateFlow(AudioState())
    val state: StateFlow<AudioState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var subtitles: List<SubtitleLine> = emptyList()

    /** Tick interval in milliseconds for the simulation loop. */
    private val tickIntervalMs = 50L

    /**
     * Loads subtitle lines and sets the total duration based on the last subtitle end time.
     */
    fun loadSubtitles(lines: List<SubtitleLine>) {
        subtitles = lines
        val duration = lines.maxOfOrNull { it.endTimeMs } ?: 20_000L
        _state.value = _state.value.copy(durationMs = duration)
    }

    fun play() {
        val current = _state.value
        if (current.isFinished) {
            // Restart from beginning if previously finished
            _state.value = current.copy(positionMs = 0L, isFinished = false, currentSubtitleIndex = -1)
        }
        _state.value = _state.value.copy(isPlaying = true)
        startTicking()
    }

    fun pause() {
        _state.value = _state.value.copy(isPlaying = false)
        tickJob?.cancel()
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) pause() else play()
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _state.value.durationMs)
        _state.value = _state.value.copy(positionMs = clamped)
        updateSubtitleState(clamped)
    }

    fun setSpeed(speed: Float) {
        _state.value = _state.value.copy(speed = speed)
    }

    fun reset() {
        tickJob?.cancel()
        _state.value = AudioState(durationMs = subtitles.maxOfOrNull { it.endTimeMs } ?: 20_000L)
    }

    /**
     * Marks a popped new word as dismissed by the user (collected into a bubble).
     */
    fun dismissNewWord(word: String) {
        val current = _state.value
        if (word in current.poppedNewWords) {
            _state.value = current.copy(
                poppedNewWords = current.poppedNewWords - word
            )
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val current = _state.value
                if (!current.isPlaying) break

                val newPos = current.positionMs + (tickIntervalMs * current.speed).toLong()
                if (newPos >= current.durationMs) {
                    _state.value = current.copy(
                        positionMs = current.durationMs,
                        isPlaying = false,
                        isFinished = true
                    )
                    break
                }

                _state.value = current.copy(positionMs = newPos)
                updateSubtitleState(newPos)
                delay(tickIntervalMs)
            }
        }
    }

    private fun updateSubtitleState(positionMs: Long) {
        val current = _state.value
        val activeIndex = subtitles.indexOfFirst { line ->
            positionMs >= line.startTimeMs && positionMs < line.endTimeMs
        }

        if (activeIndex != current.currentSubtitleIndex && activeIndex >= 0) {
            // A new subtitle line became active; pop its new words
            val newWords = subtitles[activeIndex].newWords
            val mergedPopped = (current.poppedNewWords + newWords).distinct()
            _state.value = current.copy(
                currentSubtitleIndex = activeIndex,
                poppedNewWords = mergedPopped
            )
        }
    }
}
