package org.akj.lingo.learn.data.content

import android.content.Context
import org.akj.lingo.learn.domain.model.GradeBand
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class CurriculumAssetLoaderTest {

    private val context = Mockito.mock(Context::class.java)
    private val loader = CurriculumAssetLoader(context)

    @Test
    fun `loadGrade1Units returns exactly 10 units for PRIMARY grade band`() {
        val units = loader.loadGrade1Units()
        assertEquals(10, units.size)
        units.forEachIndexed { index, unit ->
            assertEquals(GradeBand.PRIMARY, unit.gradeBand)
            assertEquals(index + 1, unit.weekNumber)
            assertEquals("PRIMARY_W%02d".format(index + 1), unit.id)
            assertTrue(unit.isAvailableOffline)
            assertNotNull(unit.theme)
            assertNotNull(unit.themeEmoji)
            assertEquals("2026.08.1", unit.contentVersion)
        }
    }

    @Test
    fun `all 10 units have at least 8 target vocabulary items with CEFR A1`() {
        val units = loader.loadGrade1Units()
        units.forEach { unit ->
            assertTrue(unit.vocabItems.size >= 8, "Unit ${unit.id} should have at least 8 vocab items")
            unit.vocabItems.forEach { item ->
                assertTrue(item.id.startsWith(unit.id), "Vocab ID ${item.id} should start with unit ID ${unit.id}")
                assertTrue(item.word.isNotBlank())
                assertTrue(item.ipa.startsWith("/"), "IPA for ${item.word} should be enclosed in slashes")
                assertEquals("A1", item.cefrLevel)
                assertTrue(item.exampleSentence.contains(item.word, ignoreCase = true),
                    "Example '${item.exampleSentence}' should contain word '${item.word}'")
                assertTrue(item.chineseHint.isNotBlank())
            }
        }
    }

    @Test
    fun `all 10 units have 3 sentence structures with patterns and examples`() {
        val units = loader.loadGrade1Units()
        units.forEach { unit ->
            assertEquals(3, unit.sentenceStructures.size, "Unit ${unit.id} must have exactly 3 sentence structures")
            unit.sentenceStructures.forEach { sentence ->
                assertTrue(sentence.pattern.contains("_____"), "Pattern should have blank placeholder: ${sentence.pattern}")
                assertTrue(sentence.example.isNotBlank())
                assertTrue(sentence.chineseHint.isNotBlank())
            }
        }
    }

    @Test
    fun `all 10 units have dialogue lines with Lingo Fox participation`() {
        val units = loader.loadGrade1Units()
        units.forEach { unit ->
            assertTrue(unit.dialogueLines.size >= 6, "Unit ${unit.id} dialogue should have at least 6 lines")
            assertTrue(unit.dialogueLines.any { it.speaker == "Lingo" }, "Unit ${unit.id} must feature Lingo Fox")
            unit.dialogueLines.forEach { line ->
                assertTrue(line.text.isNotBlank())
            }
        }
    }

    @Test
    fun `all 10 units have 5 pre-authored quiz items with target words`() {
        val units = loader.loadGrade1Units()
        units.forEach { unit ->
            assertEquals(5, unit.quizItems.size, "Unit ${unit.id} should have 5 quiz items")
            unit.quizItems.forEach { quiz ->
                assertTrue(quiz.questionText.isNotBlank())
                assertTrue(quiz.targetWord.isNotBlank())
            }
        }
    }
}
