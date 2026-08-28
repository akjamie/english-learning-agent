package org.akj.lingo.learn.domain.usecase

import kotlinx.coroutines.test.runTest
import org.akj.lingo.learn.domain.model.LearningRecord
import org.akj.lingo.learn.domain.repository.ErrorBookRepository
import org.akj.lingo.learn.domain.repository.LearningRecordRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StudentContextServiceTest {

    private class FakeLearningRecords(
        var accuracy: Float = 0.75f,
        var weakCategories: List<String> = listOf("Vocabulary"),
        var streak: Int = 5
    ) : LearningRecordRepository {
        override suspend fun saveSessionRecord(record: LearningRecord) {}
        override suspend fun getRecordsSince(timestamp: Long): List<LearningRecord> = emptyList()
        override suspend fun getWeeklyRecords(): List<LearningRecord> = emptyList()
        override suspend fun getMonthlyAccuracy(): Float = accuracy
        override suspend fun getWeakCategories(): List<String> = weakCategories
        override suspend fun getStreakDays(): Int = streak
        override suspend fun getTodayProgress(): Float = 0f
    }

    private class FakeErrorBook(var count: Int = 3) : ErrorBookRepository {
        override suspend fun upsertError(vocabId: String, errorType: String, questionType: String) {}
        override suspend fun markCorrect(vocabId: String) {}
        override suspend fun getTopPriorityErrors(limit: Int): List<org.akj.lingo.learn.domain.model.ErrorBookEntry> = emptyList()
        override suspend fun getErrorCount(): Int = count
        override suspend fun getReviewQuestionsForQuiz(count: Int): List<org.akj.lingo.learn.domain.model.QuizQuestion> = emptyList()
        override suspend fun getErrorsInObservation(): List<org.akj.lingo.learn.domain.model.ErrorBookEntry> = emptyList()
        override suspend fun retryErrorWord(vocabId: String) {}
    }

    @Test
    fun `exposes both accuracy scales so callers can't mix them`() = runTest {
        val records = FakeLearningRecords(accuracy = 0.75f)
        val service = StudentContextService(records, FakeErrorBook())

        val context = service.load("Grade 4", "B")

        assertEquals(0.75f, context.accuracyFraction)
        assertEquals(75, context.accuracyPercent)
    }

    @Test
    fun `builds milestones from streak and error count`() = runTest {
        val service = StudentContextService(FakeLearningRecords(streak = 5), FakeErrorBook(3))

        val context = service.load("Grade 4", "B")

        assertTrue(context.completedMilestones.contains("5-day streak"))
        assertTrue(context.completedMilestones.contains("3 words in error book"))
    }

    @Test
    fun `empty first week gets a starting milestone`() = runTest {
        val service = StudentContextService(FakeLearningRecords(streak = 0), FakeErrorBook(0))

        val context = service.load("Grade 4", "B")

        assertEquals(listOf("First week starting"), context.completedMilestones)
    }

    @Test
    fun `difficulty adjustment maps diagnostic level`() = runTest {
        val service = StudentContextService(FakeLearningRecords(), FakeErrorBook())

        assertEquals(-0.2f, service.load("Grade 4", "A").difficultyAdjustment)
        assertEquals(0f, service.load("Grade 4", "B").difficultyAdjustment)
        assertEquals(0.2f, service.load("Grade 4", "C").difficultyAdjustment)
    }

    @Test
    fun `cefr label derives from grade and level`() = runTest {
        val service = StudentContextService(FakeLearningRecords(), FakeErrorBook())

        // PRIMARY base A1; "C" shifts one level up to A2.
        assertEquals("A2 🌍", service.load("Grade 4", "C").cefrLabel)
    }
}
