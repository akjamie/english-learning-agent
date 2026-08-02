package org.akj.lingo.learn.domain.usecase

import javax.inject.Inject

/**
 * Sprint 10.5 — Declares which product capabilities are usable given the
 * current AI provider configuration and network state.
 *
 * Lingo is an agent-based app: some features need the LLM / cloud TTS / cloud ASR,
 * while others (cached plans, session materials, offline ASR scoring, system TTS,
 * template fallbacks) keep working without any backend.
 *
 * This pure function lets the UI show a clear "offline mode" banner and lets
 * online-only entry points decide whether to enable / disable with a message,
 * instead of failing silently.
 */
class CapabilityMatrix @Inject constructor() {

    /** Result for one capability. */
    data class Capability(
        val key: String,
        val label: String,
        val available: Boolean,
        val offlineSafe: Boolean,
        val reason: String? = null
    )

    /**
     * Evaluates capability availability.
     *
     * @param authTokenConfigured  whether a provider auth token is set (>= threshold)
     * @param hasCachedPlan        whether a weekly plan exists locally (offline-safe content)
     * @param isNetworkAvailable   rough network availability hint (unused for offline-safe ones)
     */
    fun evaluate(
        authTokenConfigured: Boolean,
        hasCachedPlan: Boolean,
        isNetworkAvailable: Boolean
    ): List<Capability> {
        val llm = authTokenConfigured && isNetworkAvailable
        val cloudTts = authTokenConfigured && isNetworkAvailable
        val cloudAsr = authTokenConfigured && isNetworkAvailable

        return listOf(
            Capability(
                key = "PLAN_VIEW",
                label = "Weekly plan & materials",
                available = hasCachedPlan,
                offlineSafe = true,
                reason = if (hasCachedPlan) null else "Generate a plan once to view it offline"
            ),
            Capability(
                key = "DAILY_SESSION",
                label = "Daily learning session",
                available = true,
                offlineSafe = true
            ),
            Capability(
                key = "OFFLINE_ASR",
                label = "Pronunciation scoring (offline)",
                available = true,
                offlineSafe = true
            ),
            Capability(
                key = "SYSTEM_TTS",
                label = "Word / sentence audio (system TTS)",
                available = true,
                offlineSafe = true
            ),
            Capability(
                key = "LLM",
                label = "AI plan / explanation / encouragement",
                available = llm,
                offlineSafe = false,
                reason = if (!authTokenConfigured) "AI model not configured" else "No network connection"
            ),
            Capability(
                key = "CLOUD_TTS",
                label = "Natural voice audio (cloud TTS)",
                available = cloudTts,
                offlineSafe = false,
                reason = if (!authTokenConfigured) "TTS model not configured" else "No network connection"
            ),
            Capability(
                key = "CLOUD_ASR",
                label = "Accurate speech recognition (cloud ASR)",
                available = cloudAsr,
                offlineSafe = false,
                reason = if (!authTokenConfigured) "ASR model not configured" else "No network connection"
            )
        )
    }

    /** True when every online-only capability is unavailable (full offline mode). */
    fun isFullOfflineMode(
        authTokenConfigured: Boolean,
        isNetworkAvailable: Boolean
    ): Boolean = !(authTokenConfigured && isNetworkAvailable)
}
