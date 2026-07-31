package org.akj.lingo.learn.data.repository

import kotlinx.coroutines.runBlocking
import org.akj.lingo.learn.data.local.dao.AgentDecisionLogDao
import org.akj.lingo.learn.data.local.entity.AgentDecisionLogEntity
import org.akj.lingo.learn.domain.model.AgentDecisionLog
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class AgentDecisionLogRepositoryImplTest {

    private lateinit var dao: AgentDecisionLogDao
    private lateinit var repository: AgentDecisionLogRepositoryImpl

    @BeforeEach
    fun setup() {
        dao = Mockito.mock(AgentDecisionLogDao::class.java)
        repository = AgentDecisionLogRepositoryImpl(dao)
    }

    @Test
    fun `insert persists entity fields via dao`() = runBlocking {
        val captor = argumentCaptor<AgentDecisionLogEntity>()
        whenever(dao.insert(captor.capture())).thenReturn(Unit)

        val log = AgentDecisionLog(
            id = "obs-1",
            timestamp = 1000L,
            decisionType = "OBSERVATION_MADE",
            title = "Lingo observed",
            description = "You got it right!",
            metadata = """{"type":"QUIZ"}""",
            confidence = 0.85f,
            lastModified = 2000L
        )

        repository.insert(log)

        val saved = captor.firstValue
        assertEquals("obs-1", saved.id)
        assertEquals(1000L, saved.timestamp)
        assertEquals("OBSERVATION_MADE", saved.decisionType)
        assertEquals("You got it right!", saved.description)
        assertEquals(0.85f, saved.confidence)
        assertEquals(2000L, saved.lastModified)
    }

    @Test
    fun `getRecentDecisions maps entities to domain with limit`() = runBlocking {
        val entities = listOf(
            AgentDecisionLogEntity(
                id = "a", timestamp = 3000L, decisionType = "PLAN_GENERATED",
                title = "Plan", description = "Rearranged week", metadata = "{}",
                confidence = 1.0f, lastModified = 3000L
            ),
            AgentDecisionLogEntity(
                id = "b", timestamp = 2000L, decisionType = "OBSERVATION_MADE",
                title = "Obs", description = "Noticed word", metadata = "{}",
                confidence = 0.9f, lastModified = 2000L
            )
        )
        whenever(dao.getRecentDecisions(eq(10))).thenReturn(entities)

        val result = repository.getRecentDecisions(limit = 10)

        assertEquals(2, result.size)
        assertEquals("PLAN_GENERATED", result[0].decisionType)
        assertEquals("OBSERVATION_MADE", result[1].decisionType)
        assertEquals(0.9f, result[1].confidence)
    }

    @Test
    fun `getDecisionsSince returns only entries after timestamp`() = runBlocking {
        val entities = listOf(
            AgentDecisionLogEntity(
                id = "c", timestamp = 5000L, decisionType = "DIFFICULTY_ADJUSTED",
                title = "Adj", description = "Adjusted difficulty", metadata = "{}",
                confidence = 0.7f, lastModified = 5000L
            )
        )
        whenever(dao.getDecisionsSince(eq(1000L))).thenReturn(entities)

        val result = repository.getDecisionsSince(1000L)

        assertEquals(1, result.size)
        assertEquals("DIFFICULTY_ADJUSTED", result[0].decisionType)
    }
}
