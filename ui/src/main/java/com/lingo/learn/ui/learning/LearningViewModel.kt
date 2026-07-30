package org.akj.lingo.learn.ui.learning

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import org.akj.lingo.learn.domain.model.*
import org.akj.lingo.learn.domain.repository.AsrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.akj.lingo.learn.ui.StreakPrefs
import javax.inject.Inject

/** Top-level stages of the daily learning flow. */
enum class LearningStage {
    PRE_TEACH,  // Stage 0: Vocabulary warm-up (ESA Engage)
    IMMERSION,  // Stage 1: immersive audio import with subtitle highlighting
    PRACTICE,   // Stage 2: read-along + consolidation mini-games
    QUIZ,       // Stage 3: daily micro-quiz
    COMPLETE    // Completion page with achievement summary
}

/** Sub-stages within the PRACTICE stage. */
enum class PracticePhase { READ_ALONG, GAME }

data class ReadAlongState(
    val currentIndex: Int = 0,
    val isRecording: Boolean = false,
    val isEvaluating: Boolean = false,
    val isPlayingDemo: Boolean = false,
    val isPlayingSelf: Boolean = false,
    val tempRecordingUri: String? = null,
    val result: PronunciationResult? = null,
    val completedCount: Int = 0,
    val cumulativeScore: Int = 0,
    val evaluationsCount: Int = 0
)

data class GameState(
    val currentIndex: Int = 0,
    val score: Int = 0,
    val combo: Int = 0,
    val lastAnswerCorrect: Boolean? = null,
    val showComboEffect: Boolean = false
)

data class QuizState(
    val currentIndex: Int = 0,
    val selectedAnswers: MutableList<Int> = mutableListOf(),
    val score: Int = 0,
    val showResult: Boolean = false,
    val lastAnswerCorrect: Boolean? = null,
    val hintLevel: Int = 0,
    val dynamicHint: String? = null,
    val isGeneratingHint: Boolean = false
)

/**
 * Core ViewModel for the Sprint 2 daily learning flow.
 *
 * Orchestrates all three stages (immersion -> practice -> quiz -> complete),
 * managing audio playback simulation, voice recording with ASR evaluation,
 * mini-game scoring, quiz progression, and final achievement summary.
 *
 * @param asrRepository ASR pronunciation evaluation (with offline fallback)
 * @param voiceRecorder Android MediaRecorder wrapper for capturing child's voice
 * @param systemTtsHelper System TTS fallback for audio playback without API keys
 */
