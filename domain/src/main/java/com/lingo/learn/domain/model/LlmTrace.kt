package org.akj.lingo.learn.domain.model

/**
 * Per-call LLM trace (Sprint 21 Phase C) — one row per model attempt (primary
 * and fallback), captured inside [org.akj.lingo.learn.domain.repository.LlmRepository].
 *
 * Unlike the aggregated [TokenUsageLog], this keeps the outcome, latency and a
 * result fingerprint so prompt issues can be debugged from a Settings panel:
 * which model ran, whether the fallback fired, how long it took, and a hash of
 * what came back.
 */
data class LlmTrace(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val taskType: String,
    val model: String,          // "primary" / "fallback"
    val success: Boolean,
    val durationMs: Long,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val totalTokens: Int = 0,
    val detail: String = ""     // result-content hash on success, error message on failure
)
