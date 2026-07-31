package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.model.Plan
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExplainDecisionUseCaseTest {

    private val useCase = ExplainDecisionUseCase()

    private fun plan(rationaleSnapshot: String?) = Plan(
        id = "p1",
        type = "WEEKLY",
        startDate = 0L,
        endDate = 1L,
        theme = "School",
        difficultyCoefficient = 1.0f,
        reviewRatio = 0.2f,
        speechTopics = "",
        weeklyTarget = "",
        snapshotData = "{}",
        dialogueOutput = null,
        rationaleSnapshot = rationaleSnapshot,
        status = "ACTIVE",
        lastModified = 0L
    )

    @Test
    fun `dayRationale returns stored rationale for matching day`() {
        val snapshot = """
            {"days":[{"day":1,"rationale":"Listening was weak last week"},{"day":3,"rationale":"Grammar needs practice"}]}
        """.trimIndent()

        assertEquals("Listening was weak last week", useCase.dayRationale(plan(snapshot), 1))
        assertEquals("Grammar needs practice", useCase.dayRationale(plan(snapshot), 3))
    }

    @Test
    fun `dayRationale returns null when no rationale for the day`() {
        val snapshot = """{"days":[{"day":1,"rationale":"Listening was weak last week"}]}"""
        assertNull(useCase.dayRationale(plan(snapshot), 5))
    }

    @Test
    fun `dayRationale returns null for blank rationale`() {
        val snapshot = """{"days":[{"day":1,"rationale":""}]}"""
        assertNull(useCase.dayRationale(plan(snapshot), 1))
    }

    @Test
    fun `dayRationale returns null when rationaleSnapshot is null`() {
        assertNull(useCase.dayRationale(plan(null), 1))
    }

    @Test
    fun `dayRationale returns null on malformed json`() {
        assertNull(useCase.dayRationale(plan("not json at all"), 1))
    }

    @Test
    fun `allDayRationales returns only days with non-blank rationale`() {
        val snapshot = """
            {"days":[{"day":1,"rationale":"A"},{"day":2,"rationale":""},{"day":4,"rationale":"B"}]}
        """.trimIndent()

        val result = useCase.allDayRationales(plan(snapshot))
        assertEquals(listOf(1 to "A", 4 to "B"), result)
    }

    @Test
    fun `allDayRationales returns empty for null snapshot`() {
        assertTrue(useCase.allDayRationales(plan(null)).isEmpty())
    }
}
