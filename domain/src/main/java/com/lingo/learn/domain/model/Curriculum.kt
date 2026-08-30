package org.akj.lingo.learn.domain.model

// ─────────────────────────────────────────────────────────────────────────────
// Curriculum — Structured content hierarchy
//
// Architecture:
//   CurriculumUnit (1 weekly themed lesson, editorially curated)
//     ├── List<CurriculumVocabItem>    (8-12 target words with full metadata)
//     ├── List<SentenceStructure>      (3 target sentence patterns)
//     ├── List<CurriculumDialogueLine> (immersive audio dialogue, 6-10 lines)
//     ├── List<CurriculumQuizItem>     (5 pre-authored quiz items)
//     └── roleplayScenarioId: String?  (links to RoleplayScenarioBank entry)
//
// Mastery lifecycle per VocabItem:
//   UNKNOWN → INTRODUCED → PRACTICED → NEAR_MASTERED → MASTERED → NEEDS_REVIEW
//
// Content versioning:
//   Each CurriculumUnit carries a contentVersion string (e.g. "2026.08.1") that
//   the backend CMS increments on editorial corrections. The app performs
//   incremental sync on launch and updates the local cache accordingly.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Mastery state for a single vocabulary item, tracked per learner.
 * Drives spaced repetition scheduling in the adaptive pacing engine.
 */
enum class VocabMasteryState {
    UNKNOWN,        // Not yet introduced
    INTRODUCED,     // Shown in immersive audio at least once
    PRACTICED,      // Successfully read aloud or matched at least once
    NEAR_MASTERED,  // Correct in ≥2 quiz sessions; review at 3-day interval
    MASTERED,       // Correct across ≥3 separate sessions; review at 14-day interval
    NEEDS_REVIEW    // Was mastered but failed in a recent spaced review
}

/**
 * A single target vocabulary item within a curriculum unit.
 *
 * @param word           The target English word (always English — never translated here)
 * @param ipa            IPA phonetic transcription for pronunciation display
 * @param partOfSpeech   Grammatical category (e.g., "noun", "verb", "adjective")
 * @param cefrLevel      CEFR level this word belongs to (e.g., "A1", "A2")
 * @param exampleSentence English example sentence demonstrating in-context use
 * @param chineseHint    Semantic Chinese translation shown only as scaffold when learner is stuck
 * @param imageQuery     Keyword for image lookup (illustrative)
 */
data class CurriculumVocabItem(
    val id: String,                                         // Stable key: "{unitId}_{word}"
    val word: String,
    val ipa: String,
    val partOfSpeech: String,
    val cefrLevel: String,
    val exampleSentence: String,
    val chineseHint: String,
    val imageQuery: String = word,
    val masteryState: VocabMasteryState = VocabMasteryState.UNKNOWN,
    val nextReviewTimestamp: Long = 0L,
    val consecutiveCorrect: Int = 0
)

/**
 * A target sentence structure pattern for the week's theme.
 * Always authentic English; chineseHint is scaffolding shown only on request.
 *
 * @param pattern     Sentence template, e.g. "Can I have a _____, please?"
 * @param example     Filled example, e.g. "Can I have a sandwich, please?"
 * @param chineseHint Semantic explanation shown only when learner requests help
 */
data class SentenceStructure(
    val id: String,
    val pattern: String,
    val example: String,
    val chineseHint: String
)

/**
 * A single line of the themed immersive audio dialogue script.
 *
 * @param speaker   Speaker label shown in transcript UI (e.g., "Lingo", "Tom")
 * @param text      English dialogue line — the immersive audio TTS input
 * @param newWords  Words from [CurriculumVocabItem] that appear in this line (for pop-up cards)
 * @param startMs   Pre-authored timing cue; null means TTS-derived timing
 * @param endMs     Pre-authored timing cue; null means TTS-derived timing
 */
data class CurriculumDialogueLine(
    val id: Int,
    val speaker: String,
    val text: String,
    val newWords: List<String> = emptyList(),
    val startMs: Long? = null,
    val endMs: Long? = null
)

/**
 * A pre-authored, editorially validated quiz question for a curriculum unit.
 * Differs from AI-generated [QuizQuestion] in that it is curated and version-controlled.
 *
 * @param questionText The question prompt — always English for comprehension/vocabulary types
 * @param type         Reuses existing [QuizQuestionType] taxonomy
 * @param options      Answer options in English for choice-based types
 * @param correctIndex Index of the correct option
 * @param targetWord   The vocabulary item this question is designed to assess
 */
data class CurriculumQuizItem(
    val id: String,
    val questionText: String,
    val type: QuizQuestionType,
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1,
    val correctOrder: List<String> = emptyList(),
    val targetWord: String
)

/**
 * The primary domain model for a single weekly curriculum unit.
 *
 * Each unit is authored for a specific [GradeBand] and week number within that band.
 * The LLM / AdaptivePacingEngine treats [CurriculumUnit] as the authoritative source
 * of truth for vocabulary and sentence structures, generating additional practice
 * variants around the curated core rather than replacing it.
 *
 * @param id               Stable unique key: "{gradeBand.name}_W{weekNumber:02d}"
 *                         e.g. "PRIMARY_W01", "PRIMARY_W52"
 * @param gradeBand        Which grade band this unit targets
 * @param weekNumber       1-indexed week within the grade band (1–52)
 * @param theme            Themed topic title in English, e.g. "At the Farm"
 * @param themeEmoji       Visual shorthand, e.g. "🐄"
 * @param contentVersion   Editorial version string for incremental sync (e.g. "2026.08.1")
 * @param isAvailableOffline  True when this unit is pre-bundled in res/raw/
 * @param roleplayScenarioId  Optional link to a RoleplayScenario in RoleplayScenarioBank
 */
data class CurriculumUnit(
    val id: String,
    val gradeBand: GradeBand,
    val weekNumber: Int,
    val theme: String,
    val themeEmoji: String,
    val vocabItems: List<CurriculumVocabItem>,
    val sentenceStructures: List<SentenceStructure>,
    val dialogueLines: List<CurriculumDialogueLine>,
    val quizItems: List<CurriculumQuizItem>,
    val contentVersion: String,
    val isAvailableOffline: Boolean = false,
    val roleplayScenarioId: String? = null
)
