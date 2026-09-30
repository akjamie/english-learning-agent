package org.akj.lingo.learn.ui.onboarding

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalAnimationApi::class)
@RunWith(AndroidJUnit4::class)
class WelcomeExperienceTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun prototypeWelcomeCopyStartsTheExistingOnboardingFlow() {
        composeRule.setContent {
            MaterialTheme {
                OnboardingContainer(onFinished = { _, _, _ -> })
            }
        }

        composeRule.onNodeWithText("Your English Adventure").assertIsDisplayed()
        composeRule.onNodeWithText("lingo. For curious minds").assertIsDisplayed()
        composeRule.onNodeWithText("Small steps. Big English.").assertIsDisplayed()
        composeRule.onNodeWithText("Start my journey").performClick()
        composeRule.onNodeWithText("Select Your Grade").assertIsDisplayed()
    }

    @Test
    fun settingsActionRemainsAvailableFromWelcome() {
        var settingsOpened = false
        composeRule.setContent {
            MaterialTheme {
                WelcomeScreen(
                    onStartClick = {},
                    onOpenSettings = { settingsOpened = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Settings")
            .assertIsDisplayed()
            .performClick()

        assertTrue(settingsOpened)
    }
}
