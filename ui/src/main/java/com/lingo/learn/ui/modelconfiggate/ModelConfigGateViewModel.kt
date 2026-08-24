package org.akj.lingo.learn.ui.modelconfiggate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.akj.lingo.learn.domain.provider.ProviderEndpoints
import org.akj.lingo.learn.domain.repository.ConfigRepository
import org.akj.lingo.learn.domain.repository.LlmRepository
import org.akj.lingo.learn.domain.repository.TtsRepository
import org.akj.lingo.learn.domain.usecase.AgentPromptRegistry
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
    val primaryModel: String = "",
    val ttsModel: String = "",
    val asrModel: String = "",
    val ttsBaseUrl: String = "",
    val ttsResourceId: String = "",
    val ttsSpeaker: String = "",
    val asrResourceId: String = "",
    val asrWsUrl: String = "",
    val isTesting: Boolean = false,
    val testSuccess: Boolean? = null,
    val testResult: String? = null,
    val isVoiceTesting: Boolean = false,
    val voiceTestSuccess: Boolean? = null,
    val voiceTestResult: String? = null,
    val isConfigured: Boolean = false
)

@HiltViewModel
class ModelConfigGateViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
    private val llmRepository: LlmRepository,
    private val ttsRepository: TtsRepository,
    private val promptRegistry: AgentPromptRegistry
) : ViewModel() {

    private val savedBaseUrl = configRepository.getBaseUrl()
    private val savedAuthToken = configRepository.getAuthToken()

    private val _state = MutableStateFlow(
        ModelConfigGateState(
            baseUrl = savedBaseUrl.ifBlank { "https://ark.cn-beijing.volces.com/api/plan/v3" },
            authToken = savedAuthToken,
            primaryModel = configRepository.getPrimaryModel(),
            ttsModel = configRepository.getTtsModel(),
            asrModel = configRepository.getAsrModel(),
            ttsBaseUrl = configRepository.getTtsBaseUrl(),
            ttsResourceId = configRepository.getTtsResourceId(),
            ttsSpeaker = configRepository.getTtsSpeaker(),
            asrResourceId = configRepository.getAsrResourceId(),
            asrWsUrl = configRepository.getAsrWsUrl(),
            // The setup page remains visible on every launch. Existing credentials
            // are prefilled and may be confirmed; changing either field requires a
            // fresh connection test before the user can continue.
            isConfigured = savedBaseUrl.isNotBlank() && savedAuthToken.length >= 10
        )
    )
    val state: StateFlow<ModelConfigGateState> = _state.asStateFlow()

    fun updateBaseUrl(value: String) {
        _state.update { it.copy(baseUrl = value, testSuccess = null, testResult = null, isConfigured = false) }
    }

    fun updateAuthToken(value: String) {
        _state.update { it.copy(authToken = value, testSuccess = null, testResult = null, isConfigured = false) }
    }

    fun updateTtsBaseUrl(value: String) {
        _state.update { it.copy(ttsBaseUrl = value, voiceTestSuccess = null, voiceTestResult = null) }
    }

    fun updateTtsResourceId(value: String) {
        _state.update { it.copy(ttsResourceId = value, voiceTestSuccess = null, voiceTestResult = null) }
    }

    fun updateTtsSpeaker(value: String) {
        _state.update { it.copy(ttsSpeaker = value, voiceTestSuccess = null, voiceTestResult = null) }
    }

    fun updateAsrResourceId(value: String) {
        _state.update { it.copy(asrResourceId = value, voiceTestSuccess = null, voiceTestResult = null) }
    }

    fun updateAsrWsUrl(value: String) {
        _state.update { it.copy(asrWsUrl = value, voiceTestSuccess = null, voiceTestResult = null) }
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
                    prompt = promptRegistry.render("PING", emptyMap()),
                    taskType = "PING"
                )
                if (result.isSuccess) {
                    val current = _state.value
                    _state.update {
                        it.copy(
                            isTesting = false,
                            testSuccess = true,
                            testResult = "Connection successful! Endpoint: $resolvedUrl\nLLM: ${current.primaryModel}\nTTS: ${current.ttsModel}\nASR: ${current.asrModel}",
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

    fun testVoice() {
        val s = _state.value
        if (s.authToken.isBlank()) return
        configRepository.setTtsBaseUrl(s.ttsBaseUrl.trim())
        configRepository.setTtsResourceId(s.ttsResourceId.trim())
        configRepository.setTtsSpeaker(s.ttsSpeaker.trim())
        configRepository.setAsrResourceId(s.asrResourceId.trim())
        configRepository.setAsrWsUrl(s.asrWsUrl.trim())
        _state.update { it.copy(isVoiceTesting = true, voiceTestSuccess = null, voiceTestResult = null) }
        viewModelScope.launch {
            val result = ttsRepository.getSpeech(
                text = "Hello! Lingo is ready to learn with you.",
                speed = 1.0f
            )
            val current = _state.value
            _state.update {
                if (result.isSuccess) {
                    it.copy(
                        isVoiceTesting = false,
                        voiceTestSuccess = true,
                        voiceTestResult = "Voice OK! TTS synthesized at ${current.ttsBaseUrl}\nSpeaker: ${current.ttsSpeaker}\nASR WS: ${current.asrWsUrl}"
                    )
                } else {
                    it.copy(
                        isVoiceTesting = false,
                        voiceTestSuccess = false,
                        voiceTestResult = "Voice test failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                    )
                }
            }
        }
    }
}
