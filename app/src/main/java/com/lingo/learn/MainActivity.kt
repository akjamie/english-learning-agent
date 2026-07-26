package com.lingo.learn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.lingo.learn.data.prefs.SecureConfigPrefs
import com.lingo.learn.ui.dashboard.DashboardScreen
import com.lingo.learn.ui.learning.LearningContainer
import com.lingo.learn.ui.onboarding.OnboardingContainer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefs: SecureConfigPrefs

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isOnboardingCompleted by remember { mutableStateOf(false) }
                    var currentGrade by remember { mutableStateOf("Grade 4") }
                    var isLearning by remember { mutableStateOf(false) }

                    // Toggle between Dashboard and Learning flow
                    AnimatedContent(
                        targetState = isLearning,
                        transitionSpec = {
                            slideInHorizontally { width -> width } + fadeIn() with
                                    slideOutHorizontally { width -> -width } + fadeOut()
                        },
                        label = "MainNav"
                    ) { learning ->
                        if (!isOnboardingCompleted) {
                            OnboardingContainer(
                                onFinished = { grade, textbook, level ->
                                    currentGrade = grade
                                    isOnboardingCompleted = true
                                }
                            )
                        } else if (!learning) {
                            DashboardScreen(
                                grade = currentGrade,
                                onStartLearning = { isLearning = true },
                                onPlanClick = {},
                                onErrorBookClick = {}
                            )
                        } else {
                            LearningContainer(
                                grade = currentGrade,
                                onExit = { isLearning = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
