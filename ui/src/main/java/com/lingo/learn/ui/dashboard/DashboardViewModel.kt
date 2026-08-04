package org.akj.lingo.learn.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import org.akj.lingo.learn.domain.repository.ConfigRepository
import org.akj.lingo.learn.domain.repository.ErrorBookRepository
import org.akj.lingo.learn.domain.repository.GamificationRepository
import org.akj.lingo.learn.domain.repository.LearningRecordRepository
import org.akj.lingo.learn.domain.repository.WeeklyPlanRepository
import org.akj.lingo.learn.domain.repository.WidgetContentRepository
import org.akj.lingo.learn.domain.usecase.DailyEncouragerUseCase
import org.akj.lingo.learn.domain.usecase.DiagnosticCalibrationUseCase
import org.akj.lingo.learn.domain.usecase.WidgetContentGenerator
import org.akj.lingo.learn.domain.usecase.XpRewardSystem
import org.akj.lingo.learn.domain.model.WidgetInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val streakDays: Int = 0,
    val todayProgress: Float = 0.0f,
    val errorCount: Int = 0,
    val themeName: String = "School Life",
    val taskDuration: String = "15 Mins",
    val taskTarget: String = "5 Words + 2 Speech",
    val greetingMessage: String? = null,
    val isLoading: Boolean = false,
    /** Non-null when DiagnosticCalibrationUseCase recommends a re-diagnosis. */
    val calibrationPrompt: String? = null,
    // Sprint 10: visible growth — XP / level / makeup balance
    val totalXp: Int = 0,
    val level: Int = 1,
    val xpIntoLevel: Int = 0,
    val xpForNextLevel: Int = 50,
    val xpProgress: Float = 0f,
    val makeupCardsLeft: Int = 2,
    // Sprint 10.5: offline-mode status for the Dashboard banner
    val isOfflineMode: Boolean = false,
    val hasPlan: Boolean = false,
    // Sprint 15: error state for retry UI
    val errorMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val learningRecordRepository: LearningRecordRepository,
    private val errorBookRepository: ErrorBookRepository,
    private val weeklyPlanRepository: WeeklyPlanRepository,
    private val dailyEncouragerUseCase: DailyEncouragerUseCase,
    private val diagnosticCalibrationUseCase: DiagnosticCalibrationUseCase,
    private val gamificationRepository: GamificationRepository,
    private val configRepository: ConfigRepository,
    private val xpRewardSystem: XpRewardSystem,
    private val widgetContentRepository: WidgetContentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val streak = learningRecordRepository.getStreakDays()
                val progress = learningRecordRepository.getTodayProgress()
                val errors = errorBookRepository.getErrorCount()
                val plan = weeklyPlanRepository.getLatestCachedPlan()

                val theme = plan?.theme ?: "School Life"

                // Sprint 10.5: derive today's task target/duration from the cached plan
                // (falling back to sensible defaults when no plan exists yet).
                val todayDay = java.util.Calendar.getInstance().let { cal ->
                    var d = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
                    if (d == 0) d = 7
                    d
                }
                val dayTask = try {
                    weeklyPlanRepository.getDayTaskSummary(todayDay)
                } catch (_: Exception) { null }
                val taskDuration = dayTask?.durationMinutes?.let { "$it Mins" } ?: "15 Mins"
                val taskTarget = dayTask?.targetWords?.takeIf { it.isNotEmpty() }
                    ?.let { "${it.size} Words + 2 Speech" } ?: "5 Words + 2 Speech"

                // Sprint 10: load persisted gamification state for the visible growth bar.
                var gamification = gamificationRepository.getState()

                // Sprint 15: reset daily goals when a new calendar day starts so
                // the 3-goal badges don't show stale "achieved" state from yesterday.
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .format(java.util.Date())
                if (gamification.dailyGoalsDate != today) {
                    gamificationRepository.resetDailyGoals(today, System.currentTimeMillis())
                    gamification = gamificationRepository.getState()
                }

                val levelInfo = xpRewardSystem.levelInfo(gamification.totalXp)
                val makeupMonth = gamification.makeupMonthKey
                val makeupUsed = if (makeupMonth != null) gamification.makeupCardsUsed else 0
                val makeupState = org.akj.lingo.learn.domain.usecase.MakeupCardManager().stateForMonth(
                    nowMs = System.currentTimeMillis(),
                    storedMonthKey = makeupMonth,
                    cardsUsedPreviously = makeupUsed
                )

                _uiState.value = DashboardUiState(
                    streakDays = streak,
                    todayProgress = progress,
                    errorCount = errors,
                    themeName = theme,
                    taskDuration = taskDuration,
                    taskTarget = taskTarget,
                    greetingMessage = null,
                    isLoading = false,
                    totalXp = gamification.totalXp,
                    level = levelInfo.level,
                    xpIntoLevel = levelInfo.xpIntoLevel,
                    xpForNextLevel = levelInfo.xpForNextLevel,
                    xpProgress = levelInfo.progressToNextLevel,
                    makeupCardsLeft = makeupState.cardsLeft,
                    isOfflineMode = configRepository.getAuthToken().length < 10,
                    hasPlan = plan != null
                )

                // Fetch daily greeting in background
                val prefs = context.getSharedPreferences("lingo_app_prefs", Context.MODE_PRIVATE)
                val childName = prefs.getString("child_name", "Buddy") ?: "Buddy"
                val result = dailyEncouragerUseCase(childName, streak)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(greetingMessage = result.getOrNull())
                }

                // Sprint 14: pre-generate personalized widget content and cache it
                // so the Glance widget (no DI) can read data-driven text.
                val widgetContent = WidgetContentGenerator.generate(
                    WidgetInput(
                        streakDays = streak,
                        todayDone = progress >= 1.0f,
                        childName = childName,
                        todayTaskTheme = theme.takeIf { it.isNotBlank() },
                        errorCount = errors,
                        totalXp = gamification.totalXp
                    )
                )
                widgetContentRepository.save(widgetContent)

                // Sprint 8: Diagnostic Calibration check every 14 active days
                val prefsEditor = prefs.edit()
                // Count each new active day (dashboard opened on a fresh date)
                if (prefs.getString("last_active_day", null) != today) {
                    val currentDays = prefs.getInt("days_since_calibration", 0)
                    prefsEditor.putInt("days_since_calibration", currentDays + 1)
                    prefsEditor.putString("last_active_day", today)
                }
                // Seed the baseline on first run so later deltas are meaningful
                if (!prefs.contains("baseline_accuracy")) {
                    val monthly = learningRecordRepository.getMonthlyAccuracy()
                    prefsEditor.putFloat("baseline_accuracy", if (monthly == 0f) 70f else monthly)
                }
                prefsEditor.apply()

                val baselineAccuracy = prefs.getFloat("baseline_accuracy", 70f)
                val daysSinceCalibration = prefs.getInt("days_since_calibration", 0)
                val calibrationResult = diagnosticCalibrationUseCase.check(
                    baselineAccuracy = baselineAccuracy,
                    activeDaysSinceLastCalibration = daysSinceCalibration
                )
                if (calibrationResult.shouldSuggestRecalibration) {
                    _uiState.value = _uiState.value.copy(calibrationPrompt = calibrationResult.reason)
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Couldn't load your dashboard. Check your connection and try again."
                )
            }
        }
    }

    /** Sprint 15: Called from the error retry button. */
    fun retry() {
        loadDashboardData()
    }

    /** Called when the user dismisses the calibration prompt dialog. */
    fun dismissCalibrationPrompt() {
        _uiState.value = _uiState.value.copy(calibrationPrompt = null)
        viewModelScope.launch {
            // Reset the counter and refresh the baseline so the next cycle
            // compares against current performance, not the stale baseline.
            val monthly = learningRecordRepository.getMonthlyAccuracy()
            val newBaseline = if (monthly == 0f) 70f else monthly
            context.getSharedPreferences("lingo_app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putInt("days_since_calibration", 0)
                .putFloat("baseline_accuracy", newBaseline)
                .apply()
        }
    }
}
