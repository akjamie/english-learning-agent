package org.akj.lingo.learn.domain.usecase

/**
 * Sprint 10.5 — Unified user-facing error taxonomy.
 *
 * Every repository/use-case failure is mapped to one of these categories so the
 * UI can render a consistent, actionable message (inline ErrorBanner for page
 * level, Snackbar/Toast for transient actions) instead of silently swallowing
 * exceptions or showing raw stack strings.
 */
enum class LingoErrorType {
    NETWORK,
    CONFIG,
    AUTH,
    RATE_LIMIT,
    TIMEOUT,
    PARSE,
    UNKNOWN
}

/** Maps a Throwable to a [LingoErrorType] using stable heuristics. */
fun classifyError(t: Throwable?): LingoErrorType {
    if (t == null) return LingoErrorType.UNKNOWN
    val msg = (t.message ?: t.javaClass.simpleName).lowercase()
    return when {
        // Rate limiting (HTTP 429) must be checked BEFORE generic keywords like
        // "model" because error messages like "Primary model request failed: 429"
        // contain "model" but are NOT configuration errors.
        msg.contains("429") || msg.contains("rate limit") || msg.contains("too many requests") ->
            LingoErrorType.RATE_LIMIT
        // Auth errors (HTTP 401/403) before network to avoid "auth" matching network messages.
        msg.contains("401") || msg.contains("unauthorized") || msg.contains("403") || msg.contains("forbidden") ||
            msg.contains("invalid api key") -> LingoErrorType.AUTH
        // Config-missing is specific: only actual configuration words, NOT the
        // word "model" which appears in unrelated HTTP error messages.
        msg.contains("not configured") || msg.contains("missing") || msg.contains("configured") ->
            LingoErrorType.CONFIG
        msg.contains("unable to resolve") || msg.contains("network") || msg.contains("connect") ||
            msg.contains("socket") || msg.contains("eof") || msg.contains("404") -> LingoErrorType.NETWORK
        msg.contains("timeout") || msg.contains("timed out") -> LingoErrorType.TIMEOUT
        msg.contains("json") || msg.contains("parse") || msg.contains("format") -> LingoErrorType.PARSE
        else -> LingoErrorType.UNKNOWN
    }
}

/** Returns a short, user-friendly message for an error type (fallback provided). */
fun userFacingError(type: LingoErrorType): String = when (type) {
    LingoErrorType.NETWORK -> "Network issue — check your connection and try again."
    LingoErrorType.CONFIG -> "AI model is not configured. Add it in Settings first."
    LingoErrorType.AUTH -> "Authentication failed — please check your API key in Settings."
    LingoErrorType.RATE_LIMIT -> "Too many requests — please wait a moment and try again."
    LingoErrorType.TIMEOUT -> "The request timed out. Please try again."
    LingoErrorType.PARSE -> "Unexpected response format. Please try again."
    LingoErrorType.UNKNOWN -> "Something went wrong. Please try again."
}
