package org.akj.lingo.learn

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.akj.lingo.learn.data.prefs.SecureConfigPrefs
import org.akj.lingo.learn.domain.model.ErrorBookEntry
import org.akj.lingo.learn.ui.dashboard.DashboardScreen
import org.akj.lingo.learn.ui.errorbook.ErrorBookDetailScreen
import org.akj.lingo.learn.ui.errorbook.ErrorBookScreen
import org.akj.lingo.learn.ui.learning.LearningContainer
import org.akj.lingo.learn.ui.onboarding.OnboardingContainer
import org.akj.lingo.learn.ui.weeklyplan.WeeklyPlanScreen
import org.akj.lingo.learn.ui.weeklyplan.WeeklyReportScreen
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefs: SecureConfigPrefs

    override fun attachBaseContext(base: Context) {
        val langPrefs = base.getSharedPreferences("lingo_lang_prefs", Context.MODE_PRIVATE)
        val lang = langPrefs.getString("app_language", "en") ?: "en"
        val locale = when (lang) { "zh" -> Locale.CHINESE else -> Locale.ENGLISH }
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(base.createConfigurationContext(config))
    }

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val appPrefs = remember {
                        applicationContext.getSharedPreferences("lingo_app_prefs", Context.MODE_PRIVATE)
                    }
                    var isOnboardingCompleted by remember { mutableStateOf(appPrefs.getBoolean("onboarding_completed", false)) }
                    var currentGrade by remember { mutableStateOf(appPrefs.getString("grade", "Grade 4") ?: "Grade 4") }
                    var isLearning by remember { mutableStateOf(false) }
                    var isSettingsOpen by remember { mutableStateOf(false) }
                    var isErrorBookOpen by remember { mutableStateOf(false) }
                    var isErrorBookDetailOpen by remember { mutableStateOf(false) }
                    var selectedErrorEntry by remember { mutableStateOf<ErrorBookEntry?>(null) }
                    var isWeeklyPlanOpen by remember { mutableStateOf(false) }
                    var isWeeklyReportOpen by remember { mutableStateOf(false) }

                    when {
                        isSettingsOpen -> org.akj.lingo.learn.ui.settings.SettingsScreen(
                            onBack = { isSettingsOpen = false }
                        )
                        isErrorBookDetailOpen && selectedErrorEntry != null -> ErrorBookDetailScreen(
                            entry = selectedErrorEntry!!,
                            onBack = {
                                isErrorBookDetailOpen = false
                                isErrorBookOpen = true
                            }
                        )
                        isErrorBookOpen -> ErrorBookScreen(
                            onBack = { isErrorBookOpen = false },
                            onEntryClick = { entry ->
                                selectedErrorEntry = entry
                                isErrorBookDetailOpen = true
                            }
                        )
                        isWeeklyReportOpen -> WeeklyReportScreen(
                            onBack = {
                                isWeeklyReportOpen = false
                                isWeeklyPlanOpen = true
                            }
                        )
                        isWeeklyPlanOpen -> WeeklyPlanScreen(
                            onBack = { isWeeklyPlanOpen = false },
                            onViewReport = {
                                isWeeklyPlanOpen = false
                                isWeeklyReportOpen = true
                            },
                            onStartLearning = {
                                isWeeklyPlanOpen = false
                                isLearning = true
                            }
                        )
                        else -> AnimatedContent(
                            targetState = isLearning,
                            transitionSpec = {
                                slideInHorizontally(animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)) { width -> width } + fadeIn() togetherWith
                                        slideOutHorizontally(animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)) { width -> -width } + fadeOut()
                            },
                            label = "MainNav"
                        ) { learning ->
                            if (!isOnboardingCompleted) {
                                OnboardingContainer(
                                    onFinished = { grade, textbook, level ->
                                        currentGrade = grade
                                        isOnboardingCompleted = true
                                        isLearning = true
                                        appPrefs.edit()
                                            .putBoolean("onboarding_completed", true)
                                            .putString("grade", grade)
                                            .putString("textbook", textbook)
                                            .apply()
                                    },
                                    onOpenSettings = { isSettingsOpen = true }
                                )
                            } else if (!learning) {
                                DashboardScreen(
                                    grade = currentGrade,
                                    onStartLearning = { isLearning = true },
                                    onPlanClick = { isWeeklyPlanOpen = true },
                                    onErrorBookClick = { isErrorBookOpen = true },
                                    onSettingsClick = { isSettingsOpen = true }
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
}
