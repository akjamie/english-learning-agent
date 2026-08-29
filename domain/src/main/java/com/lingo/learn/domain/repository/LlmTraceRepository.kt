package org.akj.lingo.learn.domain.repository

import org.akj.lingo.learn.domain.model.LlmTrace

/**
 * Read access to per-call LLM traces for the Settings debug panel (Sprint 21).
 * Traces are written by [LlmRepository] during each model attempt; this exposes
 * the most recent ones for troubleshooting prompt/model issues.
 */
interface LlmTraceRepository {
    /** Returns the most recent [limit] LLM call traces, newest first. */
    suspend fun getRecentTraces(limit: Int): List<LlmTrace>
}
