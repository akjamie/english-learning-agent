package org.akj.lingo.learn.ui.progress

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.akj.lingo.learn.ui.weeklyplan.PlanDayItem
import org.akj.lingo.learn.ui.weeklyplan.WeeklyPlanScreen
import org.akj.lingo.learn.ui.weeklyplan.WeeklyPlanUiState
import org.akj.lingo.learn.ui.reportcard.ReportCardScreen
import org.akj.lingo.learn.ui.reportcard.ReportCardUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlanAndProgressExperienceTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun savedPlanShowsCompletionAndSelectingDayContinuesThatDay() {
        val savedPlan = WeeklyPlanUiState(
            theme = "School Trip",
            difficultyCoefficient = 1.2f,
            days = listOf(
                PlanDayItem(
                    day = 1,
                    focus = "Museum words",
                    targetWords = listOf("museum", "enormous"),
                    referenceSentence = "The museum was enormous.",
                    durationMinutes = 15,
                    isCompleted = true
                ),
                PlanDayItem(
                    day = 2,
                    focus = "Past tense",
                    targetWords = listOf("visited", "learned"),
                    referenceSentence = "We visited the museum.",
                    durationMinutes = 15
                )
            )
        )
        var startedDay: Int? = null
        composeRule.setContent {
            MaterialTheme {
                WeeklyPlanScreen(
                    grade = "Grade 6",
                    onBack = {},
                    onViewReport = {},
                    onStartLearning = { startedDay = it },
                    stateOverride = savedPlan,
                    todayOverride = 2,
                    formattedDateRangeOverride = "Sep 20 – Sep 26"
                )
            }
        }

        composeRule.onNodeWithText("School Trip", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Day 1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Review").performScrollTo().performClick()
        assertEquals(1, startedDay)
        composeRule.onNodeWithText("Day 2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Start Today").performScrollTo().performClick()
        assertEquals(2, startedDay)
    }

    @Test
    fun progressShowsCurrentHistorySkillsAndErrorBookSummary() {
        var exported = false
        composeRule.setContent {
            MaterialTheme {
                ReportCardScreen(
                    onBack = {},
                    stateOverride = ReportCardUiState(
                        childName = "Maya",
                        grade = "Grade 6",
                        cefrLabel = "A2",
                        streakDays = 7,
                        totalSessions = 18,
                        weeklyAccuracy = 0.68f,
                        totalWordsLearned = 42,
                        learningMinutes = 275,
                        skillAccuracyPercent = mapOf("QUIZ" to 68, "SPEAKING" to 64),
                        masteredWords = listOf("museum", "enormous"),
                        weakWords = listOf("visited", "afternoon"),
                        errorBookCount = 6,
                        grammarErrorCount = 3,
                        encouragement = "Every step counts."
                    ),
                    onShareOverride = { exported = true }
                )
            }
        }

        composeRule.onNodeWithText("Maya · Grade 6").assertIsDisplayed()
        composeRule.onNodeWithText("4h 35m").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Quiz 68%").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Speaking 64%").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("My Error Book", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("museum · enormous").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Export PNG & Share", substring = true).performScrollTo().performClick()
        assertEquals(true, exported)
    }
}
