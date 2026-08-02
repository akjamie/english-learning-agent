package org.akj.lingo.learn.domain.provider

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ProviderEndpoints] — Sprint 9 provider endpoint adapter.
 *
 * Tests verify:
 *  - Per-channel paths are derived from a single base URL
 *  - The legacy `.../api/plan` default (missing `/v3`) is migrated
 *  - Blank input falls back to the /v3 default
 *  - Trailing slashes are stripped
 */
class ProviderEndpointsTest {

    @Test
    fun `chat url is base plus chat completions path`() {
        assertEquals(
            "https://ark.cn-beijing.volces.com/api/plan/v3/chat/completions",
            ProviderEndpoints.chatUrl("https://ark.cn-beijing.volces.com/api/plan/v3")
        )
    }

    @Test
    fun `tts url is base plus audio tts path`() {
        assertEquals(
            "https://ark.cn-beijing.volces.com/api/plan/v3/audio/tts",
            ProviderEndpoints.ttsUrl("https://ark.cn-beijing.volces.com/api/plan/v3")
        )
    }

    @Test
    fun `asr url is base plus audio transcriptions path`() {
        assertEquals(
            "https://ark.cn-beijing.volces.com/api/plan/v3/audio/transcriptions",
            ProviderEndpoints.asrUrl("https://ark.cn-beijing.volces.com/api/plan/v3")
        )
    }

    @Test
    fun `legacy base url missing v3 is migrated on read`() {
        val migrated = ProviderEndpoints.normalizeBaseUrl("https://ark.cn-beijing.volces.com/api/plan")
        assertEquals(ProviderEndpoints.DEFAULT_BASE_URL, migrated)
    }

    @Test
    fun `legacy base url with trailing slash is migrated`() {
        assertEquals(
            ProviderEndpoints.DEFAULT_BASE_URL,
            ProviderEndpoints.normalizeBaseUrl("https://ark.cn-beijing.volces.com/api/plan/")
        )
    }

    @Test
    fun `blank input falls back to default v3 url`() {
        assertEquals(ProviderEndpoints.DEFAULT_BASE_URL, ProviderEndpoints.normalizeBaseUrl(null))
        assertEquals(ProviderEndpoints.DEFAULT_BASE_URL, ProviderEndpoints.normalizeBaseUrl("  "))
    }

    @Test
    fun `custom base url with trailing slash is normalised without path changes`() {
        assertEquals(
            "https://custom.example.com/api/v1",
            ProviderEndpoints.normalizeBaseUrl("https://custom.example.com/api/v1/")
        )
        assertEquals(
            "https://custom.example.com/api/v1/chat/completions",
            ProviderEndpoints.chatUrl("https://custom.example.com/api/v1/")
        )
    }
}
