package org.akj.lingo.learn.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.akj.lingo.learn.domain.repository.ConfigRepository
import org.akj.lingo.learn.domain.repository.LlmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val baseUrl: String = "",
    val authToken: String = "",
    val groupId: String = "",
    val primaryModel: String = "",
    val fallbackModel: String = "",
    val ttsModel: String = "",
    val asrModel: String = "",
    val llmEndpoint: String = "/v1/chat/completions",
    val asrScoreThreshold: Int = 60,
    val monthlyTokenLimit: Int = 50000,
    val language: String = "en",
    val isTestingConnection: Boolean = false,
    val connectionTestResult: String? = null,
    val connectionTestSuccess: Boolean? = null,
    val isSaved: Boolean = false
)

/**
 * ViewModel for managing AI model provider configurations and API connectivity testing.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
    private val llmRepository: LlmRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        _uiState.update {
            it.copy(
                baseUrl = configRepository.getBaseUrl(),
                authToken = configRepository.getAuthToken(),
                groupId = configRepository.getGroupId(),
                primaryModel = configRepository.getPrimaryModel(),
                fallbackModel = configRepository.getFallbackModel(),
                ttsModel = configRepository.getTtsModel(),
                asrModel = configRepository.getAsrModel(),
                llmEndpoint = configRepository.getLlmEndpoint(),
                asrScoreThreshold = configRepository.getAsrScoreThreshold(),
                monthlyTokenLimit = configRepository.getMonthlyTokenLimit(),
                language = configRepository.getLanguage(),
                isSaved = false
            )
        }
    }

    fun updateBaseUrl(value: String) {
        _uiState.update { it.copy(baseUrl = value, isSaved = false) }
    }

    fun updateAuthToken(value: String) {
        _uiState.update { it.copy(authToken = value, isSaved = false) }
    }

    fun updateGroupId(value: String) {
        _uiState.update { it.copy(groupId = value, isSaved = false) }
    }

    fun updatePrimaryModel(value: String) {
        _uiState.update { it.copy(primaryModel = value, isSaved = false) }
    }

    fun updateFallbackModel(value: String) {
        _uiState.update { it.copy(fallbackModel = value, isSaved = false) }
    }

    fun updateTtsModel(value: String) {
        _uiState.update { it.copy(ttsModel = value, isSaved = false) }
    }

    fun updateAsrModel(value: String) {
        _uiState.update { it.copy(asrModel = value, isSaved = false) }
    }

    fun updateLlmEndpoint(value: String) {
        _uiState.update { it.copy(llmEndpoint = value, isSaved = false) }
    }

    fun updateAsrScoreThreshold(value: Int) {
        _uiState.update { it.copy(asrScoreThreshold = value, isSaved = false) }
    }

    fun updateMonthlyTokenLimit(value: Int) {
        _uiState.update { it.copy(monthlyTokenLimit = value, isSaved = false) }
    }

    fun updateLanguage(value: String) {
        _uiState.update { it.copy(language = value, isSaved = false) }
    }

    fun saveSettings() {
        val state = _uiState.value
        configRepository.setBaseUrl(state.baseUrl.trim())
        configRepository.setAuthToken(state.authToken.trim())
        configRepository.setGroupId(state.groupId.trim())
        configRepository.setPrimaryModel(state.primaryModel.trim())
        configRepository.setFallbackModel(state.fallbackModel.trim())
        configRepository.setTtsModel(state.ttsModel.trim())
        configRepository.setAsrModel(state.asrModel.trim())
        configRepository.setAsrScoreThreshold(state.asrScoreThreshold)
        configRepository.setMonthlyTokenLimit(state.monthlyTokenLimit)
        configRepository.setLanguage(state.language)

        _uiState.update { it.copy(isSaved = true) }
    }

    fun testApiConnection() {
        saveSettings()
        _uiState.update { it.copy(isTestingConnection = true, connectionTestResult = null, connectionTestSuccess = null) }

        viewModelScope.launch {
            try {
                val result = llmRepository.complete(
                    prompt = "Hello! Please reply 'OK' to confirm API connection.",
                    taskType = "PING"
                )
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isTestingConnection = false,
                            connectionTestSuccess = true,
                            connectionTestResult = "Connection Successful! Model responded: ${result.getOrNull()?.take(50)}"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isTestingConnection = false,
                            connectionTestSuccess = false,
                            connectionTestResult = "Connection Failed: ${result.exceptionOrNull()?.message ?: "Unknown Error"}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        connectionTestSuccess = false,
                        connectionTestResult = "Connection Error: ${e.localizedMessage}"
                    )
                }
            }
        }
    }
}
