package org.akj.lingo.learn.domain.usecase

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ProductionTaskScorerTest {

    private val scorer = ProductionTaskScorer()

    @Test
    fun `exact spelling gets full score`() {
        val result = scorer.scoreSpelling("apple", "apple")
        assertEquals(1f, result.score)
        assertTrue(result.isCorrect)
    }

    @Test
    fun `spelling ignores case`() {
        val result = scorer.scoreSpelling("Apple", "apple")
        assertTrue(result.isCorrect)
        assertEquals(1f, result.score)
    }

    @Test
    fun `close spelling gets partial credit`() {
        val result = scorer.scoreSpelling("appl", "apple")
        assertTrue(result.isCorrect) // similarity 0.8 >= 0.7
    }

    @Test
    fun `empty spelling fails with guidance`() {
        val result = scorer.scoreSpelling("", "apple")
        assertFalse(result.isCorrect)
        assertTrue(result.feedback.isNotBlank())
    }

    @Test
    fun `bad spelling fails`() {
        val result = scorer.scoreSpelling("xyz", "apple")
        assertFalse(result.isCorrect)
    }

    @Test
    fun `perfect dictation matches every word`() {
        val result = scorer.scoreDictation("i like apples", "I like apples")
        assertEquals(1f, result.score)
        assertTrue(result.isCorrect)
    }

    @Test
    fun `partial dictation gives proportional score`() {
        val result = scorer.scoreDictation("i like", "I like apples")
        assertEquals(2f / 3f, result.score)
    }

    @Test
    fun `empty dictation fails`() {
        val result = scorer.scoreDictation("", "I like apples")
        assertFalse(result.isCorrect)
    }

    @Test
    fun `sentence writing requires the target word`() {
        val result = scorer.scoreSentenceWriting("I went to school", "apple")
        assertFalse(result.isCorrect)
    }

    @Test
    fun `sentence writing with target word and length scores high`() {
        val result = scorer.scoreSentenceWriting("I eat an apple every morning", "apple")
        assertTrue(result.isCorrect)
        assertEquals(1f, result.score)
    }

    @Test
    fun `short sentence with target word still passes`() {
        val result = scorer.scoreSentenceWriting("I like apple", "apple")
        assertTrue(result.isCorrect)
    }

    @Test
    fun `blank sentence writing fails`() {
        val result = scorer.scoreSentenceWriting("   ", "apple")
        assertFalse(result.isCorrect)
    }
}
