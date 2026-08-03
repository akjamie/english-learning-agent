package org.akj.lingo.learn.domain.usecase

/**
 * Sprint 12 — offline fallback for the weekly "Lingo's letter" parent digest.
 * Pure function so it can be unit-tested independent of the LLM.
 */
object LingoLetterFallback {

    /**
     * Returns a data-driven digest template when the LLM is unavailable.
     */
    fun digest(weeklyAccuracy: Float, sessions: Int, topErrorWords: List<String>): String {
        return when {
            weeklyAccuracy >= 0.8f ->
                "This week your child practiced $sessions times and is doing great! Keep it up!"
            weeklyAccuracy >= 0.5f -> {
                val focus = topErrorWords.take(2).joinToString().ifBlank { "new words" }
                "Your child completed $sessions sessions this week — steady progress! Let's keep building those $focus."
            }
            else ->
                "Every practice counts. This week brought $sessions sessions — small steps lead to big growth!"
        }
    }
}
