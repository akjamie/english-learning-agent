package org.akj.lingo.learn.domain.repository

import org.akj.lingo.learn.domain.model.ErrorBookEntry
import org.akj.lingo.learn.domain.model.QuizQuestion

/**
 * Domain repository interface for managing Error Book entries,
 * priority scoring, and review question extraction.
 */
interface ErrorBookRepository {
    /**
     * Insert or update an error entry when a user makes a mistake.
     */
    suspend fun upsertError(vocabId: String, errorType: String, questionType: String)

    /**
     * Record a correct answer for an error entry during review.
     * Increments consecutiveCorrectCount and updates status.
     */
    suspend fun markCorrect(vocabId: String)

    /**
     * Get top-priority error book entries sorted by the priority score formula.
     */
    suspend fun getTopPriorityErrors(limit: Int = 30): List<ErrorBookEntry>

    /**
     * Get total active un-consolidated error count.
     */
    suspend fun getErrorCount(): Int

    /**
     * Get review questions formatted for daily quiz integration.
     */
    suspend fun getReviewQuestionsForQuiz(count: Int = 2): List<QuizQuestion>
}
