package org.akj.lingo.learn.domain.model

import org.akj.lingo.learn.domain.usecase.SessionBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SessionBuilderTest {

    private lateinit var builder: SessionBuilder

    private val validPlanJson = """
        {
            "theme": "School Life",
            "difficulty_coefficient": 1.0,
            "days": [
                {
                    "day": 1,
                    "focus": "Vocabulary & Dialogue",
                    "target_words": ["classroom", "teacher"],
                    "reference_sentence": "Welcome to our classroom!",
                    "duration_minutes": 15
                },
                {
                    "day": 2,
                    "focus": "Grammar & Practice",
                    "target_words": ["student", "book"],
                    "reference_sentence": "The student reads a book.",
                    "duration_minutes": 15
                }
            ]
        }
    """.trimIndent()

    private val emptyDaysJson = """
        {
            "theme": "Daily Life",
            "difficulty_coefficient": 1.0,
            "days": []
        }
    """.trimIndent()

    private val invalidJson = "not a valid json"

    @BeforeEach
    fun setup() {
        builder = SessionBuilder()
    }

    @Test
    fun `expandPlanToSession returns session for valid JSON and PRIMARY grade`() {
        val session = builder.expandPlanToSession(validPlanJson, GradeBand.PRIMARY, dayIndex = 1)

        assertNotNull(session)
        session?.let {
            assertTrue(it.theme.startsWith("Day 1"))
            assertTrue(it.subtitleLines.isNotEmpty())
            assertTrue(it.readAlongSentences.isNotEmpty())
            assertTrue(it.gameQuestions.isNotEmpty())
            assertTrue(it.quizQuestions.isNotEmpty())
            assertTrue(it.targetNewWords.isNotEmpty())
        }
    }

    @Test
    fun `expandPlanToSession returns session for valid JSON and JUNIOR grade`() {
        val session = builder.expandPlanToSession(validPlanJson, GradeBand.JUNIOR, dayIndex = 1)

        assertNotNull(session)
        session?.let {
            assertTrue(it.subtitleLines.isNotEmpty())
            assertTrue(it.quizQuestions.isNotEmpty())
        }
    }

    @Test
    fun `expandPlanToSession returns session for valid JSON and SENIOR grade`() {
        val session = builder.expandPlanToSession(validPlanJson, GradeBand.SENIOR, dayIndex = 1)

        assertNotNull(session)
        session?.let {
            assertTrue(it.subtitleLines.isNotEmpty())
            assertTrue(it.quizQuestions.isNotEmpty())
        }
    }

    @Test
    fun `expandPlanToSession returns null for invalid JSON`() {
        val session = builder.expandPlanToSession(invalidJson, GradeBand.PRIMARY, dayIndex = 1)

        assertNull(session)
    }

    @Test
    fun `expandPlanToSession returns null for empty days array`() {
        val session = builder.expandPlanToSession(emptyDaysJson, GradeBand.PRIMARY, dayIndex = 1)

        assertNull(session)
    }

    @Test
    fun `expandPlanToSession selects correct day from plan`() {
        val session = builder.expandPlanToSession(validPlanJson, GradeBand.PRIMARY, dayIndex = 2)

        assertNotNull(session)
        session?.let {
            assertTrue(it.theme.startsWith("Day 2"))
            assertTrue(it.targetNewWords.contains("student") || it.targetNewWords.contains("book"))
        }
    }

    @Test
    fun `expandPlanToSession dayIndex is clamped to valid range`() {
        // Day 0 should be clamped to Day 1
        val session0 = builder.expandPlanToSession(validPlanJson, GradeBand.PRIMARY, dayIndex = 0)
        assertNotNull(session0)

        // Day 3 should be clamped to Day 2 (max days in plan)
        val session3 = builder.expandPlanToSession(validPlanJson, GradeBand.PRIMARY, dayIndex = 3)
        assertNotNull(session3)
    }

    @Test
    fun `expandPlanToSession generates subtitles from reference sentence`() {
        val session = builder.expandPlanToSession(validPlanJson, GradeBand.PRIMARY, dayIndex = 1)

        session?.let {
            val allText = it.subtitleLines.joinToString(" ") { line -> line.text }
            assertTrue(allText.contains("Welcome") || allText.contains("classroom"))
        }
    }

    @Test
    fun `expandPlanToSession generates read-along sentences`() {
        val session = builder.expandPlanToSession(validPlanJson, GradeBand.PRIMARY, dayIndex = 1)

        session?.let {
            assertTrue(it.readAlongSentences.all { sentence -> sentence.text.isNotBlank() })
            assertTrue(it.readAlongSentences.size <= 3)
        }
    }

    @Test
    fun `expandPlanToSession generates quiz with bounded question count`() {
        val session = builder.expandPlanToSession(validPlanJson, GradeBand.PRIMARY, dayIndex = 1)

        session?.let {
            assertTrue(it.quizQuestions.isNotEmpty())
            // Sprint 7 allows up to 10 mix questions plus prepended error-book review questions.
            assertTrue(it.quizQuestions.size <= 15)
        }
    }

    @Test
    fun `expandPlanToSession includes error book recurrence questions when provided`() {
        val reviewQuestions = listOf(
            QuizQuestion(
                id = 900,
                type = QuizQuestionType.LISTEN_CHOOSE_WORD,
                question = "Review question",
                audioText = "test",
                options = listOf("test", "wrong1", "wrong2", "wrong3"),
                correctIndex = 0,
                isFromErrorBook = true
            )
        )

        val session = builder.expandPlanToSession(
            validPlanJson, GradeBand.PRIMARY, dayIndex = 1,
            reviewQuestions = reviewQuestions
        )

        session?.let {
            val hasReview = it.quizQuestions.any { q -> q.isFromErrorBook }
            assertTrue(hasReview)
        }
    }

    @Test
    fun `expandPlanToSession with empty review list still generates quiz`() {
        val session = builder.expandPlanToSession(
            validPlanJson, GradeBand.PRIMARY, dayIndex = 1,
            reviewQuestions = emptyList()
        )

        session?.let {
            assertTrue(it.quizQuestions.isNotEmpty())
        }
    }

    @Test
    fun `expandPlanToSession generates at least one game question`() {
        val session = builder.expandPlanToSession(validPlanJson, GradeBand.PRIMARY, dayIndex = 1)

        session?.let {
            assertTrue(it.gameQuestions.isNotEmpty())
            it.gameQuestions.forEach { game ->
                assertTrue(game.correctIndex >= 0)
                assertTrue(game.correctIndex < game.options.size)
            }
        }
    }

    @Test
    fun `expandPlanToSession subtitle timestamps are sequential`() {
        val session = builder.expandPlanToSession(validPlanJson, GradeBand.PRIMARY, dayIndex = 1)

        session?.let {
            for (i in 1 until it.subtitleLines.size) {
                val prev = it.subtitleLines[i - 1]
                val curr = it.subtitleLines[i]
                assertTrue(curr.startTimeMs >= prev.endTimeMs,
                    "Line ${curr.id} should start after line ${prev.id} ends")
            }
        }
    }

    @Test
    fun `expandPlanToSession handles empty reference sentence gracefully`() {
        val jsonWithEmptySentence = """
            {
                "theme": "Test",
                "difficulty_coefficient": 1.0,
                "days": [{
                    "day": 1,
                    "focus": "Test",
                    "target_words": ["hello", "world"],
                    "reference_sentence": "",
                    "duration_minutes": 15
                }]
            }
        """.trimIndent()

        val session = builder.expandPlanToSession(jsonWithEmptySentence, GradeBand.PRIMARY, dayIndex = 1)

        assertNotNull(session)
        session?.let {
            assertTrue(it.subtitleLines.isNotEmpty())
        }
    }

    @Test
    fun `spell fill blank uses the blanked letter as the correct option`() {
        val session = builder.expandPlanToSession(validPlanJson, GradeBand.PRIMARY, dayIndex = 1)

        session?.let {
            val blanks = it.quizQuestions.filter { q -> q.type == QuizQuestionType.SPELL_FILL_BLANK }
            assertTrue(blanks.isNotEmpty(), "Expected at least one SPELL_FILL_BLANK question")
            blanks.forEach { q ->
                // The blanked display is "s_chool" style: first letter + '_' + rest.
                val display = q.question.removePrefix("Complete the word: ")
                val blankIndex = display.indexOf('_')
                assertTrue(blankIndex > 0, "Fill-blank must blank a letter after the first: $display")
                // correctIndex must point to a valid option
                assertTrue(q.correctIndex in q.options.indices, "correctIndex out of range: ${q.correctIndex}")
                assertTrue(q.options.isNotEmpty())
                // The option that fills the blank must not be the word's first letter
                val correctOption = q.options[q.correctIndex]
                assertNotEquals(display.first().toString(), correctOption,
                    "Correct option must be the blanked letter, not the first letter")
            }
        }
    }

    @Test
    fun `spell fill blank correct option matches blanked letter`() {
        // Craft a plan whose only word guarantees a known blank.
        val json = """
            {
                "theme": "School",
                "difficulty_coefficient": 1.0,
                "days": [{
                    "day": 1,
                    "focus": "Vocab",
                    "target_words": ["school"],
                    "reference_sentence": "This is my school.",
                    "duration_minutes": 15
                }]
            }
        """.trimIndent()

        val session = builder.expandPlanToSession(json, GradeBand.PRIMARY, dayIndex = 1)

        session?.let {
            val blank = it.quizQuestions.first { q -> q.type == QuizQuestionType.SPELL_FILL_BLANK }
            val display = blank.question.removePrefix("Complete the word: ")
            val missingLetter = display.substring(display.indexOf('_') + 1, display.indexOf('_') + 2)
            assertEquals("s_chool", display, "Blanked display should blank the 2nd letter")
            val correctOption = blank.options[blank.correctIndex]
            assertEquals("c", correctOption, "The correct option must be the blanked letter 'c'")
            assertNotEquals("s", correctOption, "The option must NOT be the first letter")
        }
    }

    @Test
    fun `difficulty coefficient drives subtitle chunk width`() {
        // PRIMARY band base coefficient is 1.0 and maxWordsPerSentence is 8.
        // A 9-word sentence: coefficient 1.3 -> 8 + 3 = 11 words per chunk (1 line),
        // coefficient 0.7 -> 8 - 3 = 5 words per chunk (2 lines).
        val template = """
            {
                "theme": "School Life",
                "difficulty_coefficient": %s,
                "days": [{
                    "day": 1,
                    "focus": "Vocabulary & Dialogue",
                    "target_words": ["classroom", "teacher"],
                    "reference_sentence": "The classroom is big and bright and clean today",
                    "duration_minutes": 15
                }]
            }
        """.trimIndent()

        val easy = builder.expandPlanToSession(template.format("0.7"), GradeBand.PRIMARY, dayIndex = 1)
        val hard = builder.expandPlanToSession(template.format("1.3"), GradeBand.PRIMARY, dayIndex = 1)

        assertNotNull(easy)
        assertNotNull(hard)
        assertTrue(
            easy!!.subtitleLines.size > hard!!.subtitleLines.size,
            "Lower coefficient should split the sentence into more (shorter) subtitle lines"
        )
    }
}
