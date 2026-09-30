package org.akj.lingo.learn.ui.regression

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.akj.lingo.learn.ui.dashboard.DashboardScreen
import org.akj.lingo.learn.ui.dashboard.DashboardUiState
import org.akj.lingo.learn.ui.onboarding.WelcomeScreen
import org.akj.lingo.learn.domain.model.ChatMessage
import org.akj.lingo.learn.domain.model.ErrorBookEntry
import org.akj.lingo.learn.domain.model.LearningSession
import org.akj.lingo.learn.domain.model.QuizQuestion
import org.akj.lingo.learn.domain.model.QuizQuestionType
import org.akj.lingo.learn.ui.errorbook.ErrorBookScreen
import org.akj.lingo.learn.ui.errorbook.ErrorBookUiState
import org.akj.lingo.learn.ui.learning.QuizScreen
import org.akj.lingo.learn.ui.learning.QuizScreenState
import org.akj.lingo.learn.ui.learning.QuizState
import org.akj.lingo.learn.ui.learning.ReadAlongState
import org.akj.lingo.learn.ui.learning.CheckpointResumeDialog
import org.akj.lingo.learn.ui.learning.OfflineModeBanner
import org.akj.lingo.learn.ui.learning.ReadAlongFailurePanel
import org.akj.lingo.learn.ui.roleplay.RoleplayScreen
import org.akj.lingo.learn.ui.roleplay.RoleplayScreenState
import org.akj.lingo.learn.ui.reportcard.ReportCardScreen
import org.akj.lingo.learn.ui.reportcard.ReportCardUiState
import org.akj.lingo.learn.ui.dashboard.getThemeForGrade
import org.akj.lingo.learn.ui.weeklyplan.PlanDayItem
import org.akj.lingo.learn.ui.weeklyplan.WeeklyPlanScreen
import org.akj.lingo.learn.ui.weeklyplan.WeeklyPlanUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HuaweiMate80RegressionTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun welcomePrimaryActionFitsTheMate80Viewport() {
        var started = false
        composeRule.setContent {
            MaterialTheme { WelcomeScreen(onStartClick = { started = true }, onOpenSettings = {}) }
        }

        composeRule.onNodeWithText("Start my journey").assertWithinRoot().performClick()
        assertTrue(started)
    }

    @Test
    fun homePrimaryAndDestinationActionsFitAfterScrollingIntoView() {
        var started = false
        var openedPlan = false
        composeRule.setContent {
            MaterialTheme {
                DashboardScreen(
                    grade = "Grade 5", onStartLearning = { started = true },
                    onPlanClick = { openedPlan = true }, onErrorBookClick = {},
                    onRoleplayClick = {}, onReportCardClick = {},
                    stateOverride = DashboardUiState(themeName = "School Life", hasPlan = true)
                )
            }
        }

        composeRule.onNodeWithText("Start Study").performScrollTo().assertWithinRoot().performClick()
        assertTrue(started)
        composeRule.onNodeWithText("Weekly Plan").performScrollTo().assertWithinRoot().performClick()
        assertTrue(openedPlan)
    }

    @Test
    fun savedPlanActionFitsAfterScrollingIntoView() {
        var startedDay = 0
        composeRule.setContent {
            MaterialTheme {
                WeeklyPlanScreen(
                    grade = "Grade 5", onBack = {}, onViewReport = {},
                    onStartLearning = { startedDay = it },
                    stateOverride = WeeklyPlanUiState(days = listOf(
                        PlanDayItem(day = 1, focus = "Vocabulary", targetWords = listOf("museum"),
                            referenceSentence = "Visit the museum.", durationMinutes = 15)
                    )),
                    todayOverride = 1, formattedDateRangeOverride = "This week"
                )
            }
        }

        composeRule.onNodeWithText("Start Today").performScrollTo().assertWithinRoot().performClick()
        assertTrue(startedDay == 1)
    }

    @Test
    fun vocabularyRetryActionFitsAfterScrollingIntoView() {
        val entry = ErrorBookEntry(
            id = "entry-lion", vocabId = "lion", errorCount = 1,
            errorType = "QUIZ_WRONG_ANSWER", lastErrorTimestamp = 1L,
            questionType = "MULTIPLE_CHOICE", priorityScore = 1f,
            status = "TO_REVIEW", consecutiveCorrectCount = 0,
            graduationCheckTimestamp = 0L, historyJson = "[]"
        )
        var retried = false
        composeRule.setContent {
            MaterialTheme {
                ErrorBookScreen(
                    onBack = {}, onEntryClick = {},
                    stateOverride = ErrorBookUiState(entries = listOf(entry), totalCount = 1, reviewCount = 1),
                    onRetryWord = { retried = true }
                )
            }
        }

        composeRule.onNodeWithText("Practice Again", substring = true)
            .performScrollTo().assertWithinRoot().performClick()
        assertTrue(retried)
    }

    @Test
    fun roleplayTypedAndVoiceActionsFitTheMate80Viewport() {
        val scenario = org.akj.lingo.learn.domain.model.RoleplayScenario(
            id = "zoo", title = "Zoo Visit", emoji = "🦁", description = "Visit the zoo",
            targetWords = listOf("lion"), systemPrompt = "Practice a zoo visit",
            openingLine = "What animal do you see?"
        )
        var sent = false
        var recordingStarted = false
        composeRule.setContent {
            MaterialTheme {
                RoleplayScreen(
                    onNavigateBack = {},
                    stateOverride = RoleplayScreenState(
                        messages = listOf(ChatMessage("assistant", scenario.openingLine)),
                        scenarios = listOf(scenario), scenario = scenario
                    ),
                    onSendMessage = { sent = it == "A lion" },
                    onStartRecording = { recordingStarted = true }, onStopRecording = {}
                )
            }
        }

        composeRule.onNodeWithText("Type a message…").performTextInput("A lion")
        composeRule.onNodeWithContentDescription("Send message").assertWithinRoot().performClick()
        assertTrue(sent)
        composeRule.onNodeWithContentDescription("Record voice response").assertWithinRoot().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Record voice response").performTouchInput {
            down(center)
            up()
        }
        assertTrue(recordingStarted)
    }

    @Test
    fun quizAnswerAndProgressExportActionsFitAfterScrollingIntoView() {
        val session = LearningSession(
            theme = "School Life", subtitleLines = emptyList(), readAlongSentences = emptyList(),
            gameQuestions = emptyList(), targetNewWords = listOf("school"),
            quizQuestions = listOf(QuizQuestion(
                id = 1, type = QuizQuestionType.IMAGE_CHOOSE_WORD,
                question = "Where does Maya learn?", options = listOf("School", "Park"), correctIndex = 0
            ))
        )
        composeRule.setContent {
            MaterialTheme {
                QuizScreen(
                    viewModel = null, theme = getThemeForGrade("Grade 5"), onComplete = {},
                    stateOverride = QuizScreenState(session, QuizState(), ReadAlongState()),
                    actionsOverride = org.akj.lingo.learn.ui.learning.QuizScreenActions(
                        onSelectAnswer = {}, onNextQuestion = {}
                    )
                )
            }
        }
        composeRule.onNodeWithContentDescription("Answer: School").assertWithinRoot().performClick()
    }

    @Test
    fun progressExportActionFitsAfterScrollingIntoView() {
        var exported = false
        composeRule.setContent {
            MaterialTheme {
                ReportCardScreen(
                    onBack = {}, stateOverride = ReportCardUiState(childName = "Maya", grade = "Grade 5"),
                    onShareOverride = { exported = true }
                )
            }
        }
        composeRule.onNodeWithText("Export PNG & Share", substring = true)
            .performScrollTo().assertWithinRoot().performClick()
        assertTrue(exported)
    }

    @Test
    fun offlineModeDisclosureFitsTheMate80Viewport() {
        composeRule.setContent {
            MaterialTheme { OfflineModeBanner() }
        }
        composeRule.onNodeWithText("Offline mode: using device voice", substring = true)
            .assertWithinRoot().assertIsDisplayed()
    }

    @Test
    fun readAloudRecoveryActionsFitTheMate80Viewport() {
        var starts = 0
        var skips = 0
        composeRule.setContent {
            MaterialTheme {
                ReadAlongFailurePanel(
                    errorMessage = "Could not evaluate this recording",
                    isRecording = false,
                    onStartRecording = { starts++ }, onStopRecording = {}, onSkip = { skips++ }
                )
            }
        }
        composeRule.onNodeWithContentDescription("Start recording").assertWithinRoot().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Start recording").performTouchInput { down(center); up() }
        composeRule.onNodeWithText("Skip this one").assertWithinRoot().performClick()
        assertTrue(skips == 1)
        assertTrue(starts == 1)
    }

    @Test
    fun checkpointChoicesFitTheMate80Viewport() {
        var continued = false
        composeRule.setContent {
            MaterialTheme {
                CheckpointResumeDialog(onContinue = { continued = true }, onStartOver = {})
            }
        }
        composeRule.onNodeWithText("Continue").assertWithinRoot().performClick()
        assertTrue(continued)
    }

    @Test
    fun weeklyPlanRetryFitsTheMate80Viewport() {
        var retried = false
        composeRule.setContent {
            MaterialTheme {
                WeeklyPlanScreen(
                    grade = "Grade 5", onBack = {}, onViewReport = {}, onStartLearning = {},
                    stateOverride = WeeklyPlanUiState(loadError = "Plan could not be loaded"),
                    onRetryOverride = { retried = true }
                )
            }
        }
        composeRule.onNodeWithText("Retry").assertWithinRoot().performClick()
        assertTrue(retried)
    }

    private fun SemanticsNodeInteraction.assertWithinRoot(): SemanticsNodeInteraction {
        val root = composeRule.onAllNodes(isRoot()).fetchSemanticsNodes()
            .maxByOrNull { it.boundsInRoot.width * it.boundsInRoot.height }
            ?.boundsInRoot ?: error("No Compose root viewport was found")
        val action = fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Action bounds $action extend beyond root viewport $root",
            action.left >= root.left && action.top >= root.top &&
                action.right <= root.right && action.bottom <= root.bottom
        )
        return this
    }
}
