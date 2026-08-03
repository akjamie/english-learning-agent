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
import java.io.File

/**
 * Manages audio playback state for the immersive audio stage.
 *
 * Supports two playback modes:
 * 1. **Real audio mode** (Sprint 13): Plays actual TTS-synthesized audio files
 *    sequentially per subtitle line via an [AudioPlaybackEngine]. Subtitle
 *    highlighting and new-word popups are synced to real playback progress.
 * 2. **Simulated mode** (MVP fallback): Advances [positionMs] with a coroutine
 *    timer. Used when TTS is unavailable (offline) or synthesis fails.
 *
 * Lines without a real audio file degrade to simulated playback for that line's
 * duration, enabling mixed-mode playback (some lines real, some simulated).
 *
 * @param scope the coroutine scope used for simulated ticking
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
        val isFinished: Boolean = false,
        /** Whether real TTS audio files are loaded for playback. */
        val isRealAudio: Boolean = false,
        /** Whether TTS audio is being pre-synthesized (loading indicator). */
        val isPreparingAudio: Boolean = false
    )

    private val _state = MutableStateFlow(AudioState())
    val state: StateFlow<AudioState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var subtitles: List<SubtitleLine> = emptyList()

    /** Tick interval in milliseconds for the simulation loop. */
    private val tickIntervalMs = 50L

    // region Real audio mode state

    private var engine: AudioPlaybackEngine? = null
    private var audioFiles: List<File?> = emptyList()
    private var currentLineIndex = -1
    /** True when the engine is actively playing a real audio file (not simulating). */
    private var engineIsActive = false

    // endregion

    /**
     * Loads subtitle lines and sets the total duration based on the last subtitle end time.
     */
    fun loadSubtitles(lines: List<SubtitleLine>) {
        subtitles = lines
        val duration = lines.maxOfOrNull { it.endTimeMs } ?: 20_000L
        _state.value = _state.value.copy(durationMs = duration)
    }

    /**
     * Configures the controller for real TTS audio playback.
     *
     * [files] is a per-subtitle-line list (same order as the loaded subtitles);
     * null entries fall back to simulated playback for that line. Call after
     * [loadSubtitles]. If no files are available, the controller stays in
     * simulated mode.
     */
    fun configureRealAudio(engine: AudioPlaybackEngine, files: List<File?>) {
        this.engine = engine
        this.audioFiles = files
        currentLineIndex = -1
        engineIsActive = false
        val hasReal = files.any { it != null && it.exists() }
        _state.value = _state.value.copy(isRealAudio = hasReal)
    }

    /** Clears real-audio configuration, reverting to simulated mode. */
    fun clearRealAudio() {
        engine?.stop()
        engine = null
        audioFiles = emptyList()
        currentLineIndex = -1
        engineIsActive = false
        _state.value = _state.value.copy(isRealAudio = false)
    }

    /** Toggles the audio-preparing loading indicator. */
    fun setPreparingAudio(preparing: Boolean) {
        _state.value = _state.value.copy(isPreparingAudio = preparing)
    }

    fun play() {
        val current = _state.value
        if (current.isFinished) {
            _state.value = current.copy(
                positionMs = 0L,
                isFinished = false,
                currentSubtitleIndex = -1
            )
            currentLineIndex = -1
        }
        _state.value = _state.value.copy(isPlaying = true)
        if (current.isRealAudio && engine != null) {
            if (engineIsActive) {
                engine?.resume()
            } else {
                startRealPlayback()
            }
        } else {
            startTicking()
        }
    }

    fun pause() {
        _state.value = _state.value.copy(isPlaying = false)
        tickJob?.cancel()
        if (engineIsActive) {
            engine?.pause()
        }
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) pause() else play()
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _state.value.durationMs)
        _state.value = _state.value.copy(positionMs = clamped)

        if (_state.value.isRealAudio && engine != null) {
            val newIndex = subtitles.indexOfFirst { clamped >= it.startTimeMs && clamped < it.endTimeMs }
            if (newIndex >= 0 && newIndex != currentLineIndex) {
                engine?.stop()
                engineIsActive = false
                currentLineIndex = newIndex
                activateSubtitle(newIndex)
                if (_state.value.isPlaying) {
                    playLine(newIndex)
                }
            } else {
                updateSubtitleState(clamped)
            }
        } else {
            updateSubtitleState(clamped)
        }
    }

    fun setSpeed(speed: Float) {
        _state.value = _state.value.copy(speed = speed)
        if (_state.value.isRealAudio) {
            engine?.setSpeed(speed)
        }
    }

    fun reset() {
        tickJob?.cancel()
        engine?.stop()
        engineIsActive = false
        currentLineIndex = -1
        _state.value = AudioState(
            durationMs = subtitles.maxOfOrNull { it.endTimeMs } ?: 20_000L,
            isRealAudio = audioFiles.any { it != null && it.exists() },
            speed = _state.value.speed
        )
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

    // region Real audio playback

    private fun startRealPlayback() {
        if (engine == null) return
        if (currentLineIndex < 0) {
            currentLineIndex = subtitles.indexOfFirst { _state.value.positionMs < it.endTimeMs }
            if (currentLineIndex < 0) currentLineIndex = 0
        }
        if (currentLineIndex >= subtitles.size) {
            _state.value = _state.value.copy(isPlaying = false, isFinished = true)
            return
        }
        playLine(currentLineIndex)
    }

    private fun playLine(index: Int) {
        if (index >= subtitles.size) {
            _state.value = _state.value.copy(isPlaying = false, isFinished = true)
            return
        }
        currentLineIndex = index
        val line = subtitles[index]
        activateSubtitle(index)

        val file = audioFiles.getOrNull(index)
        val eng = engine
        if (file != null && file.exists() && eng != null) {
            engineIsActive = true
            eng.play(
                file = file,
                speed = _state.value.speed,
                onPrepared = { /* duration known; global duration stays from subtitles */ },
                onPositionUpdate = { pos ->
                    val globalPos = (line.startTimeMs + pos).coerceAtMost(_state.value.durationMs)
                    _state.value = _state.value.copy(positionMs = globalPos)
                },
                onCompletion = {
                    engineIsActive = false
                    val nextIndex = index + 1
                    if (nextIndex >= subtitles.size || !_state.value.isPlaying) {
                        if (nextIndex >= subtitles.size) {
                            _state.value = _state.value.copy(
                                isPlaying = false,
                                isFinished = true,
                                positionMs = _state.value.durationMs
                            )
                        }
                    } else {
                        playLine(nextIndex)
                    }
                }
            )
        } else {
            // No real audio for this line - simulate its duration then advance
            engineIsActive = false
            simulateLine(index)
        }
    }

    /**
     * Simulates playback of a single subtitle line's duration (used when no real
     * audio file exists for that line in mixed mode).
     */
    private fun simulateLine(index: Int) {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val current = _state.value
                if (!current.isPlaying) break
                val line = subtitles[index]
                val newPos = current.positionMs + (tickIntervalMs * current.speed).toLong()
                if (newPos >= line.endTimeMs) {
                    _state.value = current.copy(positionMs = line.endTimeMs)
                    val nextIndex = index + 1
                    if (nextIndex >= subtitles.size) {
                        _state.value = _state.value.copy(
                            isPlaying = false,
                            isFinished = true,
                            positionMs = _state.value.durationMs
                        )
                    } else {
                        playLine(nextIndex)
                    }
                    break
                }
                _state.value = current.copy(positionMs = newPos)
                delay(tickIntervalMs)
            }
        }
    }

    // endregion

    // region Simulated playback (offline fallback)

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

    // endregion

    private fun activateSubtitle(index: Int) {
        val current = _state.value
        if (index == current.currentSubtitleIndex) return
        val newWords = subtitles.getOrNull(index)?.newWords ?: emptyList()
        val mergedPopped = (current.poppedNewWords + newWords).distinct()
        _state.value = current.copy(
            currentSubtitleIndex = index,
            poppedNewWords = mergedPopped
        )
    }

    private fun updateSubtitleState(positionMs: Long) {
        val activeIndex = subtitles.indexOfFirst { line ->
            positionMs >= line.startTimeMs && positionMs < line.endTimeMs
        }
        if (activeIndex >= 0) {
            activateSubtitle(activeIndex)
        }
    }
}
