package org.akj.lingo.learn.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.akj.lingo.learn.domain.model.GradeBand
import org.akj.lingo.learn.domain.model.PronunciationResult
import org.akj.lingo.learn.domain.repository.AsrRepository
import org.akj.lingo.learn.domain.repository.ConfigRepository
import org.akj.lingo.learn.domain.repository.LlmRepository
import org.akj.lingo.learn.domain.repository.TtsRepository
import org.akj.lingo.learn.domain.usecase.classifyError
import org.akj.lingo.learn.domain.usecase.userFacingError
import org.akj.lingo.learn.ui.learning.SystemTtsHelper
import org.akj.lingo.learn.ui.learning.VoiceRecorder
import org.json.JSONArray
import java.io.File
import javax.inject.Inject
import android.media.MediaPlayer
import android.content.Context

@HiltViewModel
class DiagnosisViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val llmRepository: LlmRepository,
    private val asrRepository: AsrRepository,
    private val voiceRecorder: VoiceRecorder,
    private val systemTtsHelper: SystemTtsHelper,
    private val ttsRepository: TtsRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _questions = MutableStateFlow<List<DiagnosticQuestion>>(emptyList())
    val questions: StateFlow<List<DiagnosticQuestion>> = _questions.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingStatus = MutableStateFlow("")
    val loadingStatus: StateFlow<String> = _loadingStatus.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _networkError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val networkError: SharedFlow<String> = _networkError.asSharedFlow()

    private var lastAudioFile: File? = null
    private var lastReferenceText: String = ""

    fun loadDiagnosticQuestions(grade: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            _loadingStatus.value = "Talking to Lingo's teacher…"
            // Rotate staged status messages while the LLM composes the quiz, so the
            // (legitimately 20-40s) generation never looks like a hang.
            val statusTicker = viewModelScope.launch {
                val stages = listOf(
                    "Talking to Lingo's teacher…",
                    "Picking words just for you…",
                    "Writing your questions…",
                    "Almost ready…"
                )
                var i = 1
                while (true) {
                    delay(8000)
                    _loadingStatus.value = stages[i % stages.size]
                    i++
                }
            }
            val band = GradeBand.fromGrade(grade)
            val prompt = """
                You are an English curriculum assessment expert for ${band.displayName} students.
                Generate 10 grade-appropriate English diagnostic questions for a student in $grade.

                Difficulty coefficient: ${band.difficultyCoefficient}
                Vocabulary range: ${band.defaultVocabularyRange}
                Max words per sentence: ${band.maxWordsPerSentence}

                IMPORTANT: Generate HIGHLY RANDOMIZED and DIVERSE questions. Do not use the same questions every time.
                Mix up the vocabulary, grammar points, and scenarios completely.

                Use ONLY these question types (the app renders exactly these):
                - LISTENING_EMOJI: "voicePrompt" is the word spoken, "options" are emoji choices, "correctAnswer" matches one option.
                - VOCABULARY: simple word choice, "options" are plain word choices, "correctAnswer" matches one option.
                - PHONICS: sound phonics choice, "options" are word choices, "correctAnswer" matches one option.
                - SORT_WORDS: child taps words in order; provide "wordsForSort" (the full bank, 3-5 words) and "correctAnswer" as the words space-separated in the correct order (e.g. "I see a cat").
                - SPEAK_ALOUD: pronunciation; "voicePrompt" is the sentence to read aloud, no options needed.
                - CHOOSE_LETTER: letter completion; "wordWithBlank" shows the word with one letter replaced by "_" (e.g. "h_istory"), "options" are single letters, "correctAnswer" is the missing letter.
                - LISTEN_AND_TYPE: "voicePrompt" is the dictated sentence, "correctAnswer" is the exact sentence, no options needed.

                Return raw valid JSON array ONLY (no markdown, no backticks). Each object uses only the fields its type needs:
                [
                  {
                    "id": 1,
                    "type": "LISTENING_EMOJI",
                    "title": "1. Listen and Choose",
                    "description": "Select the word you hear:",
                    "voicePrompt": "apple",
                    "options": ["🍎 Apple", "🍌 Banana", "🐱 Cat"],
                    "correctAnswer": "🍎 Apple"
                  },
                  {
                    "id": 2,
                    "type": "SORT_WORDS",
                    "title": "2. Arrange the Words",
                    "description": "Tap the words in the right order:",
                    "wordsForSort": ["I", "a", "see", "cat"],
                    "correctAnswer": "I see a cat"
                  }
                ]
            """.trimIndent()

            _loadError.value = null
            llmRepository.complete(prompt, taskType = "DIAGNOSIS", maxTokens = 1500)
                .onSuccess { json ->
                    val generatedList = parseQuestionsJson(json)
                    if (generatedList.isNotEmpty()) {
                        _questions.value = generatedList
                    } else {
                        _loadError.value = userFacingError(
                            classifyError(Exception("Could not parse the AI quiz. Please retry."))
                        )
                    }
                }
                .onFailure { e ->
                    _questions.value = emptyList()
                    _loadError.value = userFacingError(classifyError(e))
                }
            statusTicker.cancel()
            _loadingStatus.value = ""
            _isLoading.value = false
        }
    }

    /**
     * Sprint 20.5: speaks the given text via cloud TTS when configured, falling
     * back to Android system TTS otherwise — mirrors LearningViewModel.speakWithTts
     * so diagnosis questions are audible even without a TTS engine/voice pack.
     */
    fun speakPrompt(text: String) {
        val token = configRepository.getAuthToken()
        if (token.length < 10) {
            // Not configured — use offline system TTS.
            if (!systemTtsHelper.speak(text, 0.85f)) {
                showTtsUnavailableWarning()
            }
            return
        }
        viewModelScope.launch {
            val result = ttsRepository.getSpeech(text)
            if (result.isSuccess) {
                val file = result.getOrNull()
                if (file != null && file.exists()) {
                    playAudioFile(file)
                    return@launch
                }
            }
            // Cloud TTS failed / unavailable — degrade gracefully to system TTS.
            if (!systemTtsHelper.speak(text, 0.85f)) {
                showTtsUnavailableWarning()
            }
        }
    }

    /** Shows a warning when the device has no usable English voice. */
    private fun showTtsUnavailableWarning() {
        android.widget.Toast.makeText(
            context,
            "⚠️ English voice engine is not ready. Please configure your API Key or install a TTS voice pack.",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    /** Plays a TTS audio file via MediaPlayer (fire-and-forget). */
    private fun playAudioFile(file: File) {
        try {
            val player = MediaPlayer()
            player.setDataSource(file.absolutePath)
            player.setOnPreparedListener { it.start() }
            player.setOnCompletionListener { it.release() }
            player.prepareAsync()
        } catch (_: Exception) {
            // Ignore playback errors; caller already degraded to system TTS on failure.
        }
    }

    fun startRecording() {
        lastAudioFile = null
        _recordingState.value = RecordingState.RECORDING
        voiceRecorder.startRecording()
    }

    fun stopAndEvaluate(referenceText: String) {
        lastReferenceText = referenceText
        val file = voiceRecorder.stopRecording()
        lastAudioFile = file

        if (file == null) {
            _recordingState.value = RecordingState.FAILED("Could not access microphone")
            return
        }

        if (!isAudioLongEnough(file)) {
            file.delete()
            lastAudioFile = null
            _recordingState.value = RecordingState.TOO_SHORT
            return
        }

        _recordingState.value = RecordingState.EVALUATING
        evaluateWithRetry(file, referenceText, retriesLeft = 2)
    }

    fun retry() {
        val file = lastAudioFile
        val text = lastReferenceText
        if (file != null && text.isNotBlank()) {
            _recordingState.value = RecordingState.EVALUATING
            evaluateWithRetry(file, text, retriesLeft = 2)
        }
    }

    private fun evaluateWithRetry(file: File, referenceText: String, retriesLeft: Int) {
        viewModelScope.launch {
            try {
                val result = asrRepository.evaluatePronunciation(file, referenceText)
                result.onSuccess { pronunciationResult ->
                    _recordingState.value = RecordingState.COMPLETED(pronunciationResult)
                }.onFailure {
                    if (retriesLeft > 0 && isNetworkError(it)) {
                        delay(1000)
                        evaluateWithRetry(file, referenceText, retriesLeft - 1)
                    } else if (isNetworkError(it)) {
                        _recordingState.value = RecordingState.NETWORK_ERROR
                        _networkError.tryEmit(userFacingError(classifyError(it)))
                    } else {
                        val fallback = asrRepository.getOfflineFallbackResult(referenceText)
                        _recordingState.value = RecordingState.COMPLETED(fallback)
                    }
                }
            } catch (e: Exception) {
                if (retriesLeft > 0 && isNetworkError(e)) {
                    delay(1000)
                    evaluateWithRetry(file, referenceText, retriesLeft - 1)
                } else if (isNetworkError(e)) {
                    _recordingState.value = RecordingState.NETWORK_ERROR
                    _networkError.tryEmit(userFacingError(classifyError(e)))
                } else {
                    val fallback = asrRepository.getOfflineFallbackResult(referenceText)
                    _recordingState.value = RecordingState.COMPLETED(fallback)
                }
            }
        }
    }

    private fun isNetworkError(e: Throwable): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("timeout") || msg.contains("unable to resolve") ||
               msg.contains("network") || msg.contains("connect") ||
               msg.contains("eof") || msg.contains("socket")
    }

    private fun isAudioLongEnough(file: File): Boolean {
        return try {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
            }
            val durationMs = player.duration
            player.release()
            durationMs >= 1000
        } catch (e: Exception) {
            true
        }
    }

    suspend fun evaluateSpeaking(audioFile: File, referenceText: String): PronunciationResult {
        val result = asrRepository.evaluatePronunciation(audioFile, referenceText)
        return result.getOrElse {
            asrRepository.getOfflineFallbackResult(referenceText)
        }
    }

    private fun parseQuestionsJson(jsonStr: String?): List<DiagnosticQuestion> {
        if (jsonStr.isNullOfBlank()) return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<DiagnosticQuestion>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val qType = parseQuestionType(obj.optString("type", "VOCABULARY"))

                val optionsArray = obj.optJSONArray("options")
                val optionsList = mutableListOf<String>()
                if (optionsArray != null) {
                    for (j in 0 until optionsArray.length()) {
                        optionsList.add(optionsArray.getString(j))
                    }
                }

                val wordsArray = obj.optJSONArray("wordsForSort")
                val wordsList = mutableListOf<String>()
                if (wordsArray != null) {
                    for (j in 0 until wordsArray.length()) {
                        wordsList.add(wordsArray.getString(j))
                    }
                }
                // SORT_WORDS without an explicit bank reuses the options as the sort bank.
                val sortWords = wordsList.ifEmpty { optionsList }

                list.add(
                    DiagnosticQuestion(
                        id = obj.optInt("id", i + 1),
                        type = qType,
                        title = obj.optString("title", "${i + 1}. Question"),
                        description = obj.optString("description", "Choose correct answer:"),
                        voicePrompt = if (obj.isNull("voicePrompt")) null else obj.optString("voicePrompt", ""),
                        options = optionsList,
                        correctAnswer = obj.optString("correctAnswer", ""),
                        wordsForSort = sortWords,
                        wordWithBlank = if (obj.isNull("wordWithBlank")) null else obj.optString("wordWithBlank", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Maps LLM-emitted type names to the app enum, tolerating synonyms. */
    private fun parseQuestionType(raw: String): QuestionType {
        val normalized = raw.trim().uppercase().replace(" ", "_")
        return when {
            normalized.contains("SORT") || normalized.contains("ARRANGE") -> QuestionType.SORT_WORDS
            normalized.contains("LETTER") || normalized.contains("BLANK") || normalized.contains("COMPLETE") -> QuestionType.CHOOSE_LETTER
            normalized.contains("LISTEN") && normalized.contains("TYPE") || normalized.contains("DICTATION") -> QuestionType.LISTEN_AND_TYPE
            normalized.contains("SPEAK") || normalized.contains("SAY") || normalized.contains("PRONOUNC") -> QuestionType.SPEAK_ALOUD
            normalized.contains("PHONICS") || normalized.contains("SOUND") -> QuestionType.PHONICS
            normalized.contains("EMOJI") || normalized.contains("LISTEN") -> QuestionType.LISTENING_EMOJI
            else -> try { QuestionType.valueOf(raw.trim().uppercase()) } catch (e: Exception) { QuestionType.VOCABULARY }
        }
    }

    private fun String?.isNullOfBlank(): Boolean = this == null || this.trim().isEmpty()

}

sealed class RecordingState {
    data object IDLE : RecordingState()
    data object RECORDING : RecordingState()
    data object EVALUATING : RecordingState()
    data object TOO_SHORT : RecordingState()
    data class COMPLETED(val result: PronunciationResult) : RecordingState()
    data class FAILED(val message: String) : RecordingState()
    data object NETWORK_ERROR : RecordingState()
}
