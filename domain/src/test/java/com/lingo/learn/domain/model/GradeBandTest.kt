package org.akj.lingo.learn.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GradeBandTest {

    @Test
    fun `fromGrade returns PRIMARY for Grade 4`() {
        assertEquals(GradeBand.PRIMARY, GradeBand.fromGrade("Grade 4"))
    }

    @Test
    fun `fromGrade returns PRIMARY for Grade 5`() {
        assertEquals(GradeBand.PRIMARY, GradeBand.fromGrade("Grade 5"))
    }

    @Test
    fun `fromGrade returns PRIMARY for Grade 6`() {
        assertEquals(GradeBand.PRIMARY, GradeBand.fromGrade("Grade 6"))
    }

    @Test
    fun `fromGrade returns PRIMARY for primary Chinese keywords`() {
        assertEquals(GradeBand.PRIMARY, GradeBand.fromGrade("小学四年级"))
        assertEquals(GradeBand.PRIMARY, GradeBand.fromGrade("Primary"))
    }

    @Test
    fun `fromGrade returns JUNIOR for Grade 7`() {
        assertEquals(GradeBand.JUNIOR, GradeBand.fromGrade("Grade 7"))
    }

    @Test
    fun `fromGrade returns JUNIOR for Grade 8`() {
        assertEquals(GradeBand.JUNIOR, GradeBand.fromGrade("Grade 8"))
    }

    @Test
    fun `fromGrade returns JUNIOR for Grade 9`() {
        assertEquals(GradeBand.JUNIOR, GradeBand.fromGrade("Grade 9"))
    }

    @Test
    fun `fromGrade returns JUNIOR for junior Chinese keywords`() {
        assertEquals(GradeBand.JUNIOR, GradeBand.fromGrade("初一"))
        assertEquals(GradeBand.JUNIOR, GradeBand.fromGrade("初二"))
        assertEquals(GradeBand.JUNIOR, GradeBand.fromGrade("初三"))
        assertEquals(GradeBand.JUNIOR, GradeBand.fromGrade("Junior"))
    }

    @Test
    fun `fromGrade returns SENIOR for Grade 10`() {
        assertEquals(GradeBand.SENIOR, GradeBand.fromGrade("Grade 10"))
    }

    @Test
    fun `fromGrade returns SENIOR for Grade 11`() {
        assertEquals(GradeBand.SENIOR, GradeBand.fromGrade("Grade 11"))
    }

    @Test
    fun `fromGrade returns SENIOR for Grade 12`() {
        assertEquals(GradeBand.SENIOR, GradeBand.fromGrade("Grade 12"))
    }

    @Test
    fun `fromGrade returns SENIOR for senior Chinese keywords`() {
        assertEquals(GradeBand.SENIOR, GradeBand.fromGrade("高一"))
        assertEquals(GradeBand.SENIOR, GradeBand.fromGrade("高二"))
        assertEquals(GradeBand.SENIOR, GradeBand.fromGrade("高三"))
        assertEquals(GradeBand.SENIOR, GradeBand.fromGrade("Senior"))
    }

    @Test
    fun `fromGrade returns PRIMARY for unrecognized grades`() {
        assertEquals(GradeBand.PRIMARY, GradeBand.fromGrade(""))
        assertEquals(GradeBand.PRIMARY, GradeBand.fromGrade("Unknown"))
    }

    @Test
    fun `PRIMARY has correct default values`() {
        assertEquals(15, GradeBand.PRIMARY.defaultDurationMinutes)
        assertEquals(8, GradeBand.PRIMARY.maxWordsPerSentence)
        assertEquals(1.0f, GradeBand.PRIMARY.difficultyCoefficient)
    }

    @Test
    fun `JUNIOR has correct default values`() {
        assertEquals(20, GradeBand.JUNIOR.defaultDurationMinutes)
        assertEquals(14, GradeBand.JUNIOR.maxWordsPerSentence)
        assertEquals(1.3f, GradeBand.JUNIOR.difficultyCoefficient)
    }

    @Test
    fun `SENIOR has correct default values`() {
        assertEquals(25, GradeBand.SENIOR.defaultDurationMinutes)
        assertEquals(20, GradeBand.SENIOR.maxWordsPerSentence)
        assertEquals(1.6f, GradeBand.SENIOR.difficultyCoefficient)
    }

    @Test
    fun `getAdjustedDuration returns clamped values`() {
        // Minimum is 10
        assertEquals(10, GradeBand.getAdjustedDuration(5, 1.0f))
        // Maximum is 40
        assertEquals(40, GradeBand.getAdjustedDuration(50, 1.0f))
    }

    @Test
    fun `difficulty increases from PRIMARY to SENIOR`() {
        assertTrue(GradeBand.PRIMARY.difficultyCoefficient < GradeBand.JUNIOR.difficultyCoefficient)
        assertTrue(GradeBand.JUNIOR.difficultyCoefficient < GradeBand.SENIOR.difficultyCoefficient)
    }

    @Test
    fun `maxWordsPerSentence increases from PRIMARY to SENIOR`() {
        assertTrue(GradeBand.PRIMARY.maxWordsPerSentence < GradeBand.JUNIOR.maxWordsPerSentence)
        assertTrue(GradeBand.JUNIOR.maxWordsPerSentence < GradeBand.SENIOR.maxWordsPerSentence)
    }

    @Test
    fun `PRIMARY has higher phonics ratio than JUNIOR`() {
        assertTrue(GradeBand.PRIMARY.defaultPhonicsRatio > GradeBand.JUNIOR.defaultPhonicsRatio)
    }

    @Test
    fun `JUNIOR has higher grammar ratio than PRIMARY`() {
        assertTrue(GradeBand.JUNIOR.defaultGrammarRatio > GradeBand.PRIMARY.defaultGrammarRatio)
    }
}
