package org.akj.lingo.learn.domain.usecase

import javax.inject.Inject

/**
 * XP reward system (Sprint 7 Phase C1).
 *
 * Assigns experience points for learning actions and maps total XP to a level.
 * Levels are a pure function of cumulative XP so progression is deterministic,
 * testable, and can be persisted as a single integer.
 */
class XpRewardSystem @Inject constructor() {

    data class LevelInfo(
        val level: Int,
        val xpIntoLevel: Int,
        val xpForNextLevel: Int
    ) {
        val progressToNextLevel: Float
            get() = if (xpForNextLevel <= 0) 1f else (xpIntoLevel.toFloat() / xpForNextLevel).coerceIn(0f, 1f)
    }

    /**
     * XP awarded for a correct answer by question type.
     */
    fun xpForCorrect(questionType: String): Int = when (questionType.uppercase()) {
        "READ_ALOUD", "SPELLING", "DICTATION", "SENTENCE_WRITING", "CVC_BUILD", "ONSET_RIME", "MINIMAL_PAIRS" -> PRODUCTION_XP
        "SPELL_FILL_BLANK" -> PRODUCTION_XP
        else -> STANDARD_XP
    }

    /**
     * XP awarded for completing a full daily session (bonus).
     */
    fun xpForSessionCompletion(): Int = SESSION_COMPLETION_XP

    /**
     * Derives level + progress info from cumulative XP.
     */
    fun levelInfo(totalXp: Int): LevelInfo {
        var level = 1
        var remaining = totalXp.coerceAtLeast(0)
        var xpForNext = xpToNextLevel(level)
        while (remaining >= xpForNext && level < MAX_LEVEL) {
            remaining -= xpForNext
            level++
            xpForNext = xpToNextLevel(level)
        }
        return LevelInfo(
            level = level,
            xpIntoLevel = remaining,
            xpForNextLevel = if (level >= MAX_LEVEL) 0 else xpForNext
        )
    }

    /**
     * Whether gaining the given XP crosses a level boundary (for level-up animation).
     */
    fun crossesLevelBoundary(totalXpBefore: Int, totalXpAfter: Int): Boolean {
        val before = levelInfo(totalXpBefore).level
        val after = levelInfo(totalXpAfter).level
        return after > before
    }

    private fun xpToNextLevel(level: Int): Int = 50 + (level - 1) * 25

    companion object {
        const val STANDARD_XP = 10
        const val PRODUCTION_XP = 15
        const val SESSION_COMPLETION_XP = 20
        const val MAX_LEVEL = 30
    }
}
