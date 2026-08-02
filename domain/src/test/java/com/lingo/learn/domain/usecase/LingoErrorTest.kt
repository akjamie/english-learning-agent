package org.akj.lingo.learn.domain.usecase

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for the Sprint 10.5 unified error taxonomy ([classifyError] / [userFacingError]).
 */
class LingoErrorTest {

    @Test
    fun `auth errors map to AUTH`() {
        assertEquals(LingoErrorType.AUTH, classifyError(Exception("HTTP 401 Unauthorized")))
        assertEquals(LingoErrorType.AUTH, classifyError(Exception("Invalid API key")))
        assertEquals(LingoErrorType.AUTH, classifyError(Exception("403 Forbidden")))
    }

    @Test
    fun `network errors map to NETWORK`() {
        assertEquals(LingoErrorType.NETWORK, classifyError(Exception("Unable to resolve host")))
        assertEquals(LingoErrorType.NETWORK, classifyError(Exception("Connection refused")))
        assertEquals(LingoErrorType.NETWORK, classifyError(Exception("Socket timeout")))
        assertEquals(LingoErrorType.NETWORK, classifyError(Exception("HTTP 404 Not Found")))
    }

    @Test
    fun `config errors map to CONFIG`() {
        assertEquals(LingoErrorType.CONFIG, classifyError(Exception("Auth Token is not configured")))
        assertEquals(LingoErrorType.CONFIG, classifyError(Exception("ASR model not configured")))
    }

    @Test
    fun `timeout and parse errors map correctly`() {
        assertEquals(LingoErrorType.TIMEOUT, classifyError(Exception("request timed out")))
        assertEquals(LingoErrorType.PARSE, classifyError(Exception("JSON parse error")))
    }

    @Test
    fun `null and unknown map to UNKNOWN`() {
        assertEquals(LingoErrorType.UNKNOWN, classifyError(null))
        assertEquals(LingoErrorType.UNKNOWN, classifyError(Exception("random failure")))
    }

    @Test
    fun `user facing messages are non-empty and localized per type`() {
        assertTrue(userFacingError(LingoErrorType.NETWORK).contains("Network"))
        assertTrue(userFacingError(LingoErrorType.CONFIG).contains("Settings"))
        assertTrue(userFacingError(LingoErrorType.AUTH).contains("API key"))
        assertTrue(userFacingError(LingoErrorType.TIMEOUT).contains("timed out"))
        assertTrue(userFacingError(LingoErrorType.UNKNOWN).isNotBlank())
    }
}
