package org.akj.lingo.learn.ui.weeklyplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.akj.lingo.learn.domain.model.Plan
import org.akj.lingo.learn.domain.repository.ErrorBookRepository
import org.akj.lingo.learn.domain.repository.LearningRecordRepository
import org.akj.lingo.learn.domain.repository.WeeklyPlanRepository
import org.akj.lingo.learn.domain.usecase.ExplainDecisionUseCase
import org.akj.lingo.learn.domain.usecase.classifyError
import org.akj.lingo.learn.domain.usecase.userFacingError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class PlanDayItem(
    val day: Int,
    val focus: String,
    val targetWords: List<String>,
    val referenceSentence: String,
    val durationMinutes: Int,
    val rationale: String = "",
    val isCompleted: Boolean = false
)

data class WeeklyPlanUiState(
    val theme: String = "",
    val difficultyCoefficient: Float = 1.0f,
    val days: List<PlanDayItem> = emptyList(),
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val generateError: String? = null,
    // Sprint 15: error state for the load (not generate) path
    val loadError: String? = null
)

@HiltViewModel
class WeeklyPlanViewModel @Inject constructor(
    private val weeklyPlanRepository: WeeklyPlanRepository,
    private val explainDecisionUseCase: ExplainDecisionUseCase,
    private val learningRecordRepository: LearningRecordRepository,
    private val errorBookRepository: ErrorBookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyPlanUiState())
    val uiState: StateFlow<WeeklyPlanUiState> = _uiState.asStateFlow()

    init { loadPlan() }

    fun loadPlan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, loadError = null)
            try {
                val plan = weeklyPlanRepository.getLatestCachedPlan()
                if (plan != null) {
                    val rationales = explainDecisionUseCase.allDayRationales(plan).toMap()
                    _uiState.value = _uiState.value.copy(
                        theme = plan.theme,
                        difficultyCoefficient = plan.difficultyCoefficient,
                        days = parsePlanDays(plan.snapshotData, rationales),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadError = "Couldn't load your plan. Tap retry."
                )
            }
        }
    }

    fun generateNewPlan(grade: String = "Grade 4", diagnosticLevel: String = "B") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, generateError = null)
            // Sprint 10.5: diagnostic level tunes difficulty within the grade band.
            // A (beginner) lowers, C (advanced) raises the plan's difficulty coefficient.
            val difficultyAdjustment = when (diagnosticLevel.uppercase()) {
                "A" -> -0.2f
                "C" -> 0.2f
                else -> 0f
            }
            // Sprint 14: fetch real learning metrics instead of hardcoded values.
            val accuracy = try { learningRecordRepository.getMonthlyAccuracy() } catch (_: Exception) { 75f }
            val weakCategories = try { learningRecordRepository.getWeakCategories() } catch (_: Exception) { emptyList() }
            val streakDays = try { learningRecordRepository.getStreakDays() } catch (_: Exception) { 0 }
            val errorCount = try { errorBookRepository.getErrorCount() } catch (_: Exception) { 0 }
            val completedMilestones = buildList {
                if (streakDays > 0) add("${streakDays}-day streak")
                if (errorCount > 0) add("$errorCount words in error book")
                if (isEmpty()) add("First week starting")
            }
            val result = weeklyPlanRepository.generateAndCacheWeeklyPlan(
                grade = grade,
                accuracy = (accuracy * 100).toInt(),
                weakCategories = weakCategories.ifEmpty { listOf("Vocabulary", "Pronunciation") },
                completedMilestones = completedMilestones,
                difficultyAdjustment = difficultyAdjustment
            )
            result.onSuccess { plan ->
                val rationales = explainDecisionUseCase.allDayRationales(plan).toMap()
                _uiState.value = WeeklyPlanUiState(
                    theme = plan.theme,
                    difficultyCoefficient = plan.difficultyCoefficient,
                    days = parsePlanDays(plan.snapshotData, rationales),
                    isGenerating = false
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    generateError = userFacingError(classifyError(e))
                )
            }
        }
    }

    private fun parsePlanDays(snapshotData: String, rationales: Map<Int, String> = emptyMap()): List<PlanDayItem> {
        return try {
            val json = JSONObject(snapshotData)
            val daysArray = json.optJSONArray("days") ?: return emptyList()
            val today = getDayOfWeek()
            (0 until daysArray.length()).map { i ->
                val dayObj = daysArray.getJSONObject(i)
                val words = mutableListOf<String>()
                val wordsArray = dayObj.optJSONArray("target_words")
                if (wordsArray != null) {
                    for (j in 0 until wordsArray.length()) {
                        words.add(wordsArray.getString(j))
                    }
                }
                val dayNumber = dayObj.optInt("day", i + 1)
                PlanDayItem(
                    day = dayNumber,
                    focus = dayObj.optString("focus", "Practice"),
                    targetWords = words,
                    referenceSentence = dayObj.optString("reference_sentence", ""),
                    durationMinutes = dayObj.optInt("duration_minutes", 15),
                    rationale = rationales[dayNumber] ?: dayObj.optString("rationale", ""),
                    isCompleted = dayNumber < today
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getDayOfWeek(): Int {
        val cal = java.util.Calendar.getInstance()
        var day = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
        if (day == 0) day = 7
        return day
    }

    fun getFormattedDateRange(): String {
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        val start = sdf.format(Date(cal.timeInMillis))
        cal.add(java.util.Calendar.DAY_OF_WEEK, 6)
        val end = sdf.format(Date(cal.timeInMillis))
        return "$start - $end"
    }
}

fun getFocusEmoji(focus: String): String = when {
    focus.contains("Vocabulary", ignoreCase = true) || focus.contains("Vocab", ignoreCase = true) -> "📖"
    focus.contains("Grammar", ignoreCase = true) || focus.contains("Syntax", ignoreCase = true) -> "📝"
    focus.contains("Dialogue", ignoreCase = true) || focus.contains("Conversation", ignoreCase = true) || focus.contains("Speaking", ignoreCase = true) -> "💬"
    focus.contains("Pronunc", ignoreCase = true) || focus.contains("Phonics", ignoreCase = true) -> "🗣️"
    focus.contains("Writing", ignoreCase = true) || focus.contains("Essay", ignoreCase = true) -> "✍️"
    focus.contains("Reading", ignoreCase = true) || focus.contains("Academic", ignoreCase = true) -> "📚"
    focus.contains("Debate", ignoreCase = true) || focus.contains("Argument", ignoreCase = true) -> "🎤"
    focus.contains("Listen", ignoreCase = true) -> "🎧"
    else -> "📅"
}
