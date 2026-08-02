package org.akj.lingo.learn.data.repository

import kotlinx.coroutines.runBlocking
import org.akj.lingo.learn.data.local.dao.GamificationStateDao
import org.akj.lingo.learn.data.local.entity.GamificationStateEntity
import org.akj.lingo.learn.domain.model.GamificationState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

/**
 * Unit tests for [GamificationRepositoryImpl] — Sprint 10 gamification persistence.
 */
class GamificationRepositoryImplTest {

    private lateinit var dao: GamificationStateDao
    private lateinit var repository: GamificationRepositoryImpl

    @BeforeEach
    fun setup() {
        dao = Mockito.mock(GamificationStateDao::class.java)
        repository = GamificationRepositoryImpl(dao)
    }

    @Test
    fun `getState returns default when nothing persisted`() = runBlocking {
        whenever(dao.getState()).thenReturn(null)

        val state = repository.getState()

        assertEquals(0, state.totalXp)
        assertNull(state.makeupMonthKey)
        assertFalse(state.sessionGoalAchieved)
    }

    @Test
    fun `getState maps persisted entity to domain`() = runBlocking {
        val entity = GamificationStateEntity(
            id = 1, totalXp = 120, makeupMonthKey = "2026-08", makeupCardsUsed = 1,
            dailyGoalsDate = "2026-08-02", sessionGoalAchieved = true,
            accuracyGoalAchieved = true, wordsGoalAchieved = false, lastModified = 5000L
        )
        whenever(dao.getState()).thenReturn(entity)

        val state = repository.getState()

        assertEquals(120, state.totalXp)
        assertEquals("2026-08", state.makeupMonthKey)
        assertEquals(1, state.makeupCardsUsed)
        assertTrue(state.sessionGoalAchieved)
        assertFalse(state.wordsGoalAchieved)
    }

    @Test
    fun `saveState persists entity via dao`() = runBlocking {
        val captor = argumentCaptor<GamificationStateEntity>()
        whenever(dao.saveState(captor.capture())).thenReturn(Unit)

        val state = GamificationState(
            totalXp = 80,
            makeupMonthKey = "2026-08",
            makeupCardsUsed = 0,
            dailyGoalsDate = "2026-08-02",
            sessionGoalAchieved = true,
            accuracyGoalAchieved = true,
            wordsGoalAchieved = true,
            lastModified = 9000L
        )

        repository.saveState(state)

        val saved = captor.firstValue
        assertEquals(80, saved.totalXp)
        assertEquals("2026-08", saved.makeupMonthKey)
        assertEquals(0, saved.makeupCardsUsed)
        assertTrue(saved.sessionGoalAchieved)
        assertTrue(saved.wordsGoalAchieved)
    }

    @Test
    fun `resetDailyGoals clears goal flags but keeps cumulative xp`() = runBlocking {
        val existing = GamificationStateEntity(
            id = 1, totalXp = 200, makeupMonthKey = "2026-08", makeupCardsUsed = 1,
            dailyGoalsDate = "2026-08-01", sessionGoalAchieved = true,
            accuracyGoalAchieved = true, wordsGoalAchieved = true, lastModified = 1000L
        )
        whenever(dao.getState()).thenReturn(existing)
        val captor = argumentCaptor<GamificationStateEntity>()
        whenever(dao.saveState(captor.capture())).thenReturn(Unit)

        repository.resetDailyGoals(todayIsoDate = "2026-08-02", nowMs = 2000L)

        val saved = captor.firstValue
        assertEquals(200, saved.totalXp)
        assertEquals("2026-08-02", saved.dailyGoalsDate)
        assertFalse(saved.sessionGoalAchieved)
        assertFalse(saved.accuracyGoalAchieved)
        assertFalse(saved.wordsGoalAchieved)
        assertEquals(2000L, saved.lastModified)
    }
}
