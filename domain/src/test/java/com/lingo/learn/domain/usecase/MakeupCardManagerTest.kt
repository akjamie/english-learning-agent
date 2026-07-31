package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.usecase.MakeupCardManager.MakeupState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MakeupCardManagerTest {

    private val manager = MakeupCardManager()
    private val nowMs = System.currentTimeMillis()

    @Test
    fun `fresh month grants a full allowance`() {
        val state = manager.stateForMonth(nowMs, storedMonthKey = "", cardsUsedPreviously = 0)
        assertEquals(2, state.cardsLeft)
        assertEquals(manager.monthKey(nowMs), state.monthKey)
    }

    @Test
    fun `same stored month keeps previously used cards`() {
        val currentKey = manager.monthKey(nowMs)
        val state = manager.stateForMonth(nowMs, storedMonthKey = currentKey, cardsUsedPreviously = 1)
        assertEquals(1, state.cardsLeft)
    }

    @Test
    fun `stale stored month resets the allowance`() {
        val state = manager.stateForMonth(nowMs, storedMonthKey = "1999-01", cardsUsedPreviously = 2)
        assertEquals(2, state.cardsLeft)
    }

    @Test
    fun `card can be used while cards remain`() {
        val state = MakeupState(monthKey = "2026-08", cardsGranted = 2, cardsUsed = 1)
        assertTrue(manager.canUseCard(state))
    }

    @Test
    fun `card cannot be used when exhausted`() {
        val state = MakeupState(monthKey = "2026-08", cardsGranted = 2, cardsUsed = 2)
        assertFalse(manager.canUseCard(state))
    }

    @Test
    fun `using a card increments the used count`() {
        val state = MakeupState(monthKey = "2026-08", cardsGranted = 2, cardsUsed = 1)
        val updated = manager.useCard(state)
        assertEquals(2, updated.cardsUsed)
        assertEquals(0, updated.cardsLeft)
    }

    @Test
    fun `using a card beyond allowance is a no-op`() {
        val state = MakeupState(monthKey = "2026-08", cardsGranted = 2, cardsUsed = 2)
        val updated = manager.useCard(state)
        assertEquals(state, updated)
    }

    @Test
    fun `month key is zero padded`() {
        assertEquals("2026-08", manager.monthKey(1_785_542_400_000L)) // Aug 2026
    }
}
