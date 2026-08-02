package org.akj.lingo.learn.domain.model

/**
 * Sprint 11 — A roleplay conversation scenario.
 *
 * Each scenario carries an offline-safe curated script (system prompt + opening
 * line + target vocabulary) so chat works without an LLM. An optional LLM pass
 * (taskType "ROLEPLAY_SCENARIO") can enrich/generate a richer script when a
 * model is configured, but the curated fallback is always available.
 */
data class RoleplayScenario(
    val id: String,                // "zoo", "restaurant", "school", "travel"
    val title: String,             // "Zoo Visit"
    val emoji: String,             // "🦁"
    val description: String,
    val targetWords: List<String>,
    val systemPrompt: String,      // offline-safe system prompt
    val openingLine: String        // Lingo's first line
)
