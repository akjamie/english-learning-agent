package org.akj.lingo.learn.ui.errorbook

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.akj.lingo.learn.domain.model.ErrorBookEntry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VocabularyExperienceTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val savedWord = ErrorBookEntry(
        id = "entry-apple",
        vocabId = "apple",
        errorCount = 2,
        errorType = "QUIZ_WRONG_ANSWER",
        lastErrorTimestamp = System.currentTimeMillis(),
        questionType = "MULTIPLE_CHOICE",
        priorityScore = 7.5f,
        status = "TO_REVIEW",
        consecutiveCorrectCount = 0,
        graduationCheckTimestamp = 0L,
        historyJson = "[]"
    )

    @Test
    fun openingSavedWordShowsExplanationAndHandsEntryToDetail() {
        var openedEntry: ErrorBookEntry? = null
        composeRule.setContent {
            MaterialTheme {
                ErrorBookScreen(
                    onBack = {},
                    onEntryClick = { openedEntry = it },
                    stateOverride = ErrorBookUiState(
                        entries = listOf(savedWord),
                        totalCount = 1,
                        reviewCount = 1
                    )
                )
            }
        }

        composeRule.onNodeWithContentDescription("apple, error type QUIZ_WRONG_ANSWER")
            .assertIsDisplayed()
            .performClick()

        assertEquals(savedWord, openedEntry)
        composeRule.onNodeWithText("Lingo's Tip for 'apple'").assertIsDisplayed()
    }

    @Test
    fun practiceAgainUsesTheExistingRetryActionForTheSavedWord() {
        var retriedWord: String? = null
        composeRule.setContent {
            MaterialTheme {
                ErrorBookScreen(
                    onBack = {},
                    onEntryClick = {},
                    stateOverride = ErrorBookUiState(
                        entries = listOf(savedWord),
                        totalCount = 1,
                        reviewCount = 1
                    ),
                    onRetryWord = { retriedWord = it }
                )
            }
        }

        composeRule.onNodeWithText("Practice Again", substring = true)
            .performScrollTo()
            .performClick()

        assertEquals("apple", retriedWord)
    }

    @Test
    fun wordDetailShowsHistoryAndExplanationWithoutChangingTheEntry() {
        composeRule.setContent {
            MaterialTheme {
                ErrorBookDetailScreen(
                    entry = savedWord,
                    onBack = {},
                    explanationOverride = "An apple is a crunchy fruit."
                )
            }
        }

        composeRule.onNodeWithText("Error History", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Error Count").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Word detail card").performClick()
        composeRule.onNodeWithText("An apple is a crunchy fruit.").assertIsDisplayed()
    }
}
