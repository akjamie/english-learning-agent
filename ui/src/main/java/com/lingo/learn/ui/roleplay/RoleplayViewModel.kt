package org.akj.lingo.learn.ui.roleplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.akj.lingo.learn.domain.model.ChatMessage
import org.akj.lingo.learn.domain.repository.AsrRepository
import org.akj.lingo.learn.domain.repository.LlmRepository
import org.akj.lingo.learn.domain.repository.TtsRepository
import org.akj.lingo.learn.ui.learning.VoiceRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RoleplayViewModel @Inject constructor(
    private val asrRepository: AsrRepository,
    private val llmRepository: LlmRepository,
    private val ttsRepository: TtsRepository,
    private val voiceRecorder: VoiceRecorder
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioToPlay = MutableStateFlow<File?>(null)
    val audioToPlay: StateFlow<File?> = _audioToPlay.asStateFlow()

    init {
        // Initialize with a scenario
        val systemPrompt = """
            You are Lingo Fox, a friendly shopkeeper at an ice cream shop. 
            The user is a child coming to buy ice cream. 
            Keep your answers very short (1-2 sentences). 
            Use simple English vocabulary suitable for a 4th-grade student. 
            Always be encouraging and cheerful! 
            Target words to naturally include if possible: [ice cream, chocolate, strawberry, please, thank you].
        """.trimIndent()

        _messages.value = listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "assistant", content = "Hello! Welcome to Lingo's Ice Cream Shop. What would you like today?")
        )
    }

    fun startRecording() {
        if (_isRecording.value) return
        voiceRecorder.startRecording()
        _isRecording.value = true
        _audioToPlay.value = null // Stop any previous audio
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

            // 1. Transcribe Audio (ASR)
            val asrResult = asrRepository.transcribeAudio(audioFile)
            val userText = asrResult.getOrNull()

            if (!userText.isNullOrBlank()) {
                val currentMessages = _messages.value.toMutableList()
                currentMessages.add(ChatMessage(role = "user", content = userText))
                _messages.value = currentMessages

                // 2. Get LLM Response
                val llmResult = llmRepository.chat(currentMessages, taskType = "ROLEPLAY", maxTokens = 100)
                val assistantText = llmResult.getOrNull() ?: "Sorry, I didn't catch that. Can you try again?"

                currentMessages.add(ChatMessage(role = "assistant", content = assistantText))
                _messages.value = currentMessages

                // 3. Generate Audio (TTS)
                _isThinking.value = false
                _isSpeaking.value = true
                val ttsResult = ttsRepository.getSpeech(assistantText)
                _audioToPlay.value = ttsResult.getOrNull()
            } else {
                _isThinking.value = false
            }
        }
    }

    fun onAudioPlaybackComplete() {
        _isSpeaking.value = false
        _audioToPlay.value = null
    }
}
