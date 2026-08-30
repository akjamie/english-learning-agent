package org.akj.lingo.learn.domain.provider

/**
 * Sprint 9 — Provider endpoint adapter.
 *
 * A provider/agent-plan connection is fully described by a single base URL plus
 * an auth token plus model names. Per-channel request paths are derived here so
 * that a user only configures `{baseUrl, token, model names}` and nothing else.
 *
 * Supported channel paths (Ark plan gateway / OpenAI-compatible):
 *  - LLM  -> {baseUrl}/chat/completions
 *  - TTS  -> {baseUrl}/audio/tts
 *  - ASR  -> {baseUrl}/audio/transcriptions
 */
object ProviderEndpoints {

    const val DEFAULT_BASE_URL = "https://ark.cn-beijing.volces.com/api/plan/v3"

    /** Legacy default that omitted the `/v3` gateway segment. */
    const val LEGACY_BASE_URL = "https://ark.cn-beijing.volces.com/api/plan"

    const val LLM_PATH = "/chat/completions"
    const val TTS_PATH = "/audio/tts"
    const val ASR_PATH = "/audio/transcriptions"

    /**
     * Normalises a raw base URL, migrating the legacy Ark plan URL that was
     * missing the `/v3` gateway segment. Blank input falls back to the default.
     *
     * Also repairs a doubled-URL bug where the base URL was accidentally
     * concatenated with itself (e.g. `…v3…v3`).
     */
    fun normalizeBaseUrl(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return DEFAULT_BASE_URL
        if (trimmed == LEGACY_BASE_URL || trimmed == "$LEGACY_BASE_URL/") {
            return DEFAULT_BASE_URL
        }
        val doubledDefault = "$DEFAULT_BASE_URL$DEFAULT_BASE_URL"
        val doubledLegacy = "$LEGACY_BASE_URL$LEGACY_BASE_URL"
        if (trimmed == doubledDefault) return DEFAULT_BASE_URL
        if (trimmed == doubledLegacy) return DEFAULT_BASE_URL
        return trimmed.trimEnd('/')
    }

    /** Returns the full LLM chat completions URL for the given base URL. */
    fun chatUrl(baseUrl: String?): String = normalizeBaseUrl(baseUrl) + LLM_PATH

    /** Returns the full TTS synthesis URL for the given base URL. */
    fun ttsUrl(baseUrl: String?): String = normalizeBaseUrl(baseUrl) + TTS_PATH

    /** Returns the full ASR transcription URL for the given base URL. */
    fun asrUrl(baseUrl: String?): String = normalizeBaseUrl(baseUrl) + ASR_PATH
}
