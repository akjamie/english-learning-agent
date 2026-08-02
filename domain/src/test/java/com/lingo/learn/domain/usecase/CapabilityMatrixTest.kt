package org.akj.lingo.learn.domain.usecase

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for [CapabilityMatrix] — Sprint 10.5 offline/online capability awareness.
 */
class CapabilityMatrixTest {

    private val matrix = CapabilityMatrix()

    @Test
    fun `full offline mode when no token and no network`() {
        assertTrue(matrix.isFullOfflineMode(authTokenConfigured = false, isNetworkAvailable = false))
        assertTrue(matrix.isFullOfflineMode(authTokenConfigured = false, isNetworkAvailable = true))
    }

    @Test
    fun `not full offline when token and network present`() {
        assertFalse(matrix.isFullOfflineMode(authTokenConfigured = true, isNetworkAvailable = true))
    }

    @Test
    fun `offline-safe capabilities stay available without token`() {
        val caps = matrix.evaluate(authTokenConfigured = false, hasCachedPlan = true, isNetworkAvailable = true)

        assertTrue(caps.first { it.key == "DAILY_SESSION" }.available)
        assertTrue(caps.first { it.key == "OFFLINE_ASR" }.available)
        assertTrue(caps.first { it.key == "SYSTEM_TTS" }.available)
        assertTrue(caps.first { it.key == "PLAN_VIEW" }.available)
    }

    @Test
    fun `online-only capabilities require token and network`() {
        val caps = matrix.evaluate(authTokenConfigured = false, hasCachedPlan = true, isNetworkAvailable = true)

        assertFalse(caps.first { it.key == "LLM" }.available)
        assertFalse(caps.first { it.key == "CLOUD_TTS" }.available)
        assertFalse(caps.first { it.key == "CLOUD_ASR" }.available)
        assertNotNull(caps.first { it.key == "LLM" }.reason)
    }

    @Test
    fun `online capabilities available when token and network present`() {
        val caps = matrix.evaluate(authTokenConfigured = true, hasCachedPlan = true, isNetworkAvailable = true)

        assertTrue(caps.first { it.key == "LLM" }.available)
        assertTrue(caps.first { it.key == "CLOUD_TTS" }.available)
        assertTrue(caps.first { it.key == "CLOUD_ASR" }.available)
    }

    @Test
    fun `plan view unavailable when no cached plan`() {
        val caps = matrix.evaluate(authTokenConfigured = true, hasCachedPlan = false, isNetworkAvailable = true)
        val plan = caps.first { it.key == "PLAN_VIEW" }
        assertFalse(plan.available)
        assertNotNull(plan.reason)
    }
}
