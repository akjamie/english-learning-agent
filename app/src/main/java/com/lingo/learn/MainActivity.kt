package org.akj.lingo.learn

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.tween
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import org.akj.lingo.learn.data.prefs.SecureConfigPrefs
import org.akj.lingo.learn.domain.model.ErrorBookEntry
import org.akj.lingo.learn.ui.dashboard.DashboardScreen
import org.akj.lingo.learn.ui.errorbook.ErrorBookDetailScreen
import org.akj.lingo.learn.ui.errorbook.ErrorBookScreen
import org.akj.lingo.learn.ui.learning.LearningContainer
import org.akj.lingo.learn.ui.onboarding.OnboardingContainer
import org.akj.lingo.learn.ui.roleplay.RoleplayScreen
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

                    // Navigation States
                    var currentTab by remember { mutableStateOf(MainTab.HOME) }
                    
                    // Sub-screen States
                    var isErrorBookDetailOpen by remember { mutableStateOf(false) }
                    var selectedErrorEntry by remember { mutableStateOf<ErrorBookEntry?>(null) }
                    var isWeeklyReportOpen by remember { mutableStateOf(false) }
                    var isRoleplayOpen by remember { mutableStateOf(false) }

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
                            onOpenSettings = { currentTab = MainTab.SETTINGS }
                        )
                    } else if (isLearning) {
                        LearningContainer(
                            grade = currentGrade,
                            onExit = { isLearning = false }
                        )
                    } else if (isErrorBookDetailOpen && selectedErrorEntry != null) {
                        ErrorBookDetailScreen(
                            entry = selectedErrorEntry!!,
                            onBack = { isErrorBookDetailOpen = false }
                        )
                    } else if (isWeeklyReportOpen) {
                        WeeklyReportScreen(
                            onBack = { isWeeklyReportOpen = false }
                        )
                    } else if (isRoleplayOpen) {
                        RoleplayScreen(
                            onNavigateBack = { isRoleplayOpen = false }
                        )
                    } else {
                        // Main Scaffold with Bottom Navigation
                        androidx.compose.material3.Scaffold(
                            bottomBar = {
                                NavigationBar(
                                    containerColor = androidx.compose.ui.graphics.Color(0xFFFFFDF5)
                                ) {
                                    val tabs = listOf(MainTab.HOME, MainTab.PLAN, MainTab.ERROR_BOOK, MainTab.SETTINGS)
                                    val icons = listOf("🏠", "📅", "📖", "⚙️")
                                    val labels = listOf("Home", "Plan", "Error Book", "Settings")
                                    
                                    tabs.forEachIndexed { index, tab ->
                                        NavigationBarItem(
                                            selected = currentTab == tab,
                                            onClick = { currentTab = tab },
                                            icon = { Text(icons[index], fontSize = 24.sp) },
                                            label = { Text(labels[index]) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = androidx.compose.ui.graphics.Color(0xFF5C6FF2),
                                                selectedTextColor = androidx.compose.ui.graphics.Color(0xFF5C6FF2),
                                                indicatorColor = androidx.compose.ui.graphics.Color(0xFF5C6FF2).copy(alpha = 0.1f)
                                            )
                                        )
                                    }
                                }
                            }
                        ) { paddingValues ->
                            Box(modifier = Modifier.padding(paddingValues)) {
                                AnimatedContent(
                                    targetState = currentTab,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                                    },
                                    label = "TabNav"
                                ) { tab ->
                                    when (tab) {
                                        MainTab.HOME -> DashboardScreen(
                                            grade = currentGrade,
                                            onStartLearning = { isLearning = true },
                                            onPlanClick = { currentTab = MainTab.PLAN },
                                            onErrorBookClick = { currentTab = MainTab.ERROR_BOOK },
                                            onSettingsClick = { currentTab = MainTab.SETTINGS },
                                            onRoleplayClick = { isRoleplayOpen = true }
                                        )
                                        MainTab.PLAN -> WeeklyPlanScreen(
                                            grade = currentGrade,
                                            onBack = { currentTab = MainTab.HOME },
                                            onViewReport = { isWeeklyReportOpen = true },
                                            onStartLearning = { isLearning = true }
                                        )
                                        MainTab.ERROR_BOOK -> ErrorBookScreen(
                                            onBack = { currentTab = MainTab.HOME },
                                            onEntryClick = { entry ->
                                                selectedErrorEntry = entry
                                                isErrorBookDetailOpen = true
                                            }
                                        )
                                        MainTab.SETTINGS -> org.akj.lingo.learn.ui.settings.SettingsScreen(
                                            onBack = { currentTab = MainTab.HOME }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    enum class MainTab { HOME, PLAN, ERROR_BOOK, SETTINGS }
}
