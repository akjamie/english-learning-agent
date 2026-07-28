package org.akj.lingo.learn.ui.learning

import org.akj.lingo.learn.domain.model.SubtitleLine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for [AudioPlayerController], verifying subtitle synchronization,
 * new-word popup behavior, and simulated playback progression.
 *
 * Per implementation_plan.md: unit tests for AudioPlayerViewModel (audio player
 * logic) must pass before Sprint 2 can be marked complete.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AudioPlayerControllerTest {

    private fun createTestSubtitles(): List<SubtitleLine> = listOf(
        SubtitleLine(id = 1, startTimeMs = 0, endTimeMs = 3000, text = "Hello", newWords = listOf("hello")),
        SubtitleLine(id = 2, startTimeMs = 3000, endTimeMs = 6000, text = "World", newWords = listOf("world")),
        SubtitleLine(id = 3, startTimeMs = 6000, endTimeMs = 10000, text = "Goodbye", newWords = listOf("goodbye"))
    )

    @Test
    fun `loadSubtitles sets duration to last subtitle end time`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        val subtitles = createTestSubtitles()

        controller.loadSubtitles(subtitles)

        assertEquals(10000L, controller.state.value.durationMs)
    }

    @Test
    fun `loadSubtitles with empty list defaults to 20 second duration`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))

        controller.loadSubtitles(emptyList())

        assertEquals(20_000L, controller.state.value.durationMs)
    }

    @Test
    fun `seekTo updates position and activates correct subtitle`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())

        controller.seekTo(4000L)

        assertEquals(4000L, controller.state.value.positionMs)
        // Subtitle index 1 (0-based) should be active at 4000ms
        assertEquals(1, controller.state.value.currentSubtitleIndex)
    }

    @Test
    fun `seekTo pops new words when subtitle becomes active`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())

        // Initially no words popped
        assertTrue(controller.state.value.poppedNewWords.isEmpty())

        controller.seekTo(1000L)

        // First subtitle has newWord "hello"
        assertTrue(controller.state.value.poppedNewWords.contains("hello"))
    }

    @Test
    fun `seekTo accumulates new words across subtitles`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())

        controller.seekTo(1000L)  // Activates subtitle 1 -> pops "hello"
        controller.seekTo(4000L)  // Activates subtitle 2 -> pops "world"

        val popped = controller.state.value.poppedNewWords
        assertTrue(popped.contains("hello"))
        assertTrue(popped.contains("world"))
        assertEquals(2, popped.size)
    }

    @Test
    fun `seekTo clamps position to valid range`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())

        controller.seekTo(-1000L)
        assertEquals(0L, controller.state.value.positionMs)

        controller.seekTo(999_999L)
        assertEquals(10000L, controller.state.value.positionMs)
    }

    @Test
    fun `play starts playback`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())

        controller.play()
        assertTrue(controller.state.value.isPlaying)
    }

    @Test
    fun `pause stops playback`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())

        controller.play()
        assertTrue(controller.state.value.isPlaying)

        controller.pause()
        assertFalse(controller.state.value.isPlaying)
    }

    @Test
    fun `togglePlayPause switches between play and pause`() = runTest {
        val scope = TestScope(StandardTestDispatcher(testScheduler))
        val controller = AudioPlayerController(scope)
        controller.loadSubtitles(createTestSubtitles())

        assertFalse(controller.state.value.isPlaying)
        controller.togglePlayPause()
        assertTrue(controller.state.value.isPlaying)
        controller.togglePlayPause()
        assertFalse(controller.state.value.isPlaying)
    }

    @Test
    fun `setSpeed changes playback speed`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())

        controller.setSpeed(0.75f)
        assertEquals(0.75f, controller.state.value.speed, 0.01f)

        controller.setSpeed(1.0f)
        assertEquals(1.0f, controller.state.value.speed, 0.01f)
    }

    @Test
    fun `dismissNewWord removes word from popped list`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())

        controller.seekTo(1000L) // Pops "hello"
        assertTrue(controller.state.value.poppedNewWords.contains("hello"))

        controller.dismissNewWord("hello")
        assertFalse(controller.state.value.poppedNewWords.contains("hello"))
    }

    @Test
    fun `reset clears state and returns to initial`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())

        controller.seekTo(5000L)
        controller.play()
        controller.reset()

        assertEquals(0L, controller.state.value.positionMs)
        assertFalse(controller.state.value.isPlaying)
        assertFalse(controller.state.value.isFinished)
        assertEquals(-1, controller.state.value.currentSubtitleIndex)
    }

    @Test
    fun `reset clears isFinished flag`() = runTest {
        val controller = AudioPlayerController(TestScope(StandardTestDispatcher(testScheduler)))
        controller.loadSubtitles(createTestSubtitles())

        controller.seekTo(10000L)
        controller.reset()

        assertFalse(controller.state.value.isFinished)
    }
}
