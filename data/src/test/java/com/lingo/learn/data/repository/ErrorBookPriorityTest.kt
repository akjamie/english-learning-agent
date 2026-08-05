package org.akj.lingo.learn.data.repository

import kotlinx.coroutines.runBlocking
import org.akj.lingo.learn.data.local.dao.ErrorBookDao
import org.akj.lingo.learn.data.local.entity.ErrorBookEntity
import org.akj.lingo.learn.domain.repository.ErrorBookRepository
import org.akj.lingo.learn.domain.usecase.SpacedRepetitionScheduler
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import kotlin.math.ln

class ErrorBookPriorityTest {

    private lateinit var dao: ErrorBookDao
    private lateinit var repository: ErrorBookRepository

    @BeforeEach
    fun setup() {
        dao = Mockito.mock(ErrorBookDao::class.java)
        repository = ErrorBookRepositoryImpl(dao, SpacedRepetitionScheduler())
    }

    @Test
    fun `new error with count 1 and recent timestamp has baseline priority`() = runBlocking {
        whenever(dao.getErrorByVocabId(eq("apple"))).thenReturn(null)
        val captor = argumentCaptor<ErrorBookEntity>()
        whenever(dao.insertOrUpdate(captor.capture())).thenReturn(Unit)

        repository.upsertError("apple", "SPELLED_WRONG", "SPELL")

        val saved = captor.firstValue
        assertEquals("apple", saved.vocabId)
        assertEquals(1, saved.errorCount)
        assertEquals("SPELLED_WRONG", saved.errorType)
        assertEquals("SPELL", saved.questionType)
        assertEquals("TO_REVIEW", saved.status)
        assertEquals(1.95f, saved.priorityScore, 0.01f)
    }

    @Test
    fun `priority increases with higher error count`() = runBlocking {
        val now = System.currentTimeMillis()
        val existing = createEntity("apple", 3, now, "SPELL")
        whenever(dao.getErrorByVocabId(eq("apple"))).thenReturn(existing)
        val captor = argumentCaptor<ErrorBookEntity>()
        whenever(dao.update(captor.capture())).thenReturn(Unit)

        repository.upsertError("apple", "SPELLED_WRONG", "SPELL")

        val updated = captor.firstValue
        assertEquals(4, updated.errorCount)
        assertEquals(0, updated.consecutiveCorrectCount)
        val expectedLog = (ln(5.0) / ln(2.0)).toFloat()
        val expectedPriority = expectedLog * 1.5f * 1.3f
        assertEquals(expectedPriority, updated.priorityScore, 0.01f)
    }

    @Test
    fun `recent errors have higher timeDecayFactor than old errors`() = runBlocking {
        val now = System.currentTimeMillis()
        val recentTime = now - 3 * 24 * 3600 * 1000L
        val oldTime = now - 14 * 24 * 3600 * 1000L
        // Start with stale priorities (0f) so refreshPriorityScores recalculates
        val recentEntry = createEntity("recent", 2, recentTime, "LISTEN_CHOOSE_WORD", priorityOverride = 0f)
        val oldEntry = createEntity("old", 2, oldTime, "LISTEN_CHOOSE_WORD", priorityOverride = 0f)
        whenever(dao.getTop30Errors()).thenReturn(listOf(oldEntry, recentEntry))
        val captor = argumentCaptor<ErrorBookEntity>()
        whenever(dao.update(captor.capture())).thenReturn(Unit)

        repository.getTopPriorityErrors(10)

        val recentUpdated = captor.allValues.first { it.vocabId == "recent" }
        val oldUpdated = captor.allValues.first { it.vocabId == "old" }
        assertTrue(recentUpdated.priorityScore > oldUpdated.priorityScore) {
            "recent=${recentUpdated.priorityScore} should be > old=${oldUpdated.priorityScore}"
        }
    }

    @Test
    fun `SPEAK_ALOUD type gets highest weight when same error count and recency`() = runBlocking {
        val now = System.currentTimeMillis()
        val speakEntry = createEntity("speak", 1, now, "SPEAK_ALOUD")
        val spellEntry = createEntity("spell", 1, now, "SPELL")
        val listenEntry = createEntity("listen", 1, now, "LISTEN_CHOOSE_WORD")
        // DAO returns unsorted; repository should recalculate and re-sort
        whenever(dao.getTop30Errors()).thenReturn(listOf(spellEntry, speakEntry, listenEntry))
        whenever(dao.update(any())).thenReturn(Unit)

        val sorted = repository.getTopPriorityErrors(10)

        assertEquals(3, sorted.size)
        val speakScore = sorted.first { it.vocabId == "speak" }.priorityScore
        val spellScore = sorted.first { it.vocabId == "spell" }.priorityScore
        val listenScore = sorted.first { it.vocabId == "listen" }.priorityScore
        assertTrue(speakScore > spellScore) { "speak=$speakScore should be > spell=$spellScore" }
        assertTrue(spellScore > listenScore) { "spell=$spellScore should be > listen=$listenScore" }
    }

