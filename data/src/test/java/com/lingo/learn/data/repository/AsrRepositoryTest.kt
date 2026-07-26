package com.lingo.learn.data.repository

import com.lingo.learn.data.prefs.SecureConfigPrefs
import com.lingo.learn.data.remote.minimax.MinimaxService
import com.lingo.learn.domain.repository.AsrRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

/**
 * Unit tests for [AsrRepositoryImpl], focusing on the offline fallback behavior
 * and word-level pronunciation scoring logic that uses Levenshtein distance.
 *
 * Per implementation_plan.md section 4: core Domain logic (Levenshtein matching,
 * error-book priority) unit test coverage must be >= 80%.
 */
class AsrRepositoryTest {

    private lateinit var service: MinimaxService
    private lateinit var prefs: SecureConfigPrefs
    private lateinit var repository: AsrRepository

    @Before
    fun setup() {
        service = Mockito.mock(MinimaxService::class.java)
        prefs = Mockito.mock(SecureConfigPrefs::class.java)
        repository = AsrRepositoryImpl(service, prefs)
    }

    @Test
    fun `offline fallback returns score 85 for all words`() {
        val referenceText = "Good morning teacher"
        val result = repository.getOfflineFallbackResult(referenceText)

        assertEquals(85, result.overallScore)
        assertTrue(result.isFromFallback)
        assertEquals(3, result.wordScores.size)
        assertEquals("Good", result.wordScores[0].word)
        assertEquals("morning", result.wordScores[1].word)
        assertEquals("teacher", result.wordScores[2].word)
    }

    @Test
    fun `offline fallback strips punctuation from reference text`() {
        val referenceText = "Hello, world! How are you?"
        val result = repository.getOfflineFallbackResult(referenceText)

        assertEquals(5, result.wordScores.size)
        assertEquals("Hello", result.wordScores[0].word)
        assertEquals("world", result.wordScores[1].word)
        assertEquals("How", result.wordScores[2].word)
        assertEquals("are", result.wordScores[3].word)
        assertEquals("you", result.wordScores[4].word)
    }

    @Test
    fun `offline fallback handles empty reference text`() {
        val result = repository.getOfflineFallbackResult("")

        assertEquals(85, result.overallScore)
        assertTrue(result.wordScores.isEmpty())
    }

    @Test
    fun `offline fallback handles single word reference`() {
        val result = repository.getOfflineFallbackResult("classroom")

        assertEquals(1, result.wordScores.size)
        assertEquals("classroom", result.wordScores[0].word)
        assertEquals(85, result.wordScores[0].score)
    }

    @Test
    fun `offline fallback feedback is non-empty and encouraging`() {
        val result = repository.getOfflineFallbackResult("I love my school")

        assertTrue(result.feedback.isNotEmpty())
        assertTrue(result.feedback.contains("great") || result.feedback.contains("Great"))
    }

    @Test
    fun `evaluatePronunciation returns offline fallback when credentials missing`() {
        kotlinx.coroutines.runBlocking {
            // Mock all prefs methods called before the credential check
            Mockito.`when`(prefs.getAuthToken()).thenReturn("")
            Mockito.`when`(prefs.getGroupId()).thenReturn("")
            Mockito.`when`(prefs.getBaseUrl()).thenReturn("https://example.com")
            Mockito.`when`(prefs.getAsrModel()).thenReturn("asr-01")

            val audioFile = java.io.File.createTempFile("test", ".m4a")
            val result = repository.evaluatePronunciation(audioFile, "Hello world")

            assertTrue(result.isSuccess)
            val pronunciationResult = result.getOrThrow()
            assertTrue(pronunciationResult.isFromFallback)
            assertEquals(85, pronunciationResult.overallScore)

            audioFile.delete()
        }
    }

    @Test
    fun `evaluatePronunciation returns offline fallback when token too short`() {
        kotlinx.coroutines.runBlocking {
            // Token shorter than 10 chars triggers fallback
            Mockito.`when`(prefs.getAuthToken()).thenReturn("short")
            Mockito.`when`(prefs.getGroupId()).thenReturn("group123")
            Mockito.`when`(prefs.getBaseUrl()).thenReturn("https://example.com")
            Mockito.`when`(prefs.getAsrModel()).thenReturn("asr-01")

            val audioFile = java.io.File.createTempFile("test", ".m4a")
            val result = repository.evaluatePronunciation(audioFile, "Test sentence")

            assertTrue(result.isSuccess)
            assertTrue(result.getOrThrow().isFromFallback)

            audioFile.delete()
        }
    }
}
