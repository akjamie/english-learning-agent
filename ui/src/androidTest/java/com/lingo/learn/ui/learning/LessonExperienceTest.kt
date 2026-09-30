package org.akj.lingo.learn.ui.learning

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.akj.lingo.learn.domain.model.LearningSession
import org.akj.lingo.learn.domain.model.QuizQuestion
import org.akj.lingo.learn.domain.model.QuizQuestionType
import org.akj.lingo.learn.domain.model.PronunciationResult
import org.akj.lingo.learn.domain.model.WordScore
import org.akj.lingo.learn.ui.dashboard.getThemeForGrade
import org.akj.lingo.learn.ui.components.WordHighlightText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LessonExperienceTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun quizAnswerShowsFeedbackAndNextAdvancesToTheNextQuestion() {
        val session = LearningSession(
            theme = "School trip",
            subtitleLines = emptyList(),
            readAlongSentences = emptyList(),
            gameQuestions = emptyList(),
            quizQuestions = listOf(
                QuizQuestion(
                    id = 1,
                    type = QuizQuestionType.IMAGE_CHOOSE_WORD,
                    question = "Which sentence tells us the trip already happened?",
                    options = listOf("We visit the museum.", "We visited the museum.", "We are visit the museum."),
                    correctIndex = 1
                ),
                QuizQuestion(
                    id = 2,
                    type = QuizQuestionType.IMAGE_CHOOSE_WORD,
                    question = "What did Maya see at the museum?",
                    options = listOf("A dinosaur", "A kite"),
                    correctIndex = 0
                )
            ),
            targetNewWords = listOf("museum")
        )
        var screenState by mutableStateOf(
            QuizScreenState(session, QuizState(), ReadAlongState())
        )
        val actions = QuizScreenActions(
            onSelectAnswer = { selected ->
                val question = screenState.session.quizQuestions[screenState.quizState.currentIndex]
                screenState = screenState.copy(
                    quizState = screenState.quizState.copy(lastAnswerCorrect = selected == question.correctIndex)
                )
            },
            onNextQuestion = {
                screenState = screenState.copy(
                    quizState = screenState.quizState.copy(
                        currentIndex = screenState.quizState.currentIndex + 1,
                        lastAnswerCorrect = null
                    )
                )
            }
        )

        composeRule.setContent {
            MaterialTheme {
                QuizScreen(
                    viewModel = null,
                    theme = getThemeForGrade("Grade 6"),
                    onComplete = {},
                    stateOverride = screenState,
                    actionsOverride = actions
                )
            }
        }

        composeRule.onNodeWithText("Which sentence tells us the trip already happened?").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Answer: We visited the museum.").performClick()
        composeRule.onNodeWithContentDescription("Correct answer: We visited the museum.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Incorrect answer: We visit the museum.").assertIsDisplayed()
        composeRule.onNodeWithText("Next ->").performClick()
        composeRule.onNodeWithText("What did Maya see at the museum?").assertIsDisplayed()
    }

    @Test
    fun newWordAnswerShowsFeedbackAndNextWordAdvancesTheWarmup() {
        val session = LearningSession(
            theme = "School trip",
            subtitleLines = emptyList(),
            readAlongSentences = emptyList(),
            gameQuestions = emptyList(),
            quizQuestions = emptyList(),
            targetNewWords = listOf("morning", "welcome")
        )
        composeRule.setContent {
            MaterialTheme {
                PreTeachScreen(
                    theme = getThemeForGrade("Grade 6"),
                    onProceed = {},
                    sessionOverride = session,
                    onSpeakWord = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Choose word: morning").performClick()
        composeRule.onNodeWithText("Nice work! “morning” unlocked.").assertIsDisplayed()
        composeRule.onNodeWithText("Next Word").performClick()
        composeRule.onNodeWithContentDescription("Choose word: welcome").assertIsDisplayed()
    }

    @Test
    fun readAloudResultShowsScoreWordFeedbackAndContinueAction() {
        val result = PronunciationResult(
            overallScore = 86,
            wordScores = listOf(WordScore("The", 90), WordScore("museum", 86), WordScore("enormous", 52)),
            feedback = "Great rhythm! Try the ‘or’ sound once more.",
            isFromFallback = false
        )
        var continued = false
        composeRule.setContent {
            MaterialTheme {
                androidx.compose.foundation.layout.Column {
                    WordHighlightText(
                        text = "The museum was enormous.",
                        wordScores = result.wordScores
                    )
                    ReadAlongResultPanel(
                        result = result,
                        isPlayingSelf = false,
                        onCompare = {},
                        onRecordAgain = {},
                        onContinue = { continued = true }
                    )
                }
            }
        }

        composeRule.onNodeWithText("Score: 86").assertIsDisplayed()
        composeRule.onNodeWithText("The museum was enormous.").assertIsDisplayed()
        composeRule.onNodeWithText("Great rhythm! Try the ‘or’ sound once more.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Continue").performClick()
        assert(continued)
    }
}
