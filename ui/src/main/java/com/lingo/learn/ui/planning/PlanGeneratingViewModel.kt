package org.akj.lingo.learn.ui.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.akj.lingo.learn.domain.repository.ErrorBookRepository
import org.akj.lingo.learn.domain.repository.LearningRecordRepository
import org.akj.lingo.learn.domain.repository.WeeklyPlanRepository
import org.akj.lingo.learn.domain.usecase.classifyError
import org.akj.lingo.learn.domain.usecase.userFacingError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanGeneratingState(
    val isGenerating: Boolean = true,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val progressText: String = "Preparing your AI tutor..."
)

@HiltViewModel
class PlanGeneratingViewModel @Inject constructor(
    private val weeklyPlanRepository: WeeklyPlanRepository,
    private val learningRecordRepository: LearningRecordRepository,
    private val errorBookRepository: ErrorBookRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PlanGeneratingState())
    val state: StateFlow<PlanGeneratingState> = _state.asStateFlow()

    fun generatePlan(grade: String, diagnosticLevel: String) {
        // Reset the whole state so a retry starts clean: spinner on,
        // previous error cleared, success flag cleared.
        _state.value = PlanGeneratingState(progressText = "Preparing your AI tutor...")
        viewModelScope.launch {
            try {
                val accuracy = try { learningRecordRepository.getMonthlyAccuracy() } catch (_: Exception) { 75f }
                val weakCategories = try { learningRecordRepository.getWeakCategories() } catch (_: Exception) { emptyList() }
                val streakDays = try { learningRecordRepository.getStreakDays() } catch (_: Exception) { 0 }
                val errorCount = try { errorBookRepository.getErrorCount() } catch (_: Exception) { 0 }
                val completedMilestones = buildList {
                    if (streakDays > 0) add("${streakDays}-day streak")
                    if (errorCount > 0) add("$errorCount words in error book")
                }

                val difficultyAdjustment = when (diagnosticLevel.uppercase()) {
                    "A" -> -0.2f; "C" -> 0.2f; else -> 0f
                }

                _state.value = _state.value.copy(progressText = "Analyzing your learning data...")
                delay(600)

                _state.value = _state.value.copy(progressText = "Generating your personalized weekly plan...")
                val result = weeklyPlanRepository.generateAndCacheWeeklyPlan(
                    grade = grade,
                    accuracy = (accuracy * 100).toInt(),
                    weakCategories = weakCategories,
                    completedMilestones = completedMilestones,
                    difficultyAdjustment = difficultyAdjustment
                )
                delay(400)

                if (result.isSuccess) {
                    _state.value = _state.value.copy(
                        isGenerating = false,
                        isSuccess = true,
                        progressText = "Plan ready! Redirecting..."
                    )
                    delay(800)
                } else {
                    _state.value = _state.value.copy(
                        isGenerating = false,
                        errorMessage = userFacingError(classifyError(result.exceptionOrNull()))
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isGenerating = false,
                    errorMessage = userFacingError(classifyError(e))
                )
            }
        }
    }
}
