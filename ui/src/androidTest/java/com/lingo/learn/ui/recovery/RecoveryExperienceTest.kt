package org.akj.lingo.learn.ui.recovery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.akj.lingo.learn.ui.learning.CheckpointResumeDialog
import org.akj.lingo.learn.ui.learning.OfflineModeBanner
import org.akj.lingo.learn.ui.learning.ReadAlongFailurePanel
import org.akj.lingo.learn.ui.weeklyplan.WeeklyPlanScreen
import org.akj.lingo.learn.ui.weeklyplan.WeeklyPlanUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecoveryExperienceTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun offlineLessonShowsExistingLocalCapabilityDisclosure() {
        composeRule.setContent {
            MaterialTheme { OfflineModeBanner() }
        }

        composeRule.onNodeWithText("Offline mode: using device voice & local scoring.", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Configure an AI model in Settings for full features.", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun readAloudFailureKeepsRecordingRetryAndSkipActions() {
        var starts = 0
        var stops = 0
        var skips = 0
        composeRule.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize()) {
                    ReadAlongFailurePanel(
                        errorMessage = "Speech service unavailable",
                        isRecording = false,
                        onStartRecording = { starts++ },
                        onStopRecording = { stops++ },
                        onSkip = { skips++ }
                    )
                }
            }
        }

        composeRule.onNodeWithText("Speech service unavailable", substring = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Start recording").performTouchInput {
            down(center)
            up()
        }
        composeRule.onNodeWithText("Skip this one").performClick()
        assertEquals(1, starts)
        assertEquals(1, stops)
        assertEquals(1, skips)
    }

    @Test
    fun restoredCheckpointOffersContinueAndStartOverActions() {
        var choice = ""
        composeRule.setContent {
            MaterialTheme {
                CheckpointResumeDialog(
                    onContinue = { choice = "continue" },
                    onStartOver = { choice = "start-over" }
                )
            }
        }

        composeRule.onNodeWithText("Continue").assertIsDisplayed().performClick()
        assertEquals("continue", choice)
    }

    @Test
    fun weeklyPlanLoadFailureRetriesTheExistingLoadAction() {
        var retries = 0
        composeRule.setContent {
            MaterialTheme {
                WeeklyPlanScreen(
                    grade = "Grade 5", onBack = {}, onViewReport = {}, onStartLearning = {},
                    stateOverride = WeeklyPlanUiState(loadError = "Plan could not be loaded"),
                    onRetryOverride = { retries++ }
                )
            }
        }

        composeRule.onNodeWithText("Plan could not be loaded").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }
}
