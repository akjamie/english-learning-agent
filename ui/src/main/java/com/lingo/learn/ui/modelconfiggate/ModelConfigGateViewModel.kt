package org.akj.lingo.learn.ui.modelconfiggate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.akj.lingo.learn.domain.provider.ProviderEndpoints
import org.akj.lingo.learn.domain.repository.ConfigRepository
import org.akj.lingo.learn.domain.repository.LlmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelConfigGateState(
    val baseUrl: String = "https://ark.cn-beijing.volces.com/api/plan/v3",
    val authToken: String = "",
    val isTesting: Boolean = false,
    val testSuccess: Boolean? = null,
    val testResult: String? = null,
    val isConfigured: Boolean = false
)

@HiltViewModel
class ModelConfigGateViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
    private val llmRepository: LlmRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ModelConfigGateState())
    val state: StateFlow<ModelConfigGateState> = _state.asStateFlow()

    fun updateBaseUrl(value: String) {
        _state.update { it.copy(baseUrl = value, testSuccess = null, testResult = null, isConfigured = false) }
    }

    fun updateAuthToken(value: String) {
        _state.update { it.copy(authToken = value, testSuccess = null, testResult = null, isConfigured = false) }
    }

    fun testConnection() {
        val s = _state.value
        if (s.baseUrl.isBlank() || s.authToken.isBlank()) return
        configRepository.setBaseUrl(s.baseUrl.trim())
        configRepository.setAuthToken(s.authToken.trim())
        _state.update { it.copy(isTesting = true, testSuccess = null, testResult = null) }
        val resolvedUrl = ProviderEndpoints.chatUrl(s.baseUrl.trim())
        viewModelScope.launch {
            try {
                val result = llmRepository.complete(
                    prompt = "Hello! Please reply 'OK' to confirm API connection.",
                    taskType = "PING"
                )
                if (result.isSuccess) {
                    _state.update {
                        it.copy(
                            isTesting = false,
                            testSuccess = true,
                            testResult = "Connection successful! Endpoint: $resolvedUrl\nModel: ${configRepository.getPrimaryModel()}",
                            isConfigured = true
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isTesting = false,
                            testSuccess = false,
                            testResult = "Connection failed at $resolvedUrl: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isTesting = false,
                        testSuccess = false,
                        testResult = "Error at $resolvedUrl: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }
}