@HiltViewModel
class LearningViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val asrRepository: AsrRepository,
    private val voiceRecorder: VoiceRecorder,
    private val systemTtsHelper: SystemTtsHelper,
    private val weeklyPlanRepository: org.akj.lingo.learn.domain.repository.WeeklyPlanRepository,
    private val learningRecordRepository: org.akj.lingo.learn.domain.repository.LearningRecordRepository,
    private val errorBookRepository: org.akj.lingo.learn.domain.repository.ErrorBookRepository,
    private val llmRepository: org.akj.lingo.learn.domain.repository.LlmRepository
) : ViewModel() {

    private val _stage = MutableStateFlow(LearningStage.PRE_TEACH)
    val stage: StateFlow<LearningStage> = _stage.asStateFlow()

    private val _practicePhase = MutableStateFlow(PracticePhase.READ_ALONG)
    val practicePhase: StateFlow<PracticePhase> = _practicePhase.asStateFlow()

    val audioPlayer = AudioPlayerController(viewModelScope)

    private val _readAlongState = MutableStateFlow(ReadAlongState())
    val readAlongState: StateFlow<ReadAlongState> = _readAlongState.asStateFlow()

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _quizState = MutableStateFlow(QuizState())
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()

    private val _session = MutableStateFlow(SampleLearningContent.createSchoolLifeSession())
    val session: StateFlow<LearningSession> = _session.asStateFlow()

    private val _summary = MutableStateFlow<SessionSummary?>(null)
    val summary: StateFlow<SessionSummary?> = _summary.asStateFlow()

    private var currentGrade: String = "Grade 4"

    fun setGrade(grade: String) {
        currentGrade = grade
        viewModelScope.launch {
            try {
                val loadedSession = weeklyPlanRepository.getCachedLearningSession(1, currentGrade)
                _session.value = loadedSession
                audioPlayer.loadSubtitles(loadedSession.subtitleLines)
            } catch (e: Exception) {
                audioPlayer.loadSubtitles(_session.value.subtitleLines)
            }
        }
    }

    init {
        setGrade(currentGrade)
        // Speak subtitle lines via TTS as they become active during immersion playback
        viewModelScope.launch {
            audioPlayer.state
                .map { it.currentSubtitleIndex }
                .distinctUntilChanged()
                .collect { index ->
                    if (index >= 0) {
                        val line = _session.value.subtitleLines.getOrNull(index)
                        if (line != null) {
                            val speed = audioPlayer.state.value.speed
                            systemTtsHelper.speak(line.text, speed * 0.85f)
                        }
                    }
                }
        }
    }

    //region Stage 1: Immersion

    fun toggleAudioPlayback() = audioPlayer.togglePlayPause()

    fun seekAudioTo(positionMs: Long) = audioPlayer.seekTo(positionMs)

    fun setAudioSpeed(speed: Float) = audioPlayer.setSpeed(speed)

    fun dismissNewWord(word: String) = audioPlayer.dismissNewWord(word)

    /** Navigates back to the previous learning stage. */
    fun goToPreviousStage() {
        val current = _stage.value
        if (current.ordinal > 0) {
            val prev = LearningStage.entries[current.ordinal - 1]
            _stage.value = prev
            if (prev == LearningStage.IMMERSION) {
                audioPlayer.reset()
            } else if (prev == LearningStage.PRACTICE) {
                _practicePhase.value = PracticePhase.GAME
            }
        }
    }

    /** Speaks a word using system TTS (for new-word popup card pronunciation). */
    fun speakWord(word: String) {
        systemTtsHelper.speak(word, 0.85f)
    }

    /** Advances from Stage 0 (pre-teach) to Stage 1 (immersion). */
    fun proceedToImmersion() {
        _stage.value = LearningStage.IMMERSION
    }

    /** Advances from Stage 1 (immersion) to Stage 2 (practice: read-along). */
    fun proceedToPractice() {
        audioPlayer.pause()
        _stage.value = LearningStage.PRACTICE
        _practicePhase.value = PracticePhase.READ_ALONG
    }

    //endregion

    //region Stage 2a: Read-along

    /** Starts recording the child's voice for the current read-along sentence. */
    fun startRecording() {
        voiceRecorder.startRecording()
        _readAlongState.value = _readAlongState.value.copy(isRecording = true, result = null)
    }

    /** Stops recording and triggers ASR pronunciation evaluation. */
    fun stopRecording() {
        val audioFile = voiceRecorder.stopRecording()
        _readAlongState.value = _readAlongState.value.copy(
            isRecording = false,
            isEvaluating = true,
            tempRecordingUri = audioFile?.absolutePath
        )

        val referenceText = _session.value.readAlongSentences[_readAlongState.value.currentIndex].text

        viewModelScope.launch {
            val result = asrRepository.evaluatePronunciation(
                audioFile ?: java.io.File(""),
                referenceText
            )
            val pronunciationResult = result.getOrElse {
                asrRepository.getOfflineFallbackResult(referenceText)
            }
            _readAlongState.value = _readAlongState.value.copy(
                isEvaluating = false,
                result = pronunciationResult,
                completedCount = _readAlongState.value.completedCount + 1,
                cumulativeScore = _readAlongState.value.cumulativeScore + pronunciationResult.overallScore,
                evaluationsCount = _readAlongState.value.evaluationsCount + 1
            )
        }
    }

    fun playReadAlongDemo() {
        val sentence = _session.value.readAlongSentences[_readAlongState.value.currentIndex]
        sentence.audioPath?.let {
            _readAlongState.value = _readAlongState.value.copy(isPlayingDemo = true)
            // Ideally trigger AudioPlayerController or MediaPlayer to play the TTS file
            systemTtsHelper.speak(sentence.text, 0.9f)
            // Reset state after a delay or on completion callback (mocked here)
            _readAlongState.value = _readAlongState.value.copy(isPlayingDemo = false)
        } ?: run {
            systemTtsHelper.speak(sentence.text, 0.9f)
        }
    }

    fun playReadAlongSelf() {
        val currentUri = _readAlongState.value.tempRecordingUri
        if (currentUri != null) {
            _readAlongState.value = _readAlongState.value.copy(isPlayingSelf = true)
            // Use Android MediaPlayer to play the recorded file
            val mp = android.media.MediaPlayer()
            try {
                mp.setDataSource(currentUri)
                mp.prepare()
                mp.start()
                mp.setOnCompletionListener {
                    it.release()
                    _readAlongState.value = _readAlongState.value.copy(isPlayingSelf = false)
                }
            } catch (e: Exception) {
                _readAlongState.value = _readAlongState.value.copy(isPlayingSelf = false)
            }
        }
    }

    /** Retries the current read-along sentence (clears previous result). */
    fun retryReadAlong() {
        _readAlongState.value = _readAlongState.value.copy(result = null)
    }

    /** Advances to the next read-along sentence, or transitions to mini-games. */
    fun nextReadAlongSentence() {
        val current = _readAlongState.value
        val total = _session.value.readAlongSentences.size

        if (current.currentIndex < total - 1) {
            _readAlongState.value = current.copy(
                currentIndex = current.currentIndex + 1,
                result = null
            )
        } else {
            // All read-along sentences completed; move to mini-games
            _practicePhase.value = PracticePhase.GAME
        }
    }

    //endregion

    //region Stage 2b: Consolidation mini-games

    /**
     * Submits an answer for the current game question and updates score/combo.
     * Triggers combo effect when 3 consecutive correct answers are achieved.
     */
    fun submitGameAnswer(selectedIndex: Int) {
        val current = _gameState.value
        val question = _session.value.gameQuestions[current.currentIndex]
        val isCorrect = selectedIndex == question.correctIndex

        val newCombo = if (isCorrect) current.combo + 1 else 0
        val newScore = current.score + if (isCorrect) 10 else 0
        val showCombo = isCorrect && newCombo >= 3

        _gameState.value = current.copy(
            score = newScore,
            combo = newCombo,
            lastAnswerCorrect = isCorrect,
            showComboEffect = showCombo
        )
    }

    /** Advances to the next game question, or transitions to quiz. */
    fun nextGameQuestion() {
        val current = _gameState.value
        val total = _session.value.gameQuestions.size

        _gameState.value = current.copy(
            currentIndex = current.currentIndex + 1,
            lastAnswerCorrect = null,
            showComboEffect = false
        )

        if (current.currentIndex >= total - 1) {
            // All game questions completed; move to quiz
            _stage.value = LearningStage.QUIZ
        }
    }

    /** Plays the audio for a listen-choose-image game question via system TTS. */
    fun playGameAudio(text: String) {
        systemTtsHelper.speak(text, 0.85f)
    }

    //endregion

    //region Stage 3: Daily Quiz

    /**
     * Submits an answer for the current quiz question and records correctness.
     * For READ_ALOUD type questions, triggers the recording flow instead.
     */
    fun submitQuizAnswer(selectedIndex: Int) {
        val current = _quizState.value
        val question = _session.value.quizQuestions[current.currentIndex]
        val isCorrect = selectedIndex == question.correctIndex

        current.selectedAnswers.add(selectedIndex)
        val newScore = current.score + if (isCorrect) 1 else 0

        _quizState.value = current.copy(
            score = newScore,
            lastAnswerCorrect = isCorrect
        )

        val word = question.audioText ?: question.options.getOrNull(question.correctIndex) ?: "vocab_${question.id}"
        viewModelScope.launch {
            if (!isCorrect) {
                errorBookRepository.upsertError(word, "QUIZ_WRONG_ANSWER", question.type.name)
            } else if (question.isFromErrorBook) {
                errorBookRepository.markCorrect(word)
            }
        }
    }

    fun incrementHint() {
        val current = _quizState.value
        val question = _session.value.quizQuestions.getOrNull(current.currentIndex) ?: return

        if (current.hintLevel == 0 && current.dynamicHint == null) {
            _quizState.value = current.copy(isGeneratingHint = true)
            viewModelScope.launch {
                val correctAnswer = question.options.getOrNull(question.correctIndex) ?: question.audioText ?: ""
                val prompt = "You are Lingo Fox, a friendly English tutor. The child is stuck on a quiz question: '${question.question}'. The correct answer is: '$correctAnswer'. Give a very short, simple, 1-sentence hint that guides them but doesn't give away the direct answer."
                val result = llmRepository.complete(prompt, "HINT")
                
                result.onSuccess { hint ->
                    _quizState.value = _quizState.value.copy(
                        isGeneratingHint = false,
                        dynamicHint = hint,
                        hintLevel = 1
                    )
                }.onFailure {
                    _quizState.value = _quizState.value.copy(
                        isGeneratingHint = false,
                        hintLevel = 1
                    )
                }
            }
        } else if (current.hintLevel < 3) {
            _quizState.value = current.copy(hintLevel = current.hintLevel + 1)
        }
    }

    fun skipQuizQuestion() {
        val current = _quizState.value
        current.selectedAnswers.add(-1) // -1 signifies skipped
        _quizState.value = current.copy(
            lastAnswerCorrect = false // Skip doesn't give a point
        )
        // Note: skip does not trigger errorBook penalty
    }

    /** Handles the read-aloud quiz question by triggering ASR evaluation. */
    fun submitQuizReadAloud(referenceText: String) {
        val audioFile = voiceRecorder.stopRecording()
        _readAlongState.value = _readAlongState.value.copy(isEvaluating = true)

        viewModelScope.launch {
            val result = asrRepository.evaluatePronunciation(
                audioFile ?: java.io.File(""),
                referenceText
            )
            val pronunciationResult = result.getOrElse {
                asrRepository.getOfflineFallbackResult(referenceText)
            }
            val isCorrect = pronunciationResult.overallScore >= 60

            val current = _quizState.value
            current.selectedAnswers.add(if (isCorrect) 0 else -1)
            _quizState.value = current.copy(
                score = current.score + if (isCorrect) 1 else 0,
                lastAnswerCorrect = isCorrect
            )
            _readAlongState.value = _readAlongState.value.copy(
                isEvaluating = false,
                result = pronunciationResult,
                cumulativeScore = _readAlongState.value.cumulativeScore + pronunciationResult.overallScore,
                evaluationsCount = _readAlongState.value.evaluationsCount + 1
            )

            if (!isCorrect) {
                errorBookRepository.upsertError(referenceText, "SPEAKING_MISPRONOUNCED", "SPEAK_ALOUD")
            } else {
                errorBookRepository.markCorrect(referenceText)
            }
        }
    }

    /** Starts recording for a read-aloud quiz question. */
    fun startQuizRecording() {
        voiceRecorder.startRecording()
        _readAlongState.value = _readAlongState.value.copy(isRecording = true, result = null)
    }

    /** Advances to the next quiz question or shows the result page. */
    fun nextQuizQuestion() {
        val current = _quizState.value
        val total = _session.value.quizQuestions.size
        _readAlongState.value = _readAlongState.value.copy(result = null, isRecording = false)

        if (current.currentIndex < total - 1) {
            _quizState.value = current.copy(
                currentIndex = current.currentIndex + 1,
                lastAnswerCorrect = null,
                hintLevel = 0,
                dynamicHint = null,
                isGeneratingHint = false
            )
        } else {
            // All quiz questions answered; proceed directly to completion
            _stage.value = LearningStage.COMPLETE
            computeSummary()
        }
    }

    /** Plays audio for a listening quiz question via system TTS. */
    fun playQuizAudio(text: String) {
        systemTtsHelper.speak(text, 0.85f)
    }

    //endregion

    //region Completion

    private fun computeSummary() {
        val session = _session.value
        val quizScore = _quizState.value.score
        val quizTotal = session.quizQuestions.size
        val readState = _readAlongState.value
        val pronunciationScore = if (readState.evaluationsCount > 0) readState.cumulativeScore / readState.evaluationsCount else 85
        val accuracy = if (quizTotal > 0) quizScore.toFloat() / quizTotal else 1.0f

        viewModelScope.launch {
            val record = org.akj.lingo.learn.domain.model.LearningRecord(
                id = java.util.UUID.randomUUID().toString(),
                taskId = "DAILY_${System.currentTimeMillis()}",
                timestamp = System.currentTimeMillis(),
                taskType = "DAILY_PRACTICE",
                accuracy = accuracy,
                duration = 900L,
                score = quizScore * 20,
                streakDays = 1,
                lastModified = System.currentTimeMillis()
            )
            learningRecordRepository.saveSessionRecord(record)
            val currentStreak = learningRecordRepository.getStreakDays()

            StreakPrefs.saveStreakData(context, currentStreak, todayDone = true)

            _summary.value = SessionSummary(
                newWordsLearned = session.targetNewWords.size,
                totalNewWords = session.targetNewWords.size,
                streakDays = currentStreak,
                weeklyDayNumber = 1,
                weeklyTotalDays = 7,
                quizScore = quizScore,
                quizTotal = quizTotal,
                pronunciationScore = pronunciationScore
            )
        }
    }

    //endregion

    override fun onCleared() {
        super.onCleared()
        systemTtsHelper.shutdown()
        voiceRecorder.cancelRecording()
    }
}
