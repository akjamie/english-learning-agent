package org.akj.lingo.learn.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.akj.lingo.learn.data.content.CurriculumAssetLoader
import org.akj.lingo.learn.data.local.dao.CurriculumDao
import org.akj.lingo.learn.data.local.dao.VocabMasteryDao
import org.akj.lingo.learn.data.local.entity.CurriculumUnitEntity
import org.akj.lingo.learn.data.local.entity.VocabMasteryEntity
import org.akj.lingo.learn.domain.model.GradeBand
import org.akj.lingo.learn.domain.model.VocabMasteryState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

import org.mockito.kotlin.eq

class CurriculumRepositoryImplTest {

    private val curriculumDao = Mockito.mock(CurriculumDao::class.java)
    private val vocabMasteryDao = Mockito.mock(VocabMasteryDao::class.java)
    private val context = Mockito.mock(android.content.Context::class.java)
    private val assetLoader = CurriculumAssetLoader(context)
    private val gson = Gson()

    private val repository = CurriculumRepositoryImpl(
        curriculumDao = curriculumDao,
        vocabMasteryDao = vocabMasteryDao,
        assetLoader = assetLoader,
        gson = gson
    )

    @Test
    fun `syncFromAssets inserts all 10 units when database is empty`() = runBlocking {
        whenever(curriculumDao.getContentVersion(any())).thenReturn(null)
        whenever(vocabMasteryDao.getByVocabItemId(any())).thenReturn(null)

        val updatedCount = repository.syncFromAssets()

        assertEquals(10, updatedCount)
        verify(curriculumDao, Mockito.times(10)).upsert(any())
        // 10 units * 8 vocab items = 80 mastery entries
        verify(vocabMasteryDao, Mockito.times(80)).upsert(any())
    }

    @Test
    fun `syncFromAssets skips units that already have matching contentVersion`() = runBlocking {
        whenever(curriculumDao.getContentVersion(any())).thenReturn("2026.08.1")

        val updatedCount = repository.syncFromAssets()

        assertEquals(0, updatedCount)
        verify(curriculumDao, Mockito.never()).upsert(any())
    }

    @Test
    fun `getUnitForWeek returns domain unit with merged mastery states`() = runBlocking {
        val unit = assetLoader.loadGrade1Units().first()
        val entity = CurriculumUnitEntity(
            id = unit.id,
            gradeBand = unit.gradeBand.name,
            weekNumber = unit.weekNumber,
            theme = unit.theme,
            themeEmoji = unit.themeEmoji,
            vocabItemsJson = gson.toJson(unit.vocabItems),
            sentenceStructuresJson = gson.toJson(unit.sentenceStructures),
            dialogueLinesJson = gson.toJson(unit.dialogueLines),
            quizItemsJson = gson.toJson(unit.quizItems),
            contentVersion = unit.contentVersion,
            isAvailableOffline = unit.isAvailableOffline
        )

        whenever(curriculumDao.getByGradeAndWeek("PRIMARY", 1)).thenReturn(entity)
        whenever(vocabMasteryDao.getMasteryForUnit(unit.id)).thenReturn(
            listOf(
                VocabMasteryEntity(
                    vocabItemId = "PRIMARY_W01_mum",
                    unitId = unit.id,
                    gradeBand = "PRIMARY",
                    word = "mum",
                    masteryState = "MASTERED",
                    consecutiveCorrect = 4,
                    nextReviewTimestamp = 1700000000L
                )
            )
        )

        val result = repository.getUnitForWeek(GradeBand.PRIMARY, 1)

        assertNotNull(result)
        assertEquals("PRIMARY_W01", result?.id)
        assertEquals("My Family", result?.theme)
        val mum = result?.vocabItems?.firstOrNull { it.id == "PRIMARY_W01_mum" }
        assertNotNull(mum)
        assertEquals(VocabMasteryState.MASTERED, mum?.masteryState)
        assertEquals(4, mum?.consecutiveCorrect)
    }

    @Test
    fun `updateVocabMastery calls dao update with correct arguments`() = runBlocking {
        repository.updateVocabMastery(
            vocabItemId = "PRIMARY_W01_dad",
            newState = VocabMasteryState.NEAR_MASTERED,
            consecutiveCorrect = 2,
            nextReviewTimestamp = 1800000000L
        )

        verify(vocabMasteryDao).updateMastery(
            vocabItemId = eq("PRIMARY_W01_dad"),
            state = eq("NEAR_MASTERED"),
            consecutive = eq(2),
            nextReview = eq(1800000000L),
            now = any()
        )
    }
}

