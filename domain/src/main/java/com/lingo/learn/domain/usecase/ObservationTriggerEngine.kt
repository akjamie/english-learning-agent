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
        recentRecords: List<LearningRecord>
    ): Observation? {
        val wordLower = currentWord.lowercase()

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
}
