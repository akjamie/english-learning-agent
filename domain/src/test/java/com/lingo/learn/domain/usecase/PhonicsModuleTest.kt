package org.akj.lingo.learn.domain.usecase

import org.akj.lingo.learn.domain.model.QuizQuestionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PhonicsModuleTest {

    private val module = PhonicsModule()

    @Test
    fun `CVC word builds a CVC_BUILD question with correct order`() {
        val q = module.buildPhonicsQuestion("cat", 0)!!
        assertEquals(QuizQuestionType.CVC_BUILD, q.type)
        assertEquals(listOf("c", "a", "t"), q.correctOrder)
        assertEquals("cat", q.audioText)
        assertTrue(q.options.size >= 4)
    }

    @Test
    fun `CVC word builds an ONSET_RIME question with correct rime`() {
        val q = module.buildPhonicsQuestion("cat", 1)!!
        assertEquals(QuizQuestionType.ONSET_RIME, q.type)
        assertEquals("at", q.options[q.correctIndex])
        assertEquals(4, q.options.size)
    }

    @Test
    fun `CVC word builds a MINIMAL_PAIRS question`() {
        val q = module.buildPhonicsQuestion("cat", 2)!!
        assertEquals(QuizQuestionType.MINIMAL_PAIRS, q.type)
        assertEquals("cat", q.options[q.correctIndex])
        assertEquals(2, q.options.size)
    }

    @Test
    fun `non CVC word returns null`() {
        assertNull(module.buildPhonicsQuestion("apple", 0))
        assertNull(module.buildPhonicsQuestion("ab", 0))
        assertNull(module.buildPhonicsQuestion("xyz", 0)) // no vowel in middle
        assertNull(module.buildPhonicsQuestion("aaa", 0)) // not consonant-vowel-consonant
    }

    @Test
    fun `word detection is case insensitive and trimmed`() {
        assertTrue(module.isPhonicsWord("  CAT "))
        assertTrue(module.isPhonicsWord("dog"))
        assertFalse(module.isPhonicsWord("elephant"))
    }

    @Test
    fun `question type rotates with question id`() {
        val q0 = module.buildPhonicsQuestion("dog", 0)!!
        val q1 = module.buildPhonicsQuestion("dog", 1)!!
        val q2 = module.buildPhonicsQuestion("dog", 2)!!
        assertEquals(setOf(QuizQuestionType.CVC_BUILD, QuizQuestionType.ONSET_RIME, QuizQuestionType.MINIMAL_PAIRS),
            setOf(q0.type, q1.type, q2.type))
    }

    @Test
    fun `options never contain duplicate tiles in CVC build`() {
        val q = module.buildPhonicsQuestion("sun", 0)!!
        assertEquals(q.options.size, q.options.toSet().size)
    }
}
