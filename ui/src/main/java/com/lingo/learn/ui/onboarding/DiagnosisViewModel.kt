package org.akj.lingo.learn.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import org.akj.lingo.learn.domain.repository.LlmRepository
import org.akj.lingo.learn.domain.usecase.classifyError
import org.akj.lingo.learn.domain.usecase.userFacingError
import org.akj.lingo.learn.ui.learning.VoiceRecorder
import org.json.JSONArray
import java.io.File
import javax.inject.Inject
import android.media.MediaPlayer

@HiltViewModel
class DiagnosisViewModel @Inject constructor(
    private val llmRepository: LlmRepository,
    private val asrRepository: AsrRepository,
    private val voiceRecorder: VoiceRecorder
) : ViewModel() {

    private val _questions = MutableStateFlow<List<DiagnosticQuestion>>(emptyList())
    val questions: StateFlow<List<DiagnosticQuestion>> = _questions.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _networkError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val networkError: SharedFlow<String> = _networkError.asSharedFlow()

    private var lastAudioFile: File? = null
    private var lastReferenceText: String = ""

    fun loadDiagnosticQuestions(grade: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val band = GradeBand.fromGrade(grade)
            val prompt = """
                You are an English curriculum assessment expert for ${band.displayName} students.
                Generate 10 grade-appropriate English diagnostic questions for a student in $grade.

                Difficulty coefficient: ${band.difficultyCoefficient}
                Vocabulary range: ${band.defaultVocabularyRange}
                Max words per sentence: ${band.maxWordsPerSentence}

                IMPORTANT: Generate HIGHLY RANDOMIZED and DIVERSE questions. Do not use the same questions every time.
                Mix up the vocabulary, grammar points, and scenarios completely.
                
                Include question types: LISTENING_EMOJI, VOCABULARY, PHONICS, SORT_WORDS, SPEAK_ALOUD.
                Return raw valid JSON array ONLY (no markdown, no backticks):
                [
                  {
                    "id": 1,
                    "type": "LISTENING_EMOJI",
                    "title": "1. Listen and Choose",
                    "description": "Select the word you hear:",
                    "voicePrompt": "apple",
                    "options": ["🍎 Apple", "🍌 Banana", "🐱 Cat"],
                    "correctAnswer": "🍎 Apple"
                  }
                ]
            """.trimIndent()

            val result = llmRepository.complete(prompt, taskType = "DIAGNOSIS", maxTokens = 1500)
            val generatedList = parseQuestionsJson(result.getOrNull())

            if (generatedList.isNotEmpty()) {
                _questions.value = generatedList
            } else {
                _questions.value = getFallbackQuestionsForGrade(grade)
            }
            _isLoading.value = false
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
                val typeStr = obj.optString("type", "VOCABULARY")
                val qType = try { QuestionType.valueOf(typeStr) } catch (e: Exception) { QuestionType.VOCABULARY }

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

                list.add(
                    DiagnosticQuestion(
                        id = obj.optInt("id", i + 1),
                        type = qType,
                        title = obj.optString("title", "${i + 1}. Question"),
                        description = obj.optString("description", "Choose correct answer:"),
                        voicePrompt = if (obj.isNull("voicePrompt")) null else obj.optString("voicePrompt", ""),
                        options = optionsList,
                        correctAnswer = obj.optString("correctAnswer", ""),
                        wordsForSort = wordsList
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun String?.isNullOfBlank(): Boolean = this == null || this.trim().isEmpty()

    private fun getFallbackQuestionsForGrade(grade: String): List<DiagnosticQuestion> {
        val isLowerGrade = grade.contains("1") || grade.contains("2") || grade.contains("一") || grade.contains("二")
        val pool = if (isLowerGrade) {
            listOf(
                DiagnosticQuestion(1, QuestionType.LISTENING_EMOJI, "Listen & Pick", "Select the word you hear:", voicePrompt = "dog", options = listOf("🐶 Dog", "🐱 Cat", "🐰 Rabbit"), correctAnswer = "🐶 Dog"),
                DiagnosticQuestion(2, QuestionType.VOCABULARY, "Color Match", "Which color is Red?", options = listOf("🔴 Red", "🔵 Blue", "🟡 Yellow"), correctAnswer = "🔴 Red"),
                DiagnosticQuestion(3, QuestionType.PHONICS, "Letter Sound", "Which word starts with /b/?", options = listOf("Ball", "Cat", "Dog"), correctAnswer = "Ball"),
                DiagnosticQuestion(4, QuestionType.SORT_WORDS, "Sentence Building", "Arrange words into a sentence:", wordsForSort = listOf("big", "is", "It"), correctAnswer = "It is big"),
                DiagnosticQuestion(5, QuestionType.VOCABULARY, "Animal Name", "Select 'Cat':", options = listOf("Cat", "Duck", "Fish"), correctAnswer = "Cat"),
                DiagnosticQuestion(6, QuestionType.LISTENING_EMOJI, "Listen & Pick", "Select the fruit:", voicePrompt = "banana", options = listOf("🍎 Apple", "🍌 Banana", "🍐 Pear"), correctAnswer = "🍌 Banana"),
                DiagnosticQuestion(7, QuestionType.VOCABULARY, "Number Word", "How many? 3", options = listOf("Three", "One", "Two"), correctAnswer = "Three"),
                DiagnosticQuestion(8, QuestionType.SPEAK_ALOUD, "Read Aloud", "Read aloud:", voicePrompt = "Good morning!"),
                DiagnosticQuestion(9, QuestionType.VOCABULARY, "Action Word", "What do you do with a book?", options = listOf("Read", "Eat", "Run"), correctAnswer = "Read"),
                DiagnosticQuestion(10, QuestionType.PHONICS, "Rhyming Words", "Which word rhymes with 'Cat'?", options = listOf("Hat", "Dog", "Pen"), correctAnswer = "Hat"),
                DiagnosticQuestion(11, QuestionType.SORT_WORDS, "Sentence Building", "Arrange into sentence:", wordsForSort = listOf("a", "am", "I", "boy"), correctAnswer = "I am a boy"),
                DiagnosticQuestion(12, QuestionType.LISTENING_EMOJI, "Listen & Pick", "Select the vehicle:", voicePrompt = "car", options = listOf("🚗 Car", "🚲 Bike", "✈️ Plane"), correctAnswer = "🚗 Car")
            )
        } else {
            listOf(
                DiagnosticQuestion(1, QuestionType.LISTENING_EMOJI, "Listen and Choose", "Select the word you hear:", voicePrompt = "apple", options = listOf("🍎 Apple", "🍌 Banana", "🐱 Cat"), correctAnswer = "🍎 Apple"),
                DiagnosticQuestion(2, QuestionType.VOCABULARY, "Opposite Word Select", "Choose opposite of 'Hot':", options = listOf("Cold", "Warm", "Big", "Dry"), correctAnswer = "Cold"),
                DiagnosticQuestion(3, QuestionType.PHONICS, "Phonics & Sound", "Which word starts with /p/?", options = listOf("Pig", "Big", "Dig", "Wig"), correctAnswer = "Pig"),
                DiagnosticQuestion(4, QuestionType.SORT_WORDS, "Sentence Ordering", "Arrange into sentence:", wordsForSort = listOf("like", "apples", "I"), correctAnswer = "I like apples"),
                DiagnosticQuestion(5, QuestionType.VOCABULARY, "Grammar & Tense", "Select correct word: 'She ___ to school every day.'", options = listOf("walks", "walked", "walking", "walk"), correctAnswer = "walks"),
                DiagnosticQuestion(6, QuestionType.LISTENING_EMOJI, "Listening Comprehension", "Select the animal:", voicePrompt = "cat", options = listOf("🐶 Dog", "🐱 Cat", "🐰 Rabbit"), correctAnswer = "🐱 Cat"),
                DiagnosticQuestion(7, QuestionType.VOCABULARY, "Contextual Antonym", "The rabbit is fast, but the turtle is ___:", options = listOf("slow", "quick", "tall", "heavy"), correctAnswer = "slow"),
                DiagnosticQuestion(8, QuestionType.VOCABULARY, "Idiom", "What does 'A piece of cake' mean?", options = listOf("Very easy", "Delicious dessert", "Hard problem"), correctAnswer = "Very easy"),
                DiagnosticQuestion(9, QuestionType.SORT_WORDS, "Sentence Ordering", "Arrange into sentence:", wordsForSort = listOf("play", "on", "We", "football", "Sunday"), correctAnswer = "We play football on Sunday"),
                DiagnosticQuestion(10, QuestionType.SPEAK_ALOUD, "Speak Aloud Challenge", "Read aloud:", voicePrompt = "Practice makes perfect every day."),
                DiagnosticQuestion(11, QuestionType.VOCABULARY, "Grammar", "He ___ a new car.", options = listOf("has", "have", "having"), correctAnswer = "has"),
                DiagnosticQuestion(12, QuestionType.PHONICS, "Vowel Sounds", "Which word has a short 'a' sound?", options = listOf("Bat", "Cake", "Rain"), correctAnswer = "Bat")
            )
        }
        
        return pool.shuffled().take(10).mapIndexed { index, q -> 
            q.copy(id = index + 1, title = "${index + 1}. ${q.title}") 
        }
    }
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
