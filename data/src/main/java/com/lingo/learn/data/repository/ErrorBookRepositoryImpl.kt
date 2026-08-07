package org.akj.lingo.learn.data.repository

import org.akj.lingo.learn.data.local.dao.ErrorBookDao
import org.akj.lingo.learn.data.local.entity.ErrorBookEntity
import org.akj.lingo.learn.domain.model.ErrorBookEntry
import org.akj.lingo.learn.domain.model.QuizQuestion
import org.akj.lingo.learn.domain.model.QuizQuestionType
import org.akj.lingo.learn.domain.repository.ErrorBookRepository
import org.akj.lingo.learn.domain.usecase.SpacedRepetitionScheduler
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln

@Singleton
class ErrorBookRepositoryImpl @Inject constructor(
    private val errorBookDao: ErrorBookDao,
    private val spacedRepetitionScheduler: SpacedRepetitionScheduler
) : ErrorBookRepository {

    override suspend fun upsertError(vocabId: String, errorType: String, questionType: String) {
        val now = System.currentTimeMillis()
        val existing = errorBookDao.getErrorByVocabId(vocabId)

        if (existing != null) {
            val newCount = existing.errorCount + 1
            val priority = calculatePriorityScore(newCount, now, questionType)
            val updated = existing.copy(
                errorCount = newCount,
                errorType = errorType,
                lastErrorTimestamp = now,
                questionType = questionType,
                priorityScore = priority,
                status = "TO_REVIEW",
                consecutiveCorrectCount = 0,
                nextReviewTimestamp = spacedRepetitionScheduler.nextReviewAfterError(now),
                lastModified = now
            )
            errorBookDao.update(updated)
        } else {
            val priority = calculatePriorityScore(1, now, questionType)
            val newEntry = ErrorBookEntity(
                id = UUID.randomUUID().toString(),
                vocabId = vocabId,
                errorCount = 1,
                errorType = errorType,
                lastErrorTimestamp = now,
                questionType = questionType,
                priorityScore = priority,
                status = "TO_REVIEW",
                consecutiveCorrectCount = 0,
                graduationCheckTimestamp = 0L,
                historyJson = "[]",
                nextReviewTimestamp = spacedRepetitionScheduler.nextReviewAfterError(now),
                lastModified = now
            )
            errorBookDao.insertOrUpdate(newEntry)
        }
    }

    override suspend fun markCorrect(vocabId: String) {
        val existing = errorBookDao.getErrorByVocabId(vocabId) ?: return
        val now = System.currentTimeMillis()
        val newCorrectCount = existing.consecutiveCorrectCount + 1
        val newStatus = when {
            newCorrectCount >= 3 -> "GRADUATION_OBSERVATION"
            newCorrectCount >= 1 -> "CONSOLIDATED"
            else -> existing.status
        }

        // Ebbinghaus spaced repetition: advance to the next review interval (1/3/7/14 days).
        val intervalIndex = spacedRepetitionScheduler.intervalIndexForTimestamp(existing.nextReviewTimestamp, now)
        val nextReview = spacedRepetitionScheduler.nextReviewAfterCorrect(now, intervalIndex + 1)

        val updated = existing.copy(
            consecutiveCorrectCount = newCorrectCount,
            status = newStatus,
            graduationCheckTimestamp = if (newStatus == "GRADUATION_OBSERVATION") now else existing.graduationCheckTimestamp,
            nextReviewTimestamp = nextReview,
            lastModified = now
        )
        errorBookDao.update(updated)
    }

    override suspend fun getTopPriorityErrors(limit: Int): List<ErrorBookEntry> {
        refreshPriorityScores()
        return errorBookDao.getTop30Errors().take(limit).map { it.toDomain() }
    }

    /** Recalculates priority for all active entries based on current time */
    private suspend fun refreshPriorityScores() {
        val allActive = errorBookDao.getTop30Errors() // actually all active non-graduated
        val now = System.currentTimeMillis()
        allActive.forEach { entity ->
            val newPriority = calculatePriorityScore(entity.errorCount, entity.lastErrorTimestamp, entity.questionType)
            if (newPriority != entity.priorityScore) {
                errorBookDao.update(entity.copy(priorityScore = newPriority, lastModified = now))
            }
        }
    }

    override suspend fun getErrorCount(): Int {
        return errorBookDao.getActiveErrorCount()
    }

    override suspend fun getErrorsInObservation(): List<ErrorBookEntry> {
        return errorBookDao.getErrorsInObservation().map { it.toDomain() }
    }

    override suspend fun retryErrorWord(vocabId: String) {
        val existing = errorBookDao.getErrorByVocabId(vocabId) ?: return
        // Sprint 20: force the review timestamp to now so the word is
        // picked up by getReviewQuestionsForQuiz on the next session start.
        errorBookDao.update(
            existing.copy(
                nextReviewTimestamp = System.currentTimeMillis(),
                lastModified = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getReviewQuestionsForQuiz(count: Int): List<QuizQuestion> {
        // Spaced repetition: only words that are due (scheduled timestamp passed) are reviewed.
        val now = System.currentTimeMillis()
        val dueEntries = errorBookDao.getTop30Errors()
            .filter { spacedRepetitionScheduler.isDue(it.nextReviewTimestamp, now) }
        return dueEntries.take(count).mapIndexed { index, entry ->
            val shuffledOptions = listOf(entry.vocabId, "apple", "banana", "cat").shuffled()
            QuizQuestion(
                id = 900 + index,
                type = QuizQuestionType.LISTEN_CHOOSE_WORD,
                question = "Review from Error Book: Choose correct word for '${entry.vocabId}':",
                audioText = entry.vocabId,
                options = shuffledOptions,
                correctIndex = shuffledOptions.indexOf(entry.vocabId),
                isFromErrorBook = true
            )
        }
    }

    private fun calculatePriorityScore(errorCount: Int, lastTimestamp: Long, questionType: String): Float {
        // Section 7.1 Formula: Priority = log2(errorCount + 1) * timeDecayFactor * questionTypeWeight
        val logFactor = (ln((errorCount + 1).toDouble()) / ln(2.0)).toFloat()

        val daysDiff = (System.currentTimeMillis() - lastTimestamp) / (24 * 3600 * 1000f)
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
