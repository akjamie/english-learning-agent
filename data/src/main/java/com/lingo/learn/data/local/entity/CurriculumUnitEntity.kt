package org.akj.lingo.learn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.akj.lingo.learn.domain.model.CurriculumDialogueLine
import org.akj.lingo.learn.domain.model.CurriculumQuizItem
import org.akj.lingo.learn.domain.model.CurriculumVocabItem
import org.akj.lingo.learn.domain.model.CurriculumUnit
import org.akj.lingo.learn.domain.model.GradeBand
import org.akj.lingo.learn.domain.model.SentenceStructure

/**
 * Room entity for a single weekly curriculum unit.
 *
 * Nested collections (vocab, sentences, dialogue, quiz) are stored as JSON strings
 * via [AppTypeConverters] to avoid complex multi-table joins for read-heavy session loading.
 * The trade-off (slightly larger row, full collection replaced on update) is acceptable
 * because curriculum units are small (<5 KB each) and updated infrequently.
 *
 * VocabMasteryEntity is stored in a separate table (vocab_mastery) to support
 * per-item mastery updates without re-serializing the entire unit row.
 */
@Entity(tableName = "curriculum_unit")
data class CurriculumUnitEntity(
    @PrimaryKey val id: String,             // e.g. "PRIMARY_W01"
    val gradeBand: String,                  // GradeBand.name()
    val weekNumber: Int,
    val theme: String,
    val themeEmoji: String,
    val vocabItemsJson: String,             // JSON: List<CurriculumVocabItem> (sans mastery fields)
    val sentenceStructuresJson: String,     // JSON: List<SentenceStructure>
    val dialogueLinesJson: String,          // JSON: List<CurriculumDialogueLine>
    val quizItemsJson: String,              // JSON: List<CurriculumQuizItem>
    val contentVersion: String,
    val isAvailableOffline: Boolean = false,
    val roleplayScenarioId: String? = null,
    val lastSyncedAt: Long = System.currentTimeMillis()
) {
    /**
     * Convert to domain model. Mastery states are merged from [VocabMasteryEntity]
     * entries by the repository before returning to callers.
     * This conversion produces base mastery states (UNKNOWN) — the repository
     * overlays actual mastery data from the vocab_mastery table.
     */
    fun toDomain(
        vocabItems: List<CurriculumVocabItem>,
        sentenceStructures: List<SentenceStructure>,
        dialogueLines: List<CurriculumDialogueLine>,
        quizItems: List<CurriculumQuizItem>
    ) = CurriculumUnit(
        id = id,
        gradeBand = GradeBand.valueOf(gradeBand),
        weekNumber = weekNumber,
        theme = theme,
        themeEmoji = themeEmoji,
        vocabItems = vocabItems,
        sentenceStructures = sentenceStructures,
        dialogueLines = dialogueLines,
        quizItems = quizItems,
        contentVersion = contentVersion,
        isAvailableOffline = isAvailableOffline,
        roleplayScenarioId = roleplayScenarioId
    )
}
