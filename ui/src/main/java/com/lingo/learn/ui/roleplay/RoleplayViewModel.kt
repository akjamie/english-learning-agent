package org.akj.lingo.learn.ui.roleplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.akj.lingo.learn.domain.model.ChatMessage
import org.akj.lingo.learn.domain.model.ConversationMessage
import org.akj.lingo.learn.domain.model.RoleplayScenario
import org.akj.lingo.learn.domain.repository.AsrRepository
import org.akj.lingo.learn.domain.repository.ConversationRepository
import org.akj.lingo.learn.domain.repository.LlmRepository
import org.akj.lingo.learn.domain.repository.TtsRepository
import org.akj.lingo.learn.domain.usecase.RoleplayScenarioBank
import org.akj.lingo.learn.ui.learning.SystemTtsHelper
import org.akj.lingo.learn.ui.learning.VoiceRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * Sprint 11 — Scenario-based roleplay companion.
 *
 * Supports multiple curated scenarios (zoo / restaurant / school / travel),
 * both voice (ASR) and text input, conversation-history persistence, and
 * optional LLM enrichment (offline-safe curated scripts always available).
 */
@HiltViewModel
class RoleplayViewModel @Inject constructor(
    private val asrRepository: AsrRepository,
    private val llmRepository: LlmRepository,
    private val ttsRepository: TtsRepository,
    private val voiceRecorder: VoiceRecorder,
    private val scenarioBank: RoleplayScenarioBank,
    private val conversationRepository: ConversationRepository,
    private val systemTtsHelper: SystemTtsHelper
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _scenario = MutableStateFlow<RoleplayScenario?>(null)
    val scenario: StateFlow<RoleplayScenario?> = _scenario.asStateFlow()

    private val _scenarios = MutableStateFlow<List<RoleplayScenario>>(scenarioBank.curatedScenarios())
    val scenarios: StateFlow<List<RoleplayScenario>> = _scenarios.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioToPlay = MutableStateFlow<File?>(null)
    val audioToPlay: StateFlow<File?> = _audioToPlay.asStateFlow()

    private var currentScenarioId: String = "zoo"

    init {
        selectScenario("zoo")
    }

    /** Selects a scenario, restoring its last conversation history. */
    fun selectScenario(id: String) {
        val selected = scenarioBank.getScenario(id)
        currentScenarioId = selected.id
        _scenario.value = selected
        _messages.value = scenarioBank.initialMessages(selected)
        _audioToPlay.value = null
        viewModelScope.launch {
            val history = conversationRepository.getRecentMessages(selected.id, 12)
            if (history.isNotEmpty()) {
                val chatHistory = history.map { ChatMessage(role = it.role, content = it.content) }
                _messages.value = chatHistory.ifEmpty { scenarioBank.initialMessages(selected) }
            }
        }
    }

    /** Starts a fresh conversation for the current scenario (clears persisted history). */
    fun resetConversation() {
        val selected = _scenario.value ?: return
        viewModelScope.launch {
            conversationRepository.clearScenario(selected.id)
            _messages.value = scenarioBank.initialMessages(selected)
        }
    }

    // ---- Voice input ----

    fun startRecording() {
        if (_isRecording.value) return
        voiceRecorder.startRecording()
        _isRecording.value = true
        _audioToPlay.value = null
    }

    fun stopRecordingAndSend() {
        if (!_isRecording.value) return
        val audioFile = voiceRecorder.stopRecording()
        _isRecording.value = false
        if (audioFile != null && audioFile.exists()) {
            processUserAudio(audioFile)
        }
    }

    private fun processUserAudio(audioFile: File) {
        viewModelScope.launch {
            _isThinking.value = true
            val asrResult = asrRepository.transcribeAudio(audioFile)
            val userText = asrResult.getOrNull()
            _isThinking.value = false
            if (!userText.isNullOrBlank()) {
                sendUserMessage(userText)
            }
        }
    }

    // ---- Text input ----

    /** Sends a typed message (text-input mode, works without a microphone). */
    fun sendUserMessage(userText: String) {
        if (userText.isBlank()) return
        viewModelScope.launch {
            val currentMessages = _messages.value.toMutableList()
            currentMessages.add(ChatMessage(role = "user", content = userText.trim()))
            _messages.value = currentMessages
            persist(currentScenarioId, "user", userText.trim())

            _isThinking.value = true
            val llmResult = llmRepository.chat(currentMessages, taskType = "ROLEPLAY", maxTokens = 100)
            val assistantText = llmResult.getOrNull() ?: fallbackReply()
            _isThinking.value = false

            currentMessages.add(ChatMessage(role = "assistant", content = assistantText))
            _messages.value = currentMessages
            persist(currentScenarioId, "assistant", assistantText)

            speakAssistant(assistantText)
        }
    }

    private fun speakAssistant(text: String) {
        _isSpeaking.value = true
        viewModelScope.launch {
            val ttsResult = ttsRepository.getSpeech(text)
            if (ttsResult.isSuccess) {
                _audioToPlay.value = ttsResult.getOrNull()
            } else {
                // Sprint 15: fall back to system TTS when cloud TTS is unavailable (offline / error)
                systemTtsHelper.speak(text)
                _isSpeaking.value = false
            }
        }
    }

    private suspend fun persist(scenarioId: String, role: String, content: String) {
        conversationRepository.insertMessage(
            ConversationMessage(
                id = UUID.randomUUID().toString(),
                scenarioId = scenarioId,
                role = role,
                content = content,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    /** Offline-friendly fallback when the LLM is unavailable. */
    private fun fallbackReply(): String {
        val lastUser = _messages.value.lastOrNull { it.role == "user" }?.content ?: ""
        return when {
            lastUser.contains("lion", ignoreCase = true) ||
                lastUser.contains("animal", ignoreCase = true) -> "The lion is big and strong. What else do you see?"
            lastUser.contains("menu", ignoreCase = true) ||
                lastUser.contains("food", ignoreCase = true) -> "Good choice! The food here is delicious. Anything else?"
            lastUser.contains("class", ignoreCase = true) ||
                lastUser.contains("teacher", ignoreCase = true) -> "I love that class too! Do you learn new words?"
            lastUser.contains("ticket", ignoreCase = true) ||
                lastUser.contains("plane", ignoreCase = true) -> "Great! Let's grab our tickets and fly somewhere fun!"
            else -> "That sounds fun! Tell me more!"
        }
    }

    fun onAudioPlaybackComplete() {
        _isSpeaking.value = false
        _audioToPlay.value = null
    }
}
