package org.akj.lingo.learn.domain.model

//region Stage 1: Immersive Audio Import

/**
 * A single subtitle line synced to audio playback timestamps.
 * Used by the immersive audio player for sentence-by-sentence highlighting.
 */
data class SubtitleLine(
    val id: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val newWords: List<String> = emptyList()
)

//endregion

//region Stage 2: Read-along & Consolidation Games

/**
 * A sentence the child reads aloud for ASR pronunciation evaluation.
 */
data class ReadAlongSentence(
    val id: Int,
    val text: String,
    val chineseHint: String,
    val audioPath: String? = null
)

/**
 * Question types for the consolidation mini-games that follow read-along practice.
 */
enum class GameType {
    DRAG_MATCH,         // Drag vocabulary word to its matching Chinese meaning
    LISTEN_CHOOSE_IMAGE // Listen to audio and choose the correct image
}

/**
 * A single mini-game question. The options and correct index vary by game type.
 */
data class GameQuestion(
    val id: Int,
    val type: GameType,
    val prompt: String,
    val audioText: String? = null,
    val options: List<String>,
    val correctIndex: Int
)

//endregion

//region Stage 3: Daily Quiz

/**
 * Question types supported in the daily micro-quiz.
 */
enum class QuizQuestionType {
    IMAGE_CHOOSE_WORD,  // Pick the correct word for a shown image
    LISTEN_CHOOSE_WORD, // Listen to audio and pick the matching word
    SPELL_FILL_BLANK,   // Fill in the missing letter(s) of a word
    SENTENCE_ORDER,     // Reorder shuffled words into a correct sentence
    READ_ALOUD,         // Read aloud for pronunciation scoring
    SPELLING,           // Active spelling production
    DICTATION,          // Listen and type the full sentence
    CVC_BUILD,          // Phonics: build a CVC word from letter tiles
    ONSET_RIME,         // Phonics: match onset and rime to form a word
    MINIMAL_PAIRS,      // Phonics: discriminate between minimal-pair words
    SENTENCE_WRITING    // Production: write a sentence using the target word
}

/**
 * A single quiz question. Options and correctIndex are used for choice-based types.
 * For SENTENCE_ORDER, options hold the shuffled tokens and correctIndex is unused;
 * correctOrder holds the expected token sequence.
 */
data class QuizQuestion(
    val id: Int,
    val type: QuizQuestionType,
    val question: String,
    val audioText: String? = null,
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1,
    val correctOrder: List<String> = emptyList(),
    val isFromErrorBook: Boolean = false
)

//endregion

//region Session Aggregate

/**
 * The full content payload for one daily learning session.
 * Contains all data needed across the three stages (immersion, practice, quiz).
 */
data class LearningSession(
    val theme: String,
    val subtitleLines: List<SubtitleLine>,
    val readAlongSentences: List<ReadAlongSentence>,
    val gameQuestions: List<GameQuestion>,
    val quizQuestions: List<QuizQuestion>,
    val targetNewWords: List<String>
)

//endregion

//region Completion Summary

/**
 * Achievement summary shown on the task completion page after finishing all stages.
 */
data class SessionSummary(
    val newWordsLearned: Int,
    val totalNewWords: Int,
    val streakDays: Int,
    val weeklyDayNumber: Int,
    val weeklyTotalDays: Int,
    val quizScore: Int,
    val quizTotal: Int,
    /**
     * Average pronunciation score over successful ASR evaluations; null when no
     * recording was evaluated (e.g. ASR unavailable during the session).
     */
    val pronunciationScore: Int?,
    /** Phoneme hints collected during the session for Parent Companion Card. */
    val phonemeHints: List<org.akj.lingo.learn.domain.usecase.PhonemeHintEngine.PhonemeHint> = emptyList()
)

//endregion
