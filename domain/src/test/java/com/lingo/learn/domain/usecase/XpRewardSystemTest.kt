package org.akj.lingo.learn.domain.usecase

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class XpRewardSystemTest {

    private val xp = XpRewardSystem()

    @Test
    fun `standard question types award base XP`() {
        assertEquals(XpRewardSystem.STANDARD_XP, xp.xpForCorrect("LISTEN_CHOOSE_WORD"))
        assertEquals(XpRewardSystem.STANDARD_XP, xp.xpForCorrect("MATCH_PAIR"))
    }

    @Test
    fun `production and phonics types award bonus XP`() {
        assertEquals(XpRewardSystem.PRODUCTION_XP, xp.xpForCorrect("SPELLING"))
        assertEquals(XpRewardSystem.PRODUCTION_XP, xp.xpForCorrect("DICTATION"))
        assertEquals(XpRewardSystem.PRODUCTION_XP, xp.xpForCorrect("CVC_BUILD"))
        assertEquals(XpRewardSystem.PRODUCTION_XP, xp.xpForCorrect("SENTENCE_WRITING"))
    }

    @Test
    fun `session completion has its own bonus`() {
        assertEquals(XpRewardSystem.SESSION_COMPLETION_XP, xp.xpForSessionCompletion())
    }

    @Test
    fun `zero XP starts at level one`() {
        val info = xp.levelInfo(0)
        assertEquals(1, info.level)
        assertEquals(0, info.xpIntoLevel)
        assertEquals(50, info.xpForNextLevel)
    }

    @Test
    fun `XP below next level threshold stays at level one`() {
        assertEquals(1, xp.levelInfo(49).level)
    }

    @Test
    fun `reaching the threshold levels up`() {
        assertEquals(2, xp.levelInfo(50).level)
        assertEquals(0, xp.levelInfo(50).xpIntoLevel)
    }

    @Test
    fun `progress is normalized between zero and one`() {
        val info = xp.levelInfo(25)
        assertTrue(info.progressToNextLevel in 0f..1f)
        assertEquals(0.5f, info.progressToNextLevel)
    }

    @Test
    fun `level is capped at max level`() {
        val info = xp.levelInfo(100_000)
        assertEquals(XpRewardSystem.MAX_LEVEL, info.level)
        assertEquals(0, info.xpForNextLevel)
        assertEquals(1f, info.progressToNextLevel)
    }

    @Test
    fun `crossesLevelBoundary detects a level up`() {
        assertTrue(xp.crossesLevelBoundary(40, 60))
        assertFalse(xp.crossesLevelBoundary(40, 45))
    }
}