    @Test
    fun `error beyond 30 days has lower priority than recent same-count error`() = runBlocking {
        val now = System.currentTimeMillis()
        val recentTime = now - 1 * 24 * 3600 * 1000L
        val veryOldTime = now - 60 * 24 * 3600 * 1000L
        val recentEntry = createEntity("recent", 1, recentTime, "LISTEN")
        val oldEntry = createEntity("old", 1, veryOldTime, "LISTEN")
        whenever(dao.getTop30Errors()).thenReturn(listOf(oldEntry, recentEntry))
        whenever(dao.update(any())).thenReturn(Unit)

        val sorted = repository.getTopPriorityErrors(10)

        val recentScore = sorted.first { it.vocabId == "recent" }.priorityScore
        val oldScore = sorted.first { it.vocabId == "old" }.priorityScore
        assertTrue(recentScore > oldScore) { "recent=$recentScore should be > old=$oldScore" }
    }

    @Test
    fun `markCorrect increments consecutive count and changes status`() = runBlocking {
        val now = System.currentTimeMillis()
        val existing = createEntity("book", 2, now, "SPELL")
        whenever(dao.getErrorByVocabId(eq("book"))).thenReturn(existing)
        val captor = argumentCaptor<ErrorBookEntity>()
        whenever(dao.update(captor.capture())).thenReturn(Unit)

        repository.markCorrect("book")

        val updated = captor.firstValue
        assertEquals(1, updated.consecutiveCorrectCount)
        assertEquals("CONSOLIDATED", updated.status)
    }

    @Test
    fun `three consecutive correct marks triggers GRADUATION_OBSERVATION`() = runBlocking {
        val now = System.currentTimeMillis()
        val existing = createEntity("perfect", 1, now, "SPELL", consecutiveCorrect = 2)
        whenever(dao.getErrorByVocabId(eq("perfect"))).thenReturn(existing)
        val captor = argumentCaptor<ErrorBookEntity>()
        whenever(dao.update(captor.capture())).thenReturn(Unit)

        repository.markCorrect("perfect")

        val updated = captor.firstValue
        assertEquals(3, updated.consecutiveCorrectCount)
        assertEquals("GRADUATION_OBSERVATION", updated.status)
        assertTrue(updated.graduationCheckTimestamp > 0)
    }

    @Test
    fun `getErrorCount returns active count`() = runBlocking {
        whenever(dao.getActiveErrorCount()).thenReturn(5)
        assertEquals(5, repository.getErrorCount())
    }

    @Test
    fun `getErrorsInObservation maps entities to domain entries`() = runBlocking {
        val now = System.currentTimeMillis()
        val entities = listOf(
            createEntity("watch1", 2, now, "SPELL", consecutiveCorrect = 3).copy(status = "GRADUATION_OBSERVATION"),
            createEntity("watch2", 1, now, "READ_ALOUD", consecutiveCorrect = 3).copy(status = "GRADUATION_OBSERVATION")
        )
        whenever(dao.getErrorsInObservation()).thenReturn(entities)

        val result = repository.getErrorsInObservation()

        assertEquals(2, result.size)
        assertTrue(result.all { it.status == "GRADUATION_OBSERVATION" })
        assertEquals("watch1", result[0].vocabId)
        assertEquals(3, result[0].consecutiveCorrectCount)
    }

