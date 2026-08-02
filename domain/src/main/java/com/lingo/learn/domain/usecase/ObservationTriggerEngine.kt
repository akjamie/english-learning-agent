package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.model.LearningRecord
import javax.inject.Inject

enum class ObservationType {
    WORD_PREVIOUSLY_WRONG_NOW_CORRECT,
    PRONUNCIATION_IMPROVED,
    FAST_ANSWER,
    RETRY_THEN_CORRECT
}

data class Observation(
    val type: ObservationType,
    val word: String,
    val message: String
)

class ObservationTriggerEngine @Inject constructor() {

    fun checkObservation(
        currentWord: String,
        currentScore: Int?,
        questionType: String?,
        attemptCount: Int,
        recentRecords: List<LearningRecord>,
        responseTimeMs: Long? = null
    ): Observation? {
        val wordLower = currentWord.lowercase()

        // Sprint 11: FAST_ANSWER — a correct answer given well under the average
        // response time (5s vs a 10s default baseline) shows quick mastery.
        if (currentScore != null && currentScore >= 60) {
            val avg = averageResponseTimeMs(recentRecords) ?: DEFAULT_AVG_RESPONSE_MS
            if (responseTimeMs != null && responseTimeMs > 0 &&
                responseTimeMs < avg * FAST_ANSWER_FACTOR
            ) {
                return Observation(
                    type = ObservationType.FAST_ANSWER,
                    word = currentWord,
                    message = "Wow, that was fast! You really know \"$currentWord\"!"
                )
            }
        }

        val wordRecords = recentRecords.filter { record ->
            record.taskType == "QUIZ" || record.taskType == "GAME" || record.taskType == "SPEAKING"
        }

        val wordWrongHistory = wordRecords.any { record ->
            record.taskId.lowercase().contains(wordLower) && (record.accuracy < 0.6f)
        }

        if (wordWrongHistory && (currentScore == null || currentScore >= 60)) {
            return Observation(
                type = ObservationType.WORD_PREVIOUSLY_WRONG_NOW_CORRECT,
                word = currentWord,
                message = "You got \"$currentWord\" right this time — last week it was tricky!"
            )
        }

        val sameSentenceRecords = wordRecords.filter { record ->
            record.taskId.lowercase() == wordLower && record.accuracy > 0
        }

        if (currentScore != null && sameSentenceRecords.isNotEmpty()) {
            val bestPrevious = sameSentenceRecords.maxOf { it.accuracy }
            val currentAccuracy = currentScore / 100f
            if (currentAccuracy > bestPrevious && bestPrevious > 0) {
                return Observation(
                    type = ObservationType.PRONUNCIATION_IMPROVED,
                    word = currentWord,
                    message = "You're reading \"$currentWord\" better than before!"
                )
            }
        }

        if (attemptCount > 1 && currentScore != null && currentScore >= 60) {
            return Observation(
                type = ObservationType.RETRY_THEN_CORRECT,
                word = currentWord,
                message = "You kept trying and got it! That's the spirit!"
            )
        }

        return null
    }

    /**
     * Derives the child's average response time from historical records.
     * LearningRecords persist `duration` (seconds) per session; as a lightweight
     * baseline we average those (clamped to a sane range). Falls back to null when
     * there is no history so the default baseline is used.
     */
    private fun averageResponseTimeMs(recentRecords: List<LearningRecord>): Long? {
        val durations = recentRecords
            .asSequence()
            .map { it.duration }
            .filter { it in 1..60 }
            .toList()
        if (durations.isEmpty()) return null
        return (durations.average() * 1000).toLong()
    }

    companion object {
        /** Fallback baseline when no history exists (10 seconds). */
        const val DEFAULT_AVG_RESPONSE_MS = 10_000L

        /** A fast answer is one below this fraction of the average. */
        const val FAST_ANSWER_FACTOR = 0.5f
    }
}
