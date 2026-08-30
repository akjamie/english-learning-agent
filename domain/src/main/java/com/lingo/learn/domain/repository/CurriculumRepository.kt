package org.akj.lingo.learn.domain.repository

import org.akj.lingo.learn.domain.model.CurriculumUnit
import org.akj.lingo.learn.domain.model.CurriculumVocabItem
import org.akj.lingo.learn.domain.model.GradeBand
import org.akj.lingo.learn.domain.model.VocabMasteryState

/**
 * Contract for accessing and mutating structured curriculum content.
 *
 * Data flow:
 *  1. App launch: [syncFromAssets] pre-seeds bundled units (res/raw/) into Room.
 *  2. Background: [syncFromRemote] pulls delta updates from the backend CMS by
 *     comparing [CurriculumUnit.contentVersion] and upserts changed units.
 *  3. Learner session: [getUnitForWeek] supplies the correct unit; the
 *     AdaptivePacingEngine calls [updateVocabMastery] after each quiz result.
 */
interface CurriculumRepository {

    // ── Read ──────────────────────────────────────────────────────────────────

    /** Retrieve a specific unit by its stable [unitId] (e.g. "PRIMARY_W01"). */
    suspend fun getUnitById(unitId: String): CurriculumUnit?

    /**
     * Retrieve the unit for a learner's current week within a [gradeBand].
     * Week number is 1-indexed and clamps to 1–52.
     */
    suspend fun getUnitForWeek(gradeBand: GradeBand, weekNumber: Int): CurriculumUnit?

    /** All units available for a grade band, ordered by weekNumber ascending. */
    suspend fun getUnitsForGrade(gradeBand: GradeBand): List<CurriculumUnit>

    /** Units pre-bundled and available offline (isAvailableOffline = true). */
    suspend fun getOfflineUnits(gradeBand: GradeBand): List<CurriculumUnit>

    // ── Mastery ───────────────────────────────────────────────────────────────

    /**
     * Update the mastery state and spaced repetition schedule for a single vocab item
     * after a quiz or practice result.
     *
     * @param vocabItemId    Stable ID of the vocab item: "{unitId}_{word}"
     * @param newState       The new [VocabMasteryState] computed by the adaptive engine
     * @param consecutiveCorrect  Running count of correct answers in a row (0 on wrong)
     * @param nextReviewTimestamp Epoch millis when the item should next be reviewed
     */
    suspend fun updateVocabMastery(
        vocabItemId: String,
        newState: VocabMasteryState,
        consecutiveCorrect: Int,
        nextReviewTimestamp: Long
    )

    /**
     * Return all vocab items due for spaced review for a learner (nextReviewTimestamp ≤ now).
     * Used by the AdaptivePacingEngine to inject review items into the daily session.
     */
    suspend fun getVocabItemsDueForReview(gradeBand: GradeBand, nowMs: Long): List<CurriculumVocabItem>

    // ── Sync ──────────────────────────────────────────────────────────────────

    /**
     * Seed the local database from pre-bundled assets (res/raw/curriculum_*.json).
     * Called once on first launch; no-ops if the unit already exists with the same
     * [CurriculumUnit.contentVersion].
     *
     * @return Number of units newly inserted or updated.
     */
    suspend fun syncFromAssets(): Int

    /**
     * Pull curriculum updates from the remote backend CMS.
     * Compares local [CurriculumUnit.contentVersion] against the remote manifest;
     * fetches and upserts only changed units.
     *
     * @return Number of units updated from remote.
     */
    suspend fun syncFromRemote(): Int
}
