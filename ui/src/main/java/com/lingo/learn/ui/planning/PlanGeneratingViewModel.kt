package org.akj.lingo.learn.ui.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.akj.lingo.learn.domain.repository.WeeklyPlanRepository
import org.akj.lingo.learn.domain.usecase.StudentContextService
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
    private val studentContextService: StudentContextService
) : ViewModel() {

    private val _state = MutableStateFlow(PlanGeneratingState())
    val state: StateFlow<PlanGeneratingState> = _state.asStateFlow()

    fun generatePlan(grade: String, diagnosticLevel: String) {
        // Reset the whole state so a retry starts clean: spinner on,
        // previous error cleared, success flag cleared.
        _state.value = PlanGeneratingState(progressText = "Preparing your AI tutor...")
        viewModelScope.launch {
            try {
                val context = studentContextService.load(grade, diagnosticLevel)

                _state.value = _state.value.copy(progressText = "Analyzing your learning data...")
                delay(600)

                _state.value = _state.value.copy(progressText = "Generating your personalized weekly plan...")
                val result = weeklyPlanRepository.generateAndCacheWeeklyPlan(
                    grade = grade,
                    accuracy = context.accuracyPercent,
                    weakCategories = context.weakCategories,
                    completedMilestones = context.completedMilestones,
                    difficultyAdjustment = context.difficultyAdjustment
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
