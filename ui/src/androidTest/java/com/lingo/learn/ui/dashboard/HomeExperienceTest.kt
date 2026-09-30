package org.akj.lingo.learn.ui.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeExperienceTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeShowsCurrentLearnerProgressAndLessonAndRoutesActions() {
        val state = DashboardUiState(
            streakDays = 6,
            todayProgress = 0.4f,
            themeName = "School Life",
            taskDuration = "15 Mins",
            taskTarget = "5 Words + 2 Speech",
            totalXp = 125,
            level = 3,
            hasPlan = true
        )

        var startedLesson = false
        var openedPlan = false
        var openedPractice = false
        var openedProgress = false
        composeRule.setContent {
            MaterialTheme {
                DashboardScreen(
                    grade = "Grade 5",
                    onStartLearning = { startedLesson = true },
                    onPlanClick = { openedPlan = true },
                    onErrorBookClick = {},
                    onRoleplayClick = { openedPractice = true },
                    onReportCardClick = { openedProgress = true },
                    stateOverride = state
                )
            }
        }

        composeRule.onNodeWithText("Grade 5").assertIsDisplayed()
        composeRule.onNodeWithText("School Life").assertIsDisplayed()
        composeRule.onNodeWithText("Total 125 XP").assertIsDisplayed()
        composeRule.onNodeWithText("40% complete").assertIsDisplayed()
        composeRule.onNodeWithText("Start Study").performScrollTo().performClick()
        assertEquals(true, startedLesson)

        composeRule.onNodeWithText("Weekly Plan").performScrollTo().performClick()
        assertEquals(true, openedPlan)
        composeRule.onNodeWithText("Practice").performScrollTo().performClick()
        assertEquals(true, openedPractice)
        composeRule.onNodeWithText("Progress").performScrollTo().performClick()
        assertEquals(true, openedProgress)
    }
}
