package org.akj.lingo.learn.ui.weeklyplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.akj.lingo.learn.domain.repository.LearningRecordRepository
import org.akj.lingo.learn.domain.repository.WeeklyPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeeklyReportUiState(
    val weeklyAccuracy: Float = 0f,
    val monthlyAccuracy: Float = 0f,
    val streakDays: Int = 0,
    val totalSessions: Int = 0,
    val totalWordsLearned: Int = 0,
    val weakCategories: List<String> = emptyList(),
    val themeName: String = "",
    val isLoading: Boolean = false,
    val shareBitmap: ByteArray? = null
)

@HiltViewModel
class WeeklyReportViewModel @Inject constructor(
    private val learningRecordRepository: LearningRecordRepository,
    private val weeklyPlanRepository: WeeklyPlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyReportUiState())
    val uiState: StateFlow<WeeklyReportUiState> = _uiState.asStateFlow()

    init { loadReport() }

    fun loadReport() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val weeklyRecords = learningRecordRepository.getWeeklyRecords()
                val monthlyAccuracy = learningRecordRepository.getMonthlyAccuracy()
                val streak = learningRecordRepository.getStreakDays()
                val weakCats = learningRecordRepository.getWeakCategories()
                val plan = weeklyPlanRepository.getLatestCachedPlan()

                val weeklyAccuracy = if (weeklyRecords.isNotEmpty()) {
                    weeklyRecords.map { it.accuracy }.average().toFloat()
                } else 0f

                _uiState.value = WeeklyReportUiState(
                    weeklyAccuracy = weeklyAccuracy,
                    monthlyAccuracy = monthlyAccuracy,
                    streakDays = streak,
                    totalSessions = weeklyRecords.size,
                    totalWordsLearned = (weeklyRecords.size * 3).coerceAtLeast(0),
                    weakCategories = weakCats,
                    themeName = plan?.theme ?: "No active plan",
                    isLoading = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun getGradeMessage(accuracy: Float): String = when {
        accuracy >= 0.9f -> "Outstanding! 🌟"
        accuracy >= 0.7f -> "Great work! 👍"
        accuracy >= 0.5f -> "Keep trying! 💪"
        else -> "Let's improve! 📈"
    }

    fun getGradeColor(accuracy: Float): Long = when {
        accuracy >= 0.9f -> 0xFF2ECC71L
        accuracy >= 0.7f -> 0xFF5C6FF2L
        accuracy >= 0.5f -> 0xFFFFD449L
        else -> 0xFFFF7052L
    }
}
