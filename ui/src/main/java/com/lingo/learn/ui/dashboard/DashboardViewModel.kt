package org.akj.lingo.learn.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.akj.lingo.learn.domain.repository.ErrorBookRepository
import org.akj.lingo.learn.domain.repository.LearningRecordRepository
import org.akj.lingo.learn.domain.repository.WeeklyPlanRepository
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
    val isLoading: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val learningRecordRepository: LearningRecordRepository,
    private val errorBookRepository: ErrorBookRepository,
    private val weeklyPlanRepository: WeeklyPlanRepository
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
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
