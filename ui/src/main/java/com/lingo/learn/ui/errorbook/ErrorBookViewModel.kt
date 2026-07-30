package org.akj.lingo.learn.ui.errorbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.akj.lingo.learn.domain.model.ErrorBookEntry
import org.akj.lingo.learn.domain.repository.ErrorBookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import org.akj.lingo.learn.domain.usecase.ExplanationAgentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ErrorBookSortMode { PRIORITY, DATE, ERROR_COUNT, ERROR_TYPE }

data class ErrorBookUiState(
    val entries: List<ErrorBookEntry> = emptyList(),
    val sortMode: ErrorBookSortMode = ErrorBookSortMode.PRIORITY,
    val isLoading: Boolean = false,
    val totalCount: Int = 0,
    val reviewCount: Int = 0,
    val consolidatedCount: Int = 0
)

@HiltViewModel
class ErrorBookViewModel @Inject constructor(
    private val errorBookRepository: ErrorBookRepository,
    private val explanationAgentUseCase: ExplanationAgentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ErrorBookUiState())
    val uiState: StateFlow<ErrorBookUiState> = _uiState.asStateFlow()
    
    private val _explanationState = MutableStateFlow<Map<String, String>>(emptyMap())
    val explanationState: StateFlow<Map<String, String>> = _explanationState.asStateFlow()

    init { loadErrors() }

    fun loadErrors() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val all = errorBookRepository.getTopPriorityErrors(100)
                val sorted = sortEntries(all, _uiState.value.sortMode)
                _uiState.value = ErrorBookUiState(
                    entries = sorted,
                    sortMode = _uiState.value.sortMode,
                    totalCount = all.size,
                    reviewCount = all.count { it.status == "TO_REVIEW" },
                    consolidatedCount = all.count { it.status == "CONSOLIDATED" || it.status == "GRADUATION_OBSERVATION" },
                    isLoading = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun setSortMode(mode: ErrorBookSortMode) {
        val sorted = sortEntries(_uiState.value.entries, mode)
        _uiState.value = _uiState.value.copy(sortMode = mode, entries = sorted)
    }

    private fun sortEntries(entries: List<ErrorBookEntry>, mode: ErrorBookSortMode): List<ErrorBookEntry> {
        return when (mode) {
            ErrorBookSortMode.PRIORITY -> entries.sortedByDescending { it.priorityScore }
            ErrorBookSortMode.DATE -> entries.sortedByDescending { it.lastErrorTimestamp }
            ErrorBookSortMode.ERROR_COUNT -> entries.sortedByDescending { it.errorCount }
            ErrorBookSortMode.ERROR_TYPE -> entries.sortedBy { it.errorType }
        }
    }

    fun getErrorTypeDisplay(type: String): String = when (type.uppercase()) {
        "SPELLED_WRONG" -> "Spelling"
        "LISTENING_WRONG" -> "Listening"
        "GRAMMAR_WRONG" -> "Grammar"
        "PRONUNCIATION_WRONG" -> "Pronunciation"
        else -> type
    }

    fun getErrorTypeEmoji(type: String): String = when (type.uppercase()) {
        "SPELLED_WRONG" -> "✍️"
        "LISTENING_WRONG" -> "👂"
        "GRAMMAR_WRONG" -> "📝"
        "PRONUNCIATION_WRONG" -> "🗣️"
        else -> "❓"
    }

    fun getStatusColor(status: String): Long = when (status) {
        "TO_REVIEW" -> 0xFFFF7052L
        "CONSOLIDATED" -> 0xFF5C6FF2L
        "GRADUATION_OBSERVATION" -> 0xFFFFD449L
        "GRADUATED" -> 0xFF2ECC71L
        else -> 0xFF7F8C8DL
    }

    fun getStatusEmoji(status: String): String = when (status) {
        "TO_REVIEW" -> "🔴"
        "CONSOLIDATED" -> "🟡"
        "GRADUATION_OBSERVATION" -> "🔵"
        "GRADUATED" -> "🟢"
        else -> "⚪"
    }

    fun fetchExplanation(word: String, errorType: String, grade: String) {
        if (_explanationState.value.containsKey(word)) return
        
        viewModelScope.launch {
            _explanationState.value = _explanationState.value + (word to "Thinking...")
            val result = explanationAgentUseCase(word, errorType, grade)
            if (result.isSuccess) {
                _explanationState.value = _explanationState.value + (word to result.getOrNull().orEmpty())
            } else {
                _explanationState.value = _explanationState.value + (word to "Failed to get explanation. Try again later.")
            }
        }
    }
}
