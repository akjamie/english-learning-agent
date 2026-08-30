package org.akj.lingo.learn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.akj.lingo.learn.data.local.entity.VocabMasteryEntity

/** Data access for the vocab_mastery table (spaced repetition state). */
@Dao
interface VocabMasteryDao {

    @Query("SELECT * FROM vocab_mastery WHERE vocabItemId = :vocabItemId LIMIT 1")
    suspend fun getByVocabItemId(vocabItemId: String): VocabMasteryEntity?

    @Query("SELECT * FROM vocab_mastery WHERE unitId = :unitId")
    suspend fun getMasteryForUnit(unitId: String): List<VocabMasteryEntity>

    /**
     * Returns all vocab items whose spaced-review timestamp has passed.
     * Used by the AdaptivePacingEngine to inject review items into the daily session.
     */
    @Query("SELECT * FROM vocab_mastery WHERE gradeBand = :gradeBand AND nextReviewTimestamp > 0 AND nextReviewTimestamp <= :nowMs ORDER BY nextReviewTimestamp ASC")
    suspend fun getDueForReview(gradeBand: String, nowMs: Long): List<VocabMasteryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VocabMasteryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<VocabMasteryEntity>)

    @Query("UPDATE vocab_mastery SET masteryState = :state, consecutiveCorrect = :consecutive, nextReviewTimestamp = :nextReview, lastUpdated = :now WHERE vocabItemId = :vocabItemId")
    suspend fun updateMastery(
        vocabItemId: String,
        state: String,
        consecutive: Int,
        nextReview: Long,
        now: Long = System.currentTimeMillis()
    )
}
