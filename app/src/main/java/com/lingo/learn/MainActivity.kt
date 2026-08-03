package org.akj.lingo.learn

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import org.akj.lingo.learn.data.prefs.SecureConfigPrefs
import org.akj.lingo.learn.domain.model.ErrorBookEntry
import org.akj.lingo.learn.ui.dashboard.DashboardScreen
import org.akj.lingo.learn.ui.aigrowthnotes.AiGrowthNotesScreen
import org.akj.lingo.learn.ui.errorbook.ErrorBookDetailScreen
import org.akj.lingo.learn.ui.errorbook.ErrorBookScreen
import org.akj.lingo.learn.ui.learning.LearningContainer
import org.akj.lingo.learn.ui.onboarding.DiagnosisResultScreen
import org.akj.lingo.learn.ui.onboarding.DiagnosisScreen
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

    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Sprint 14: refresh the widget on app open so it picks up
                    // the latest pre-generated content from WidgetContentCache.
                    LaunchedEffect(Unit) {
                        LingoStreakWidget.refreshAll(applicationContext)
                    }

                    val appPrefs = remember {
                        applicationContext.getSharedPreferences("lingo_app_prefs", Context.MODE_PRIVATE)
                    }
                    var isOnboardingCompleted by remember { mutableStateOf(appPrefs.getBoolean("onboarding_completed", false)) }
                    var currentGrade by remember { mutableStateOf(appPrefs.getString("grade", "Grade 4") ?: "Grade 4") }
                    var isLearning by remember { mutableStateOf(false) }
                    // Sprint 10.5: which plan day to load when starting learning (1-7, default = today)
                    var learningDayIndex by remember { mutableStateOf(1) }
                    // Sprint 12: re-run the diagnostic to recalibrate level
                    var isRediagnosing by remember { mutableStateOf(false) }

                    // Navigation States
                    var currentTab by remember { mutableStateOf(MainTab.HOME) }
                    
                    // Sub-screen States
                    var isErrorBookDetailOpen by remember { mutableStateOf(false) }
                    var selectedErrorEntry by remember { mutableStateOf<ErrorBookEntry?>(null) }
                    var isWeeklyReportOpen by remember { mutableStateOf(false) }
                    var isRoleplayOpen by remember { mutableStateOf(false) }
                    var isAiGrowthNotesOpen by remember { mutableStateOf(false) }

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
                                    // Sprint 10.5: persist diagnostic level so it can drive
                                    // within-band difficulty tuning (A=beginner, C=advanced)
                                    .putString("diagnostic_level", level)
                                    .apply()
                            },
                            onOpenSettings = { currentTab = MainTab.SETTINGS }
                        )
                    } else if (isRediagnosing) {
                        // Sprint 12 + 14: recalibrate the child's level by re-running the
                        // diagnosis, then show the result screen before exiting.
                        var rediagnosisResult by remember { mutableStateOf<String?>(null) }
                        if (rediagnosisResult != null) {
                            DiagnosisResultScreen(
                                level = rediagnosisResult!!,
                                onStartLearning = {
                                    appPrefs.edit().putString("diagnostic_level", rediagnosisResult).apply()
                                    isRediagnosing = false
                                }
                            )
                        } else {
                            DiagnosisScreen(
                                grade = currentGrade,
                                onDiagnosisFinished = { level ->
                                    rediagnosisResult = level
                                }
                            )
                        }
                    } else if (isLearning) {
                        LearningContainer(
                            grade = currentGrade,
                            dayIndex = learningDayIndex,
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
                    } else if (isAiGrowthNotesOpen) {
                        AiGrowthNotesScreen(
                            onBack = { isAiGrowthNotesOpen = false }
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
                                when (currentTab) {
                                    MainTab.HOME -> DashboardScreen(
                                        grade = currentGrade,
                                        onStartLearning = {
                                            learningDayIndex = 1
                                            isLearning = true
                                        },
                                        onPlanClick = { currentTab = MainTab.PLAN },
                                        onErrorBookClick = { currentTab = MainTab.ERROR_BOOK },
                                        onSettingsClick = { currentTab = MainTab.SETTINGS },
                                        onRoleplayClick = { isRoleplayOpen = true },
                                        onUpdateLevel = { isRediagnosing = true }
                                    )
                                    MainTab.PLAN -> WeeklyPlanScreen(
                                        grade = currentGrade,
                                        onBack = { currentTab = MainTab.HOME },
                                        onViewReport = { isWeeklyReportOpen = true },
                                        onStartLearning = { dayIndex ->
                                            learningDayIndex = dayIndex.coerceIn(1, 7)
                                            isLearning = true
                                        }
                                    )
                                    MainTab.ERROR_BOOK -> ErrorBookScreen(
                                        onBack = { currentTab = MainTab.HOME },
                                        onEntryClick = { entry ->
                                            selectedErrorEntry = entry
                                            isErrorBookDetailOpen = true
                                        }
                                    )
                                    MainTab.SETTINGS -> org.akj.lingo.learn.ui.settings.SettingsScreen(
                                        onBack = { currentTab = MainTab.HOME },
                                        onOpenAiGrowthNotes = { isAiGrowthNotesOpen = true },
                                        onRerunOnboarding = {
                                            appPrefs.edit().putBoolean("onboarding_completed", false).apply()
                                            isOnboardingCompleted = false
                                            currentGrade = "Grade 4"
                                            currentTab = MainTab.HOME
                                        }
                                    )
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
