package com.lingo.learn.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Validates the structure and integrity of [SampleLearningContent], ensuring
 * the MVP placeholder data has correct indices, non-empty fields, and that
 * quiz questions include error-book recurrence items per design spec section 6.
 */
class SampleLearningContentTest {

    @Test
    fun `school life session has valid theme`() {
        val session = SampleLearningContent.createSchoolLifeSession()

        assertEquals("School Life", session.theme)
    }

    @Test
    fun `session has non-empty subtitle lines with valid timestamps`() {
        val session = SampleLearningContent.createSchoolLifeSession()

        assertTrue(session.subtitleLines.isNotEmpty())
        session.subtitleLines.forEach { line ->
            assertTrue(line.startTimeMs < line.endTimeMs)
            assertTrue(line.text.isNotEmpty())
        }
    }

    @Test
    fun `subtitle lines have sequential timestamps`() {
        val session = SampleLearningContent.createSchoolLifeSession()

        for (i in 1 until session.subtitleLines.size) {
            val prev = session.subtitleLines[i - 1]
            val curr = session.subtitleLines[i]
            assertTrue(
                "Line ${curr.id} should start after line ${prev.id} ends",
                curr.startTimeMs >= prev.endTimeMs
            )
        }
    }

    @Test
    fun `subtitle lines contain new words`() {
        val session = SampleLearningContent.createSchoolLifeSession()
        val allNewWords = session.subtitleLines.flatMap { it.newWords }

        assertTrue(allNewWords.isNotEmpty())
    }

    @Test
    fun `session has read-along sentences with Chinese hints`() {
        val session = SampleLearningContent.createSchoolLifeSession()

        assertTrue(session.readAlongSentences.isNotEmpty())
        session.readAlongSentences.forEach { sentence ->
            assertTrue(sentence.text.isNotEmpty())
            assertTrue(sentence.chineseHint.isNotEmpty())
        }
    }

    @Test
    fun `game questions have valid correct indices`() {
        val session = SampleLearningContent.createSchoolLifeSession()

        assertTrue(session.gameQuestions.isNotEmpty())
        session.gameQuestions.forEach { question ->
            assertTrue(question.correctIndex >= 0)
            assertTrue(question.correctIndex < question.options.size)
            assertTrue(question.options.size >= 2)
        }
    }

    @Test
    fun `quiz has at least one listening question`() {
        val session = SampleLearningContent.createSchoolLifeSession()
        val listeningQuestions = session.quizQuestions.filter {
            it.type == QuizQuestionType.LISTEN_CHOOSE_WORD
        }

        assertTrue(
            "Quiz must have at least 1 listening question per design spec",
            listeningQuestions.isNotEmpty()
        )
    }

    @Test
    fun `quiz has at least one error-book recurrence question`() {
        val session = SampleLearningContent.createSchoolLifeSession()
        val errorBookQuestions = session.quizQuestions.filter { it.isFromErrorBook }

        assertTrue(
            "Quiz must include 1-2 error-book recurrence questions per design spec",
            errorBookQuestions.isNotEmpty()
        )
    }

    @Test
    fun `quiz has at most 5 questions`() {
        val session = SampleLearningContent.createSchoolLifeSession()

        assertTrue(
            "Daily micro-quiz should have at most 5 questions per design spec",
            session.quizQuestions.size <= 5
        )
    }

    @Test
    fun `session target new words are non-empty`() {
        val session = SampleLearningContent.createSchoolLifeSession()

        assertTrue(session.targetNewWords.isNotEmpty())
        assertTrue(session.targetNewWords.size >= 5)
    }

    @Test
    fun `sentence order quiz has correct order matching options`() {
        val session = SampleLearningContent.createSchoolLifeSession()
        val orderQuestion = session.quizQuestions.find {
            it.type == QuizQuestionType.SENTENCE_ORDER
        }

        if (orderQuestion != null) {
            assertEquals(
                "Options and correctOrder should have the same size",
                orderQuestion.options.size,
                orderQuestion.correctOrder.size
            )
            assertTrue(orderQuestion.correctOrder.isNotEmpty())
        }
    }
}
