package org.akj.lingo.learn.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.akj.lingo.learn.data.content.CurriculumAssetLoader
import org.akj.lingo.learn.data.local.dao.CurriculumDao
import org.akj.lingo.learn.data.local.dao.VocabMasteryDao
import org.akj.lingo.learn.data.local.entity.CurriculumUnitEntity
import org.akj.lingo.learn.data.local.entity.VocabMasteryEntity
import org.akj.lingo.learn.domain.model.CurriculumDialogueLine
import org.akj.lingo.learn.domain.model.CurriculumQuizItem
import org.akj.lingo.learn.domain.model.CurriculumUnit
import org.akj.lingo.learn.domain.model.CurriculumVocabItem
import org.akj.lingo.learn.domain.model.GradeBand
import org.akj.lingo.learn.domain.model.SentenceStructure
import org.akj.lingo.learn.domain.model.VocabMasteryState
import org.akj.lingo.learn.domain.repository.CurriculumRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurriculumRepositoryImpl @Inject constructor(
    private val curriculumDao: CurriculumDao,
    private val vocabMasteryDao: VocabMasteryDao,
    private val assetLoader: CurriculumAssetLoader,
    private val gson: Gson
) : CurriculumRepository {

    // ── Type tokens for Gson deserialization ──────────────────────────────────
    private val vocabListType = object : TypeToken<List<CurriculumVocabItem>>() {}.type
    private val sentenceListType = object : TypeToken<List<SentenceStructure>>() {}.type
    private val dialogueListType = object : TypeToken<List<CurriculumDialogueLine>>() {}.type
    private val quizListType = object : TypeToken<List<CurriculumQuizItem>>() {}.type

    // ── Read ──────────────────────────────────────────────────────────────────

    override suspend fun getUnitById(unitId: String): CurriculumUnit? =
        withContext(Dispatchers.IO) {
            curriculumDao.getById(unitId)?.toDomainWithMastery(unitId)
        }

    override suspend fun getUnitForWeek(gradeBand: GradeBand, weekNumber: Int): CurriculumUnit? =
        withContext(Dispatchers.IO) {
            val week = weekNumber.coerceIn(1, 52)
            curriculumDao.getByGradeAndWeek(gradeBand.name, week)
                ?.let { entity -> entity.toDomainWithMastery(entity.id) }
        }

    override suspend fun getUnitsForGrade(gradeBand: GradeBand): List<CurriculumUnit> =
        withContext(Dispatchers.IO) {
            curriculumDao.getAllForGrade(gradeBand.name).map { entity ->
                entity.toDomainWithMastery(entity.id)
            }
        }

    override suspend fun getOfflineUnits(gradeBand: GradeBand): List<CurriculumUnit> =
        withContext(Dispatchers.IO) {
            curriculumDao.getOfflineUnits(gradeBand.name).map { entity ->
                entity.toDomainWithMastery(entity.id)
            }
        }

    // ── Mastery ───────────────────────────────────────────────────────────────

    override suspend fun updateVocabMastery(
        vocabItemId: String,
        newState: VocabMasteryState,
        consecutiveCorrect: Int,
        nextReviewTimestamp: Long
    ) = withContext(Dispatchers.IO) {
        vocabMasteryDao.updateMastery(
            vocabItemId = vocabItemId,
            state = newState.name,
            consecutive = consecutiveCorrect,
            nextReview = nextReviewTimestamp,
            now = System.currentTimeMillis()
        )
    }

    override suspend fun getVocabItemsDueForReview(
        gradeBand: GradeBand,
        nowMs: Long
    ): List<CurriculumVocabItem> = withContext(Dispatchers.IO) {
        vocabMasteryDao.getDueForReview(gradeBand.name, nowMs).mapNotNull { masteryEntity ->
            // Retrieve the base vocab item from the parent unit
            val unitId = masteryEntity.unitId
            val unitEntity = curriculumDao.getById(unitId) ?: return@mapNotNull null
            val vocabItems: List<CurriculumVocabItem> = gson.fromJson(unitEntity.vocabItemsJson, vocabListType)
            val baseItem = vocabItems.firstOrNull { it.id == masteryEntity.vocabItemId }
                ?: return@mapNotNull null
            baseItem.copy(
                masteryState = runCatching { VocabMasteryState.valueOf(masteryEntity.masteryState) }
                    .getOrDefault(VocabMasteryState.UNKNOWN),
                consecutiveCorrect = masteryEntity.consecutiveCorrect,
                nextReviewTimestamp = masteryEntity.nextReviewTimestamp
            )
        }
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    override suspend fun syncFromAssets(): Int = withContext(Dispatchers.IO) {
        val units = assetLoader.loadGrade1Units()
        var updated = 0
        units.forEach { unit ->
            val existingVersion = curriculumDao.getContentVersion(unit.id)
            if (existingVersion != unit.contentVersion) {
                curriculumDao.upsert(unit.toEntity())
                // Ensure mastery rows exist for all vocab items (INSERT OR IGNORE semantics
                // via REPLACE only if not present — we check first to avoid wiping progress)
                unit.vocabItems.forEach { vocab ->
                    if (vocabMasteryDao.getByVocabItemId(vocab.id) == null) {
                        vocabMasteryDao.upsert(
                            VocabMasteryEntity(
                                vocabItemId = vocab.id,
                                unitId = unit.id,
                                gradeBand = unit.gradeBand.name,
                                word = vocab.word,
                                masteryState = VocabMasteryState.UNKNOWN.name,
                                lastUpdated = System.currentTimeMillis()
                            )
                        )
                    }
                }
                updated++
            }
        }
        updated
    }

    override suspend fun syncFromRemote(): Int {
        // Remote CMS sync is a H2 deliverable — stub returns 0.
        // Implementation will compare contentVersion from backend manifest
        // and call curriculumDao.upsert() for changed units.
        return 0
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Converts a [CurriculumUnitEntity] to the domain [CurriculumUnit], overlaying
     * real mastery state from the vocab_mastery table onto the base vocab items.
     */
    private suspend fun CurriculumUnitEntity.toDomainWithMastery(unitId: String): CurriculumUnit {
        val masteryMap = vocabMasteryDao.getMasteryForUnit(unitId)
            .associateBy { it.vocabItemId }

        val baseVocab: List<CurriculumVocabItem> = gson.fromJson(vocabItemsJson, vocabListType)
        val enrichedVocab = baseVocab.map { item ->
            val mastery = masteryMap[item.id]
            if (mastery != null) {
                item.copy(
                    masteryState = runCatching { VocabMasteryState.valueOf(mastery.masteryState) }
                        .getOrDefault(VocabMasteryState.UNKNOWN),
                    consecutiveCorrect = mastery.consecutiveCorrect,
                    nextReviewTimestamp = mastery.nextReviewTimestamp
                )
            } else item
        }

        val sentences: List<SentenceStructure> = gson.fromJson(sentenceStructuresJson, sentenceListType)
        val dialogue: List<CurriculumDialogueLine> = gson.fromJson(dialogueLinesJson, dialogueListType)
        val quiz: List<CurriculumQuizItem> = gson.fromJson(quizItemsJson, quizListType)

        return toDomain(enrichedVocab, sentences, dialogue, quiz)
    }

    /** Serialises a [CurriculumUnit] to its Room entity using Gson. */
    private fun CurriculumUnit.toEntity() = CurriculumUnitEntity(
        id = id,
        gradeBand = gradeBand.name,
        weekNumber = weekNumber,
        theme = theme,
        themeEmoji = themeEmoji,
        vocabItemsJson = gson.toJson(vocabItems),
        sentenceStructuresJson = gson.toJson(sentenceStructures),
        dialogueLinesJson = gson.toJson(dialogueLines),
        quizItemsJson = gson.toJson(quizItems),
        contentVersion = contentVersion,
        isAvailableOffline = isAvailableOffline,
        roleplayScenarioId = roleplayScenarioId,
        lastSyncedAt = System.currentTimeMillis()
    )
}
