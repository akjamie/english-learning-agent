package org.akj.lingo.learn.data.repository

import kotlinx.coroutines.runBlocking
import org.akj.lingo.learn.data.local.dao.LlmTraceDao
import org.akj.lingo.learn.data.local.entity.LlmTraceEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever

class LlmTraceRepositoryImplTest {

    private val dao = Mockito.mock(LlmTraceDao::class.java)
    private val repository = LlmTraceRepositoryImpl(dao)

    @Test
    fun `getRecentTraces maps entities to domain in order`() = runBlocking {
        val entities = listOf(
            LlmTraceEntity(
                id = 2,
                timestamp = 2000,
                taskType = "PLAN",
                model = "fallback",
                success = true,
                durationMs = 5000,
                inputTokens = 100,
                outputTokens = 300,
                totalTokens = 400,
                detail = "1234"
            ),
            LlmTraceEntity(
                id = 1,
                timestamp = 1000,
                taskType = "DIAGNOSIS",
                model = "primary",
                success = false,
                durationMs = 3000,
                inputTokens = 50,
                outputTokens = 0,
                totalTokens = 50,
                detail = "timeout"
            )
        )
        whenever(dao.getRecentTraces(20)).thenReturn(entities)

        val traces = repository.getRecentTraces(20)

        assertEquals(2, traces.size)
        assertEquals("PLAN", traces[0].taskType)
        assertEquals("fallback", traces[0].model)
        assertTrue(traces[0].success)
        assertEquals(5000L, traces[0].durationMs)
        assertEquals("DIAGNOSIS", traces[1].taskType)
        assertTrue(!traces[1].success)
    }

    @Test
    fun `getRecentTraces returns empty when dao has nothing`() = runBlocking {
        whenever(dao.getRecentTraces(20)).thenReturn(emptyList())

        assertTrue(repository.getRecentTraces(20).isEmpty())
    }
}
