package org.akj.lingo.learn.data.repository

import kotlinx.coroutines.runBlocking
import org.akj.lingo.learn.data.local.dao.ConversationHistoryDao
import org.akj.lingo.learn.data.local.entity.ConversationHistoryEntity
import org.akj.lingo.learn.domain.model.ConversationMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

/**
 * Unit tests for [ConversationRepositoryImpl] — Sprint 11 conversation history.
 */
class ConversationRepositoryImplTest {

    private lateinit var dao: ConversationHistoryDao
    private lateinit var repository: ConversationRepositoryImpl

    @BeforeEach
    fun setup() {
        dao = Mockito.mock(ConversationHistoryDao::class.java)
        repository = ConversationRepositoryImpl(dao)
    }

    @Test
    fun `insertMessage persists entity via dao`() = runBlocking {
        val captor = argumentCaptor<ConversationHistoryEntity>()
        whenever(dao.insert(captor.capture())).thenReturn(Unit)

        val message = ConversationMessage(
            id = "m1", scenarioId = "zoo", role = "user",
            content = "I see a lion", timestamp = 1000L
        )

        repository.insertMessage(message)

        val saved = captor.firstValue
        assertEquals("m1", saved.id)
        assertEquals("zoo", saved.scenarioId)
        assertEquals("user", saved.role)
        assertEquals("I see a lion", saved.content)
    }

    @Test
    fun `getRecentMessages reverses to chronological order`() = runBlocking {
        val entities = listOf(
            ConversationHistoryEntity("a", "zoo", "assistant", "hi", 3000L),
            ConversationHistoryEntity("b", "zoo", "user", "hello", 2000L)
        )
        whenever(dao.getRecent(eq("zoo"), eq(10))).thenReturn(entities)

        val result = repository.getRecentMessages("zoo", 10)

        assertEquals(2, result.size)
        assertEquals(2000L, result[0].timestamp)
        assertEquals(3000L, result[1].timestamp)
    }

    @Test
    fun `clearScenario calls dao delete`() = runBlocking {
        whenever(dao.clearScenario(eq("travel"))).thenReturn(Unit)
        repository.clearScenario("travel")
        Mockito.verify(dao).clearScenario("travel")
    }
}
