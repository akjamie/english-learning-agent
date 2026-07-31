package org.akj.lingo.learn.ui.aigrowthnotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.akj.lingo.learn.domain.model.AgentDecisionLog
import org.akj.lingo.learn.domain.repository.AgentDecisionLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiGrowthNotesUiState(
    val entries: List<AgentDecisionLog> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class AiGrowthNotesViewModel @Inject constructor(
    private val agentDecisionLogRepository: AgentDecisionLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiGrowthNotesUiState())
    val uiState: StateFlow<AiGrowthNotesUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val entries = agentDecisionLogRepository.getRecentDecisions(limit = 100)
                _uiState.value = AiGrowthNotesUiState(entries = entries, isLoading = false)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun typeLabel(decisionType: String): String = when (decisionType) {
        "OBSERVATION_MADE" -> "Observation"
        "PLAN_GENERATED" -> "Plan"
        "DIFFICULTY_ADJUSTED" -> "Difficulty"
        "ERROR_PATTERN" -> "Pattern"
        else -> "Note"
    }

    fun typeColor(decisionType: String): Long = when (decisionType) {
        "OBSERVATION_MADE" -> 0xFF5C6FF2
        "PLAN_GENERATED" -> 0xFF2ECC71
        "DIFFICULTY_ADJUSTED" -> 0xFFFFA726
        "ERROR_PATTERN" -> 0xFFFF7052
        else -> 0xFF7F8C8D
    }
}