    @Test
    fun `getErrorsInObservation returns empty when no entries in observation`() = runBlocking {
        whenever(dao.getErrorsInObservation()).thenReturn(emptyList())

        val result = repository.getErrorsInObservation()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getReviewQuestionsForQuiz returns expected format`() = runBlocking {
        val now = System.currentTimeMillis()
        val entries = listOf(
            createEntity("mistake1", 3, now, "SPELL"),
            createEntity("mistake2", 1, now, "READ_ALOUD")
        )
        whenever(dao.getTop30Errors()).thenReturn(entries)
        whenever(dao.update(any())).thenReturn(Unit)

        val questions = repository.getReviewQuestionsForQuiz(2)

        assertEquals(2, questions.size)
        assertTrue(questions.all { it.isFromErrorBook })
        assertTrue(questions[0].question.contains("mistake1"))
        assertTrue(questions[1].question.contains("mistake2"))
    }

    @Test
    fun `priority formula produces log2 scale for error count`() {
        val now = System.currentTimeMillis()
        val score1 = calculateExpectedPriority(1, now, "DEFAULT")
        val score3 = calculateExpectedPriority(3, now, "DEFAULT")
        val score7 = calculateExpectedPriority(7, now, "DEFAULT")

        assertTrue(score1 > 0f)
        assertTrue(score3 > score1)
        assertTrue(score7 > score3)
    }

    @Test
    fun `time decay produces correct factor values at boundaries`() {
        val now = System.currentTimeMillis()
        val boundary7 = now - 7 * 24 * 3600 * 1000L
        val boundary8 = now - 8 * 24 * 3600 * 1000L
        val boundary30 = now - 30 * 24 * 3600 * 1000L
        val boundary31 = now - 31 * 24 * 3600 * 1000L

        assertEquals(1.5f, calculateExpectedPriority(1, boundary7, "DEFAULT"), 0.001f)
        assertEquals(1.0f, calculateExpectedPriority(1, boundary8, "DEFAULT"), 0.001f)
        assertEquals(1.0f, calculateExpectedPriority(1, boundary30, "DEFAULT"), 0.001f)
        assertEquals(0.8f, calculateExpectedPriority(1, boundary31, "DEFAULT"), 0.001f)
    }

    @Test
    fun `question type weights produce correct ratios`() {
        val now = System.currentTimeMillis()
        val speak = calculateExpectedPriority(1, now, "SPEAK_ALOUD")
        val spell = calculateExpectedPriority(1, now, "SPELL_FILL_BLANK")
        val default = calculateExpectedPriority(1, now, "LISTEN_CHOOSE_WORD")

        assertEquals(1.5f, speak / default, 0.001f)
        assertEquals(1.3f, spell / default, 0.001f)
    }

    @Test
    fun `upsertError does not update priority when getErrorByVocabId returns null`() = runBlocking {
        whenever(dao.getErrorByVocabId(any())).thenReturn(null)
        val captor = argumentCaptor<ErrorBookEntity>()
        whenever(dao.insertOrUpdate(captor.capture())).thenReturn(Unit)

        repository.upsertError("new_word", "GRAMMAR_WRONG", "SENTENCE_ORDER")

        val saved = captor.firstValue
        assertEquals("new_word", saved.vocabId)
        assertEquals(1, saved.errorCount)
        assertTrue(saved.priorityScore > 0f)
    }

    private fun createEntity(
        vocabId: String,
        errorCount: Int,
        lastTimestamp: Long,
        questionType: String,
        consecutiveCorrect: Int = 0,
        priorityOverride: Float? = null
    ): ErrorBookEntity {
        return ErrorBookEntity(
            id = "id_$vocabId",
            vocabId = vocabId,
            errorCount = errorCount,
            errorType = "SPELLED_WRONG",
            lastErrorTimestamp = lastTimestamp,
            questionType = questionType,
            priorityScore = priorityOverride ?: calculateExpectedPriority(errorCount, lastTimestamp, questionType),
            status = "TO_REVIEW",
            consecutiveCorrectCount = consecutiveCorrect,
            graduationCheckTimestamp = 0L,
            historyJson = "[]",
            lastModified = lastTimestamp
        )
    }

    private fun calculateExpectedPriority(errorCount: Int, lastTimestamp: Long, questionType: String): Float {
        val logFactor = (ln((errorCount + 1).toDouble()) / ln(2.0)).toFloat()
        val daysDiff = (java.lang.System.currentTimeMillis() - lastTimestamp) / (24 * 3600 * 1000f)
        val timeDecayFactor = when {
            daysDiff <= 7f -> 1.5f
            daysDiff <= 30f -> 1.0f
            else -> 0.8f
        }
        val questionTypeWeight = when (questionType.uppercase()) {
            "SPEAK_ALOUD", "READ_ALOUD" -> 1.5f
            "SPELL_FILL_BLANK", "SPELL" -> 1.3f
            else -> 1.0f
        }
        return logFactor * timeDecayFactor * questionTypeWeight
    }
}
