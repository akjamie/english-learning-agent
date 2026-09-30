package org.akj.lingo.learn.ui.infrastructure

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeSemanticsSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun buttonIsVisibleAndUpdatesAfterClick() {
        val clicked = mutableStateOf(false)

        composeRule.setContent {
            MaterialTheme {
                Button(onClick = { clicked.value = true }) {
                    Text(if (clicked.value) "Started" else "Start")
                }
            }
        }

        composeRule.onNodeWithText("Start").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Started").assertIsDisplayed()
    }
}
