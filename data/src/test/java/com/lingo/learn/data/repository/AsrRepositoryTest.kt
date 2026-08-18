package org.akj.lingo.learn.data.repository

import org.akj.lingo.learn.data.prefs.SecureConfigPrefs
import org.akj.lingo.learn.data.remote.minimax.MinimaxAsrResponse
import org.akj.lingo.learn.data.remote.minimax.MinimaxService
import org.akj.lingo.learn.data.remote.minimax.PlanAsrClient
import org.akj.lingo.learn.domain.repository.AsrRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import okhttp3.MediaType.Companion.toMediaTypeOrNull

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
    private lateinit var planAsrClient: PlanAsrClient
    private lateinit var repository: AsrRepository

    @BeforeEach
    fun setup() {
        service = mock<MinimaxService>()
        prefs = Mockito.mock(SecureConfigPrefs::class.java)
        planAsrClient = mock<PlanAsrClient>()
        repository = AsrRepositoryImpl(service, prefs, planAsrClient)
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

    @Test
    fun `evaluatePronunciation does NOT fabricate a fallback score when ASR call fails with credentials configured`() {
        kotlinx.coroutines.runBlocking {
            // Credentials are present, but the cloud ASR call fails (non-2xx).
            Mockito.`when`(prefs.getAuthToken()).thenReturn("valid-token-0123456789")
            Mockito.`when`(prefs.getGroupId()).thenReturn("group123")
            Mockito.`when`(prefs.getBaseUrl()).thenReturn("https://example.com")
            Mockito.`when`(prefs.getAsrModel()).thenReturn("volc.seedasr.sauc.duration")

            val failed = retrofit2.Response.error<MinimaxAsrResponse>(
                500,
                okhttp3.ResponseBody.create(
                    "application/json".toMediaTypeOrNull()!!,
                    "{}"
                )
            )
            whenever(service.audioToText(any(), any(), any(), anyOrNull(), any(), any())).thenReturn(failed)

            val audioFile = java.io.File.createTempFile("test", ".m4a")
            val result = repository.evaluatePronunciation(audioFile, "Hello world")

            // The call must surface the failure, NOT return a fake success score.
            assertTrue(result.isFailure)

            audioFile.delete()
        }
    }

    @Test
    fun `evaluatePronunciation scores real ASR transcription with Levenshtein similarity`() {
        kotlinx.coroutines.runBlocking {
            Mockito.`when`(prefs.getAuthToken()).thenReturn("valid-token-0123456789")
            Mockito.`when`(prefs.getGroupId()).thenReturn("group123")
            Mockito.`when`(prefs.getBaseUrl()).thenReturn("https://example.com")
            Mockito.`when`(prefs.getAsrModel()).thenReturn("volc.seedasr.sauc.duration")
            Mockito.`when`(prefs.getAsrResourceId()).thenReturn("volc.seedasr.sauc.duration")

            val ok = retrofit2.Response.success(MinimaxAsrResponse(text = "hello world", detailedInfo = null, baseResp = null))
            whenever(service.audioToText(any(), any(), any(), anyOrNull(), any(), any())).thenReturn(ok)

            val audioFile = java.io.File.createTempFile("test", ".m4a")
            val result = repository.evaluatePronunciation(audioFile, "Hello world")

            verify(service).audioToText(any(), any(), any(), anyOrNull(), any(), any())
            if (result.isFailure) {
                println("SUCCESS-PATH FAILURE: ${result.exceptionOrNull()}")
            }
            assertTrue(result.isSuccess)
            val pronunciationResult = result.getOrThrow()
            assertFalse(pronunciationResult.isFromFallback)
            assertEquals(100, pronunciationResult.overallScore)

            audioFile.delete()
        }
    }
}
