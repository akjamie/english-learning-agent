package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.model.GradeBand
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AdaptiveDifficultyEngineTest {

    private val engine = AdaptiveDifficultyEngine()

    @Test
    fun `low accuracy shortens sentences`() {
        assertEquals(-3, engine.sentenceLengthAdjustment(0.5f))
        assertEquals(-1, engine.sentenceLengthAdjustment(0.6f))
    }

    @Test
    fun `mid accuracy holds sentence length`() {
        assertEquals(0, engine.sentenceLengthAdjustment(0.75f))
        assertEquals(0, engine.sentenceLengthAdjustment(0.85f))
    }

    @Test
    fun `high accuracy lengthens sentences`() {
        assertEquals(2, engine.sentenceLengthAdjustment(0.9f))
        assertEquals(3, engine.sentenceLengthAdjustment(0.96f))
    }

    @Test
    fun `adjusted max words is clamped within bounds`() {
        val low = engine.adjustedMaxWordsPerSentence(GradeBand.PRIMARY, 0.4f)
        assertTrue(low in 4..22)
        assertTrue(low <= GradeBand.PRIMARY.maxWordsPerSentence)

        val high = engine.adjustedMaxWordsPerSentence(GradeBand.SENIOR, 0.99f)
        assertTrue(high >= GradeBand.SENIOR.maxWordsPerSentence)
        assertTrue(high <= 22)
    }

    @Test
    fun `cefr level drops on low accuracy`() {
        assertTrue(engine.cefrLevel(GradeBand.PRIMARY, 0.5f).contains("A1"))
        assertFalse(engine.cefrLevel(GradeBand.PRIMARY, 0.5f).contains("A1-A2"))
    }

    @Test
    fun `cefr level rises on high accuracy`() {
        assertTrue(engine.cefrLevel(GradeBand.PRIMARY, 0.98f).contains("A2"))
    }

    @Test
    fun `describe adjustment is human readable`() {
        val desc = engine.describeAdjustment(GradeBand.PRIMARY, 0.62f)
        assertTrue(desc.contains("62%"))
        assertTrue(desc.contains("sentence length"))
    }
}
