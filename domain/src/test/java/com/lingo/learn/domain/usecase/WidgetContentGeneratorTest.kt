package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.model.WidgetInput
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for [WidgetContentGenerator] (Sprint 14 - Enhancement 5).
 * Verifies personalized widget text generation from learning data, including
 * name interpolation, streak-based variants, and context-aware subtitles.
 */
class WidgetContentGeneratorTest {

    @Test
    fun `streak of at least 30 gives fire emoji and personalized title`() {
        val content = WidgetContentGenerator.generate(
            WidgetInput(streakDays = 35, todayDone = false, childName = "Emma")
        )
        assertEquals("🔥", content.emoji)
        assertTrue(content.title.contains("Emma"))
        assertTrue(content.title.contains("35"))
    }

    @Test
    fun `streak of at least 7 gives trophy emoji and personalized title`() {
        val content = WidgetContentGenerator.generate(
            WidgetInput(streakDays = 10, todayDone = false, childName = "Tom")
        )
        assertEquals("🏆", content.emoji)
        assertTrue(content.title.contains("Tom"))
        assertTrue(content.title.contains("10"))
    }

    @Test
    fun `streak of at least 1 with todayDone gives done message`() {
        val content = WidgetContentGenerator.generate(
            WidgetInput(streakDays = 3, todayDone = true, childName = "Lily")
        )
        assertEquals("🦊", content.emoji)
        assertTrue(content.title.contains("Lily"))
        assertTrue(content.title.contains("Done"))
    }

    @Test
    fun `streak of at least 1 without todayDone gives day streak message`() {
        val content = WidgetContentGenerator.generate(
            WidgetInput(streakDays = 5, todayDone = false, childName = "Jack")
        )
        assertEquals("🦊", content.emoji)
        assertTrue(content.title.contains("Jack"))
        assertTrue(content.title.contains("5"))
    }

    @Test
    fun `streak 0 produces welcome message with task theme`() {
        val content = WidgetContentGenerator.generate(
            WidgetInput(streakDays = 0, todayDone = false, childName = "Mia", todayTaskTheme = "School Life")
        )
        assertEquals("🦊", content.emoji)
        assertTrue(content.title.contains("Mia"))
        assertTrue(content.subtitle.contains("School Life"))
    }

    @Test
    fun `streak 0 without task theme produces generic welcome`() {
        val content = WidgetContentGenerator.generate(
            WidgetInput(streakDays = 0, todayDone = false, childName = "Mia")
        )
        assertTrue(content.subtitle.contains("Ready to start"))
    }

    @Test
    fun `subtitle includes task theme when available`() {
        val content = WidgetContentGenerator.generate(
            WidgetInput(streakDays = 7, todayDone = false, childName = "Sam", todayTaskTheme = "Shopping")
        )
        assertTrue(content.subtitle.contains("Shopping"))
    }

    @Test
    fun `subtitle includes error count when errors exist`() {
        val content = WidgetContentGenerator.generate(
            WidgetInput(streakDays = 7, todayDone = false, childName = "Sam", errorCount = 5)
        )
        assertTrue(content.subtitle.contains("5"))
        assertTrue(content.subtitle.contains("review"))
    }

    @Test
    fun `subtitle includes both theme and error count`() {
        val content = WidgetContentGenerator.generate(
            WidgetInput(streakDays = 7, todayDone = false, childName = "Sam", todayTaskTheme = "Animals", errorCount = 3)
        )
        assertTrue(content.subtitle.contains("Animals"))
        assertTrue(content.subtitle.contains("3"))
    }

    @Test
    fun `blank child name falls back to friend`() {
        val content = WidgetContentGenerator.generate(
            WidgetInput(streakDays = 5, todayDone = false, childName = "")
        )
        assertTrue(content.title.contains("friend"))
    }

    @Test
    fun `todayDone wins over a streak of at least 7`() {
        val content = WidgetContentGenerator.generate(
            WidgetInput(streakDays = 10, todayDone = true, childName = "Alex")
        )
        // todayDone with streak >= 1 shows "Done" message (not trophy)
        assertTrue(content.title.contains("Done"))
        assertNotEquals("🏆", content.emoji)
    }

    @Test
    fun `default content has fox emoji and practice subtitle`() {
        val default = org.akj.lingo.learn.domain.model.WidgetContent.DEFAULT
        assertEquals("🦊", default.emoji)
        assertTrue(default.subtitle.contains("Ready to practice"))
    }
}
