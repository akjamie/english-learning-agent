package org.akj.lingo.learn.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import org.akj.lingo.learn.domain.repository.ErrorBookRepository
import org.akj.lingo.learn.domain.repository.LearningRecordRepository
import org.akj.lingo.learn.domain.repository.WeeklyPlanRepository
import org.akj.lingo.learn.domain.usecase.DailyEncouragerUseCase
import org.akj.lingo.learn.domain.usecase.DiagnosticCalibrationUseCase
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
    val calibrationPrompt: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val learningRecordRepository: LearningRecordRepository,
    private val errorBookRepository: ErrorBookRepository,
    private val weeklyPlanRepository: WeeklyPlanRepository,
    private val dailyEncouragerUseCase: DailyEncouragerUseCase,
    private val diagnosticCalibrationUseCase: DiagnosticCalibrationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val streak = learningRecordRepository.getStreakDays()
                val progress = learningRecordRepository.getTodayProgress()
                val errors = errorBookRepository.getErrorCount()
                val plan = weeklyPlanRepository.getLatestCachedPlan()

                val theme = plan?.theme ?: "School Life"

                _uiState.value = DashboardUiState(
                    streakDays = streak,
                    todayProgress = progress,
                    errorCount = errors,
                    themeName = theme,
                    taskDuration = "15 Mins",
                    taskTarget = "5 Words + 2 Speech",
                    greetingMessage = null,
                    isLoading = false
                )

                // Fetch daily greeting in background
                val prefs = context.getSharedPreferences("lingo_app_prefs", Context.MODE_PRIVATE)
                val childName = prefs.getString("child_name", "Buddy") ?: "Buddy"
                val result = dailyEncouragerUseCase(childName, streak)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(greetingMessage = result.getOrNull())
                }

                // Sprint 8: Diagnostic Calibration check every 14 active days
                val prefsEditor = prefs.edit()
                // Count each new active day (dashboard opened on a fresh date)
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .format(java.util.Date())
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
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
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
