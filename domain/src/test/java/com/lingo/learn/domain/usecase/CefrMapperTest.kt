package org.akj.lingo.learn.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CefrMapperTest {

    @Test
    fun `PRIMARY band maps to A1 base`() {
        assertEquals(CefrLevel.A1, CefrMapper.map("Grade 4", "B"))
        assertEquals(CefrLevel.A1, CefrMapper.map("Grade 5", "B"))
        assertEquals(CefrLevel.A1, CefrMapper.map("Grade 6", "B"))
    }

    @Test
    fun `JUNIOR band maps to A2 base`() {
        assertEquals(CefrLevel.A2, CefrMapper.map("Grade 7", "B"))
        assertEquals(CefrLevel.A2, CefrMapper.map("Grade 8", "B"))
        assertEquals(CefrLevel.A2, CefrMapper.map("Grade 9", "B"))
    }

    @Test
    fun `SENIOR band maps to B1 base`() {
        assertEquals(CefrLevel.B1, CefrMapper.map("Grade 10", "B"))
        assertEquals(CefrLevel.B1, CefrMapper.map("Grade 11", "B"))
        assertEquals(CefrLevel.B1, CefrMapper.map("Grade 12", "B"))
    }

    @Test
    fun `diagnostic A shifts one level down`() {
        assertEquals(CefrLevel.PRE_A1, CefrMapper.map("Grade 4", "A"))
        assertEquals(CefrLevel.A1, CefrMapper.map("Grade 7", "A"))
        assertEquals(CefrLevel.A2, CefrMapper.map("Grade 10", "A"))
    }

    @Test
    fun `diagnostic C shifts one level up`() {
        assertEquals(CefrLevel.A2, CefrMapper.map("Grade 4", "C"))
        assertEquals(CefrLevel.B1, CefrMapper.map("Grade 7", "C"))
        assertEquals(CefrLevel.B2, CefrMapper.map("Grade 10", "C"))
    }

    @Test
    fun `B2 does not shift above the ceiling`() {
        assertEquals(CefrLevel.B2, CefrMapper.map("Grade 12", "C"))
    }

    @Test
    fun `PRE_A1 does not shift below the floor`() {
        assertEquals(CefrLevel.PRE_A1, CefrMapper.map("Grade 4", "A"))
    }

    @Test
    fun `unknown diagnostic level falls back to base`() {
        assertEquals(CefrLevel.A1, CefrMapper.map("Grade 4", "X"))
        assertEquals(CefrLevel.A2, CefrMapper.map("Grade 8", ""))
    }

    @Test
    fun `unknown grade falls back to PRIMARY`() {
        assertEquals(CefrLevel.A1, CefrMapper.map("unknown", "B"))
    }

    @Test
    fun `badge includes globe emoji`() {
        assertEquals("A1 🌍", CefrMapper.badge(CefrLevel.A1))
        assertEquals("B1 🌍", CefrMapper.badge(CefrLevel.B1))
    }
}
