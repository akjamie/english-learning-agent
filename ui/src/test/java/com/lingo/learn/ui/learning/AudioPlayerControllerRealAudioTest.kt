package org.akj.lingo.learn.ui.learning

import org.akj.lingo.learn.domain.model.SubtitleLine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Unit tests for [AudioPlayerController] real audio mode (Sprint 13), covering
 * sequential per-line playback, position mapping, mixed-mode degradation,
 * seek, pause/resume, and reset behavior using a fake [AudioPlaybackEngine].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AudioPlayerControllerRealAudioTest {

    private fun createTestSubtitles(): List<SubtitleLine> = listOf(
        SubtitleLine(id = 1, startTimeMs = 0, endTimeMs = 3000, text = "Hello", newWords = listOf("hello")),
        SubtitleLine(id = 2, startTimeMs = 3000, endTimeMs = 6000, text = "World", newWords = listOf("world")),
        SubtitleLine(id = 3, startTimeMs = 6000, endTimeMs = 10000, text = "Goodbye", newWords = listOf("goodbye"))
    )

    private fun tempFile(name: String): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "lingo_audio_test")
        if (!dir.exists()) dir.mkdirs()
        val f = File(dir, "$name.mp3")
        if (!f.exists()) f.writeText("fake audio")
        return f
    }

    @Test
    fun `configureRealAudio sets isRealAudio true when at least one file exists`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())
        val engine = FakeAudioPlaybackEngine()

        controller.configureRealAudio(engine, listOf(tempFile("line1"), null, tempFile("line3")))

        assertTrue(controller.state.value.isRealAudio)
    }

    @Test
    fun `configureRealAudio stays simulated when all files are null`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())
        val engine = FakeAudioPlaybackEngine()

        controller.configureRealAudio(engine, listOf(null, null, null))

        assertFalse(controller.state.value.isRealAudio)
    }

    @Test
    fun `play in real mode triggers engine play for first line and activates subtitle`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())
        val engine = FakeAudioPlaybackEngine()
        controller.configureRealAudio(engine, listOf(tempFile("l1"), tempFile("l2"), tempFile("l3")))

        controller.play()

        assertTrue(controller.state.value.isPlaying)
        assertTrue(controller.state.value.isRealAudio)
        assertEquals(1, engine.sessions.size)
        assertEquals(0, controller.state.value.currentSubtitleIndex)
        // New words from subtitle 0 should be popped
        assertTrue(controller.state.value.poppedNewWords.contains("hello"))
    }

    @Test
    fun `onCompletion advances to next line and plays its file`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())
        val engine = FakeAudioPlaybackEngine()
        controller.configureRealAudio(engine, listOf(tempFile("l1"), tempFile("l2"), tempFile("l3")))

        controller.play()
        engine.simulateCompletion() // line 0 finishes

        assertEquals(2, engine.sessions.size)
        assertEquals(1, controller.state.value.currentSubtitleIndex)
        assertTrue(controller.state.value.poppedNewWords.contains("world"))
    }

    @Test
    fun `onCompletion on last line sets isFinished and stops playback`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())
        val engine = FakeAudioPlaybackEngine()
        controller.configureRealAudio(engine, listOf(tempFile("l1"), tempFile("l2"), tempFile("l3")))

        controller.play()
        engine.simulateCompletion() // line 0 -> line 1
        engine.simulateCompletion() // line 1 -> line 2
        engine.simulateCompletion() // line 2 -> finished

        assertFalse(controller.state.value.isPlaying)
        assertTrue(controller.state.value.isFinished)
        assertEquals(controller.state.value.durationMs, controller.state.value.positionMs)
    }

    @Test
    fun `onPositionUpdate maps to global timeline offset by line start time`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())
        val engine = FakeAudioPlaybackEngine()
        controller.configureRealAudio(engine, listOf(tempFile("l1"), tempFile("l2"), tempFile("l3")))

        controller.play()
        // Simulate position update of 1500ms within line 0 (starts at 0)
        engine.simulatePosition(1500)
        assertEquals(1500L, controller.state.value.positionMs)

        // Advance to line 1 (starts at 3000)
        engine.simulateCompletion()
        engine.simulatePosition(1000)
        // Global position = line1.startTimeMs(3000) + 1000 = 4000
        assertEquals(4000L, controller.state.value.positionMs)
    }

    @Test
    fun `pause in real mode stops engine and sets isPlaying false`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())
        val engine = FakeAudioPlaybackEngine()
        controller.configureRealAudio(engine, listOf(tempFile("l1"), tempFile("l2"), tempFile("l3")))

        controller.play()
        controller.pause()

        assertFalse(controller.state.value.isPlaying)
        assertTrue(engine.isPaused)
    }

    @Test
    fun `play after pause resumes engine instead of restarting`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())
        val engine = FakeAudioPlaybackEngine()
        controller.configureRealAudio(engine, listOf(tempFile("l1"), tempFile("l2"), tempFile("l3")))

        controller.play()
        engine.simulateCompletion() // now on line 1
        controller.pause()
        controller.play() // should resume, not restart

        assertTrue(engine.isResumed)
        // Should not have started a new playback session
        assertEquals(2, engine.sessions.size)
    }

    @Test
    fun `seekTo in real mode jumps to target line and plays it`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())
        val engine = FakeAudioPlaybackEngine()
        controller.configureRealAudio(engine, listOf(tempFile("l1"), tempFile("l2"), tempFile("l3")))

        controller.play()
        // Seek to position 7000 which is in line 2 (6000-10000)
        controller.seekTo(7000L)

        assertEquals(2, controller.state.value.currentSubtitleIndex)
        // Engine should have been stopped and a new session started for line 2
        assertTrue(engine.stopCount >= 1)
        assertEquals(2, engine.sessions.size)
    }

    @Test
    fun `reset stops engine and clears playback state`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())
        val engine = FakeAudioPlaybackEngine()
        controller.configureRealAudio(engine, listOf(tempFile("l1"), tempFile("l2"), tempFile("l3")))

        controller.play()
        controller.reset()

        assertFalse(controller.state.value.isPlaying)
        assertFalse(controller.state.value.isFinished)
        assertEquals(0L, controller.state.value.positionMs)
        assertEquals(-1, controller.state.value.currentSubtitleIndex)
        assertTrue(engine.stopCount >= 1)
        // isRealAudio should be preserved after reset
        assertTrue(controller.state.value.isRealAudio)
    }

    @Test
    fun `setSpeed in real mode updates engine speed`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())
        val engine = FakeAudioPlaybackEngine()
        controller.configureRealAudio(engine, listOf(tempFile("l1"), tempFile("l2"), tempFile("l3")))

        controller.play()
        controller.setSpeed(0.75f)

        assertEquals(0.75f, controller.state.value.speed, 0.01f)
        assertTrue(engine.speeds.contains(0.75f))
    }

    @Test
    fun `mixed mode - null file for a line degrades to simulated playback then advances`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())
        val engine = FakeAudioPlaybackEngine()
        // Line 0 has real audio, line 1 is null (simulated), line 2 has real audio
        controller.configureRealAudio(engine, listOf(tempFile("l1"), null, tempFile("l3")))

        controller.play()
        // Line 0 plays via engine
        assertEquals(1, engine.sessions.size)
        assertEquals(0, controller.state.value.currentSubtitleIndex)

        // Simulate line 0's audio reaching its end (3000ms = line 0 endTime)
        engine.simulatePosition(3000)
        // Line 0 completes -> line 1 has no file -> simulated playback kicks in
        engine.simulateCompletion()
        assertEquals(1, controller.state.value.currentSubtitleIndex)
        assertTrue(controller.state.value.poppedNewWords.contains("world"))

        // Advance virtual time past line 1's duration (position 3000 -> 6000)
        advanceTimeBy(3500)

        // After simulated line completes, line 2 should play via engine
        assertEquals(2, controller.state.value.currentSubtitleIndex)
        assertEquals(2, engine.sessions.size)
    }

    @Test
    fun `clearRealAudio reverts to simulated mode`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())
        val engine = FakeAudioPlaybackEngine()
        controller.configureRealAudio(engine, listOf(tempFile("l1"), tempFile("l2"), tempFile("l3")))

        assertTrue(controller.state.value.isRealAudio)

        controller.clearRealAudio()

        assertFalse(controller.state.value.isRealAudio)
        assertTrue(engine.stopCount >= 1)
    }

    @Test
    fun `setPreparingAudio toggles isPreparingAudio flag`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))

        assertFalse(controller.state.value.isPreparingAudio)

        controller.setPreparingAudio(true)
        assertTrue(controller.state.value.isPreparingAudio)

        controller.setPreparingAudio(false)
        assertFalse(controller.state.value.isPreparingAudio)
    }

    // region Fake Engine

    /**
     * Test double for [AudioPlaybackEngine] that records calls and lets tests
     * drive callback timing explicitly via [simulatePrepared], [simulatePosition],
     * and [simulateCompletion].
     */
    private class FakeAudioPlaybackEngine : AudioPlaybackEngine {
        data class PlaybackSession(
            val file: File,
            val speed: Float,
            val onPrepared: (Long) -> Unit,
            val onPositionUpdate: (Long) -> Unit,
            val onCompletion: () -> Unit
        )

        val sessions = mutableListOf<PlaybackSession>()
        var currentSession: PlaybackSession? = null
            private set
        var isPaused = false
            private set
        var isResumed = false
            private set
        var stopCount = 0
            private set
        val speeds = mutableListOf<Float>()

        override fun play(
            file: File,
            speed: Float,
            onPrepared: (Long) -> Unit,
            onPositionUpdate: (Long) -> Unit,
            onCompletion: () -> Unit
        ) {
            val session = PlaybackSession(file, speed, onPrepared, onPositionUpdate, onCompletion)
            sessions.add(session)
            currentSession = session
            isPaused = false
            isResumed = false
        }

        override fun pause() {
            isPaused = true
        }

        override fun resume() {
            isResumed = true
        }

        override fun stop() {
            stopCount++
            currentSession = null
        }

        override fun setSpeed(speed: Float) {
            speeds.add(speed)
        }

        fun simulatePosition(positionMs: Long) {
            currentSession?.onPositionUpdate?.invoke(positionMs)
        }

        fun simulateCompletion() {
            val s = currentSession
            currentSession = null
            s?.onCompletion?.invoke()
        }
    }

    // endregion
}
