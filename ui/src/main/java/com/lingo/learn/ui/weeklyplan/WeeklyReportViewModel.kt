package org.akj.lingo.learn.ui.weeklyplan

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import org.akj.lingo.learn.domain.model.AgentDecisionLog
import org.akj.lingo.learn.domain.repository.AgentDecisionLogRepository
import org.akj.lingo.learn.domain.repository.ErrorBookRepository
import org.akj.lingo.learn.domain.repository.LearningRecordRepository
import org.akj.lingo.learn.domain.repository.LlmRepository
import org.akj.lingo.learn.domain.repository.WeeklyPlanRepository
import org.akj.lingo.learn.domain.usecase.CefrMapper
import org.akj.lingo.learn.domain.usecase.LingoLetterFallback
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
    val agentAdjustments: List<String> = emptyList(),
    val topErrorWords: List<String> = emptyList(),
    val timeByTaskType: Map<String, Long> = emptyMap(),
    val isLoading: Boolean = false,
    val shareBitmap: ByteArray? = null,
    // Sprint 12: "Lingo's letter" weekly parent digest
    val lingoLetter: String? = null,
    // Sprint 19: estimated CEFR level label (e.g. "A1")
    val cefrLabel: String? = null,
    val loadError: String? = null
)

@HiltViewModel
class WeeklyReportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val learningRecordRepository: LearningRecordRepository,
    private val weeklyPlanRepository: WeeklyPlanRepository,
    private val agentDecisionLogRepository: AgentDecisionLogRepository,
    private val errorBookRepository: ErrorBookRepository,
    private val llmRepository: LlmRepository
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
                val adjustments = loadAdjustments()
                val topErrors = loadTopErrorWords()
                val timeByType = buildTimeDistribution(weeklyRecords)

                val weeklyAccuracy = if (weeklyRecords.isNotEmpty()) {
                    weeklyRecords.map { it.accuracy }.average().toFloat()
                } else 0f

                val prefs = context.getSharedPreferences("lingo_app_prefs", Context.MODE_PRIVATE)
                val grade = prefs.getString("grade", "Grade 4") ?: "Grade 4"
                val diagnosticLevel = prefs.getString("diagnostic_level", "B") ?: "B"
                val cefrLabel = CefrMapper.badge(CefrMapper.map(grade, diagnosticLevel))

                _uiState.value = WeeklyReportUiState(
                    weeklyAccuracy = weeklyAccuracy,
                    monthlyAccuracy = monthlyAccuracy,
                    streakDays = streak,
                    totalSessions = weeklyRecords.size,
                    totalWordsLearned = (weeklyRecords.size * 3).coerceAtLeast(0),
                    weakCategories = weakCats,
                    themeName = plan?.theme ?: "No active plan",
                    agentAdjustments = adjustments,
                    topErrorWords = topErrors,
                    timeByTaskType = timeByType,
                    isLoading = false,
                    lingoLetter = loadLingoLetter(weeklyAccuracy, weeklyRecords.size, topErrors),
                    cefrLabel = cefrLabel
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, loadError = "Couldn't load weekly report. Tap retry.")
            }
        }
    }

    /**
     * Sprint 12: "Lingo's letter" — a short agent-generated weekly digest for parents.
     * Uses the LLM when configured; otherwise falls back to a data-driven template.
     */
    private suspend fun loadLingoLetter(
        weeklyAccuracy: Float,
        sessions: Int,
        topErrorWords: List<String>
    ): String? {
        val prompt = """
            You are Lingo, the fox tutor. Summarize this week's learning for a parent about their child.

            --- This Week's Data ---
            Weekly accuracy: ${(weeklyAccuracy * 100).toInt()}%
            Sessions completed: $sessions
            Weak areas: ${_uiState.value.weakCategories.joinToString()}
            Words to review: ${topErrorWords.joinToString()}

            --- Guidelines ---
            1. 3-4 short sentences, warm and specific (reference actual numbers/words).
            2. Lead with progress, then ONE gentle area to practice.
            3. Max 60 words. Frame as growth, never as grades.
        """.trimIndent()
        val result = llmRepository.complete(prompt, taskType = "LINGO_LETTER", maxTokens = 120)
        return result.getOrNull() ?: LingoLetterFallback.digest(weeklyAccuracy, sessions, topErrorWords)
    }

    /** Top 5 most-missed words currently in the Error Book for the parent report. */
    private suspend fun loadTopErrorWords(): List<String> {
        return errorBookRepository.getTopPriorityErrors(limit = 5)
            .map { it.vocabId }
    }

    /** Total learning time (seconds) split by activity type. */
    private fun buildTimeDistribution(records: List<org.akj.lingo.learn.domain.model.LearningRecord>): Map<String, Long> {
        return records.groupBy { it.taskType }.mapValues { (_, rs) -> rs.sumOf { it.duration } }
    }

    /** Pulls the past 7 days of logged agent decisions and renders 1-2 in natural language. */
    private suspend fun loadAdjustments(): List<String> {
        val decisions = agentDecisionLogRepository.getDecisionsSince(
            System.currentTimeMillis() - 7 * 24 * 3600 * 1000L
        )
        return decisions.take(2).map { formatDecision(it) }
    }

    private fun formatDecision(log: AgentDecisionLog): String {
        return when (log.decisionType) {
            "OBSERVATION_MADE" -> "🦊 Lingo noticed: ${log.description}"
            "PLAN_GENERATED" -> "📅 Lingo rearranged the week: ${log.description}"
            "DIFFICULTY_ADJUSTED" -> "⚖️ Lingo adjusted difficulty: ${log.description}"
            "ERROR_PATTERN" -> "🎯 Lingo spotted a pattern: ${log.description}"
            else -> "🦊 Lingo: ${log.description}"
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
