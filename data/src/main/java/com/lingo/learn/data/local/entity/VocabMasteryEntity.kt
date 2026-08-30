package org.akj.lingo.learn.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import org.akj.lingo.learn.domain.model.VocabMasteryState

/**
 * Per-learner mastery state for a single vocabulary item.
 *
 * Stored separately from [CurriculumUnitEntity] so individual mastery updates
 * (after each quiz answer) don't require re-serializing the entire unit row.
 * The composite primary key (vocabItemId) ensures one row per vocabulary item.
 */
@Entity(
    tableName = "vocab_mastery",
    indices = [Index(value = ["unitId", "gradeBand"]), Index(value = ["nextReviewTimestamp"])]
)
data class VocabMasteryEntity(
    @androidx.room.PrimaryKey val vocabItemId: String,  // "{unitId}_{word}"
    val unitId: String,
    val gradeBand: String,
    val word: String,
    val masteryState: String = VocabMasteryState.UNKNOWN.name,
    val consecutiveCorrect: Int = 0,
    val nextReviewTimestamp: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
)
