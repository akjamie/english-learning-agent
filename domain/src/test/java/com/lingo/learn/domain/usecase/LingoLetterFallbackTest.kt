package org.akj.lingo.learn.domain.usecase

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Sprint 12 — tests for the weekly "Lingo's letter" parent digest offline fallback.
 */
class LingoLetterFallbackTest {

    @Test
    fun `high accuracy uses encouraging template`() {
        val text = LingoLetterFallback.digest(weeklyAccuracy = 0.9f, sessions = 5, topErrorWords = listOf("apple"))
        assertTrue(text.contains("5"))
        assertTrue(text.contains("great"))
    }

    @Test
    fun `mid accuracy references error words`() {
        val text = LingoLetterFallback.digest(weeklyAccuracy = 0.6f, sessions = 3, topErrorWords = listOf("apple", "school"))
        assertTrue(text.contains("apple"))
        assertTrue(text.contains("school"))
        assertTrue(text.contains("3"))
    }

    @Test
    fun `low accuracy uses gentle growth language`() {
        val text = LingoLetterFallback.digest(weeklyAccuracy = 0.2f, sessions = 1, topErrorWords = emptyList())
        assertTrue(text.contains("Every practice counts"))
        assertTrue(text.contains("1"))
    }

    @Test
    fun `boundary at 0_8 uses high tier`() {
        assertTrue(LingoLetterFallback.digest(0.8f, 2, emptyList()).contains("great"))
        assertTrue(LingoLetterFallback.digest(0.79f, 2, listOf("word")).contains("steady progress"))
    }
}
