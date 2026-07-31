package org.akj.lingo.learn.domain.usecase

import java.util.Calendar
import javax.inject.Inject

/**
 * Makeup card mechanic (Sprint 7 Phase D3).
 *
 * Children receive 2 makeup cards per calendar month. When a streak would break,
 * the app presents an *active choice*: the child (or parent) may use a makeup
 * card to preserve the streak, or decline and accept the break. Cards are never
 * auto-used — the mechanic exists to preserve the child's sense of agency.
 *
 * Pure Kotlin logic; the persistence layer stores {monthKey -> cardsLeft}.
 */
class MakeupCardManager @Inject constructor() {

    data class MakeupState(
        val monthKey: String,      // e.g. "2026-08"
        val cardsGranted: Int,
        val cardsUsed: Int
    ) {
        val cardsLeft: Int get() = (CARDS_PER_MONTH - cardsUsed).coerceAtLeast(0)
    }

    /**
     * Resolves the persistence state for the current month, granting a fresh
     * allowance whenever the stored month differs from the current one.
     *
     * @param storedMonthKey month key previously persisted (or "" / null)
     * @param cardsUsedPreviously cards already used in that stored month
     */
    fun stateForMonth(nowMs: Long, storedMonthKey: String?, cardsUsedPreviously: Int): MakeupState {
        val currentKey = monthKey(nowMs)
        return if (storedMonthKey == currentKey) {
            MakeupState(monthKey = currentKey, cardsGranted = CARDS_PER_MONTH, cardsUsed = cardsUsedPreviously)
        } else {
            // New month: fresh allowance.
            MakeupState(monthKey = currentKey, cardsGranted = CARDS_PER_MONTH, cardsUsed = 0)
        }
    }

    /**
     * Whether a makeup card can be used right now.
     */
    fun canUseCard(state: MakeupState): Boolean = state.cardsLeft > 0

    /**
     * Consumes one card and returns the updated state.
     * Returns the input unchanged if no cards remain.
     */
    fun useCard(state: MakeupState): MakeupState =
        if (canUseCard(state)) state.copy(cardsUsed = state.cardsUsed + 1) else state

    /**
     * Builds the calendar-month key for a timestamp (e.g. "2026-08").
     */
    fun monthKey(nowMs: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val yyyy = cal.get(Calendar.YEAR)
        val mm = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        return "$yyyy-$mm"
    }

    companion object {
        const val CARDS_PER_MONTH = 2
    }
}
