package org.akj.lingo.learn.ui.roleplay

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.akj.lingo.learn.domain.model.ChatMessage
import org.akj.lingo.learn.domain.model.RoleplayScenario
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoleplayExperienceTest {
    @get:Rule val composeRule = createComposeRule()

    private val zoo = RoleplayScenario(
        id = "zoo", title = "Zoo Visit", emoji = "🦁", description = "Visit the zoo",
        targetWords = listOf("lion"), systemPrompt = "Practice a zoo visit",
        openingLine = "What animal do you see?"
    )

    @Test
    fun typedResponseAppearsInConversationAndContinuesScenario() {
        var sent: String? = null
        composeRule.setContent {
            MaterialTheme {
                RoleplayScreen(
                    onNavigateBack = {},
                    stateOverride = RoleplayScreenState(
                        messages = listOf(ChatMessage("assistant", zoo.openingLine)),
                        scenarios = listOf(zoo), scenario = zoo
                    ),
                    onSendMessage = { sent = it }
                )
            }
        }

        composeRule.onNodeWithText("What animal do you see?").assertIsDisplayed()
        composeRule.onNodeWithText("Type a message…").performTextInput("I see a lion")
        composeRule.onNodeWithContentDescription("Send message").performClick()
        composeRule.onNodeWithText("I see a lion").assertIsDisplayed()
        composeRule.onNodeWithText("What animal do you see?").assertIsDisplayed()
        assertEquals("I see a lion", sent)
    }

    @Test
    fun spokenResponseUsesExistingRecordStartAndSubmitActions() {
        var started = 0
        var submitted = 0
        composeRule.setContent {
            MaterialTheme {
                RoleplayScreen(
                    onNavigateBack = {},
                    stateOverride = RoleplayScreenState(
                        messages = listOf(ChatMessage("assistant", zoo.openingLine)),
                        scenarios = listOf(zoo), scenario = zoo
                    ),
                    onStartRecording = { started++ },
                    onStopRecording = { submitted++ }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Record voice response").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Record voice response").performTouchInput {
            down(center)
            up()
        }
        assertEquals(1, started)
        assertEquals(1, submitted)
    }
}
