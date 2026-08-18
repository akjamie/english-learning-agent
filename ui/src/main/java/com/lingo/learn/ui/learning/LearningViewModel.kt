package org.akj.lingo.learn.ui.learning

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import org.akj.lingo.learn.domain.model.*
import org.akj.lingo.learn.domain.repository.AsrRepository
import org.akj.lingo.learn.domain.repository.AgentDecisionLogRepository
import org.akj.lingo.learn.domain.repository.ConfigRepository
import org.akj.lingo.learn.domain.repository.GamificationRepository
import org.akj.lingo.learn.domain.repository.TtsRepository
import org.akj.lingo.learn.domain.usecase.AdaptiveDifficultyEngine
import org.akj.lingo.learn.domain.usecase.DailyGoalTracker
import org.akj.lingo.learn.domain.usecase.MakeupCardManager
import org.akj.lingo.learn.domain.usecase.Observation
import org.akj.lingo.learn.domain.usecase.ObservationTriggerEngine
import org.akj.lingo.learn.domain.usecase.ProductionTaskScorer
import org.akj.lingo.learn.domain.usecase.PhonemeHintEngine
import org.akj.lingo.learn.domain.usecase.XpRewardSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.akj.lingo.learn.ui.Checkpoint
import org.akj.lingo.learn.ui.StreakPrefs
import org.akj.lingo.learn.ui.TaskState
import org.akj.lingo.learn.ui.TaskStatePrefs
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
    /** Set when ASR credentials exist but the cloud ASR call failed; the recording is NOT scored. */
    val asrErrorMessage: String? = null,
    val completedCount: Int = 0,
    val cumulativeScore: Int = 0,
    val evaluationsCount: Int = 0,
    /** Phoneme hints aggregated across read-along evaluations for the Parent Companion Card. */
    val phonemeHints: List<PhonemeHintEngine.PhonemeHint> = emptyList(),
    val isShadowMode: Boolean = false,
    val isCountdownActive: Boolean = false,
    val countdownValue: Int = 0
)

data class GameState(
    val currentIndex: Int = 0,
    val score: Int = 0,
    val combo: Int = 0,
    val lastAnswerCorrect: Boolean? = null,
    val showComboEffect: Boolean = false,
    // Sprint 11: question-start timestamp for FAST_ANSWER observation
    val questionStartMs: Long = 0L
)

data class QuizState(
    val currentIndex: Int = 0,
    val selectedAnswers: MutableList<Int> = mutableListOf(),
    val score: Int = 0,
    val showResult: Boolean = false,
    val lastAnswerCorrect: Boolean? = null,
    val hintLevel: Int = 0,
    val dynamicHint: String? = null,
    val isGeneratingHint: Boolean = false,
    // Sprint 11: question-start timestamp for FAST_ANSWER observation
    val questionStartMs: Long = 0L
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
private const val NEGATIVE_THRESHOLD = 3

@HiltViewModel
class LearningViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val asrRepository: AsrRepository,
    private val voiceRecorder: VoiceRecorder,
    private val systemTtsHelper: SystemTtsHelper,
    private val ttsRepository: TtsRepository,
    private val configRepository: ConfigRepository,
    private val weeklyPlanRepository: org.akj.lingo.learn.domain.repository.WeeklyPlanRepository,
    private val learningRecordRepository: org.akj.lingo.learn.domain.repository.LearningRecordRepository,
    private val errorBookRepository: org.akj.lingo.learn.domain.repository.ErrorBookRepository,
    private val llmRepository: org.akj.lingo.learn.domain.repository.LlmRepository,
    private val observationTriggerEngine: ObservationTriggerEngine,
    private val agentDecisionLogRepository: AgentDecisionLogRepository,
    private val adaptiveDifficultyEngine: AdaptiveDifficultyEngine,
    private val xpRewardSystem: XpRewardSystem,
    private val dailyGoalTracker: DailyGoalTracker,
    private val makeupCardManager: MakeupCardManager,
    private val gamificationRepository: GamificationRepository,
    private val productionTaskScorer: ProductionTaskScorer,
    private val audioEngine: AudioPlaybackEngine
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

    // Sprint 10.5: true when cloud AI channels are unavailable (offline mode banner)
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    // Sprint 19: challenge track active (B1-level content)
    private val _isChallengeMode = MutableStateFlow(false)
    val isChallengeMode: StateFlow<Boolean> = _isChallengeMode.asStateFlow()

    private val _restoredFromCheckpoint = MutableStateFlow(false)
    val restoredFromCheckpoint: StateFlow<Boolean> = _restoredFromCheckpoint.asStateFlow()

    private val _checkpointIndex = MutableStateFlow(0)
    val checkpointIndex: StateFlow<Int> = _checkpointIndex.asStateFlow()

    private var consecutiveNegativeSignals = 0

    private val _showIntervention = MutableStateFlow(false)
    val showIntervention: StateFlow<Boolean> = _showIntervention.asStateFlow()

    private val _observation = MutableStateFlow<Observation?>(null)
    val observation: StateFlow<Observation?> = _observation.asStateFlow()

    // Sprint 7: XP / level progression
    private val _totalXp = MutableStateFlow(0)
    val totalXp: StateFlow<Int> = _totalXp.asStateFlow()

    private val _levelInfo = MutableStateFlow<XpRewardSystem.LevelInfo>(XpRewardSystem().levelInfo(0))
    val levelInfo: StateFlow<XpRewardSystem.LevelInfo> = _levelInfo.asStateFlow()

    private val _showLevelUp = MutableStateFlow(false)
    val showLevelUp: StateFlow<Boolean> = _showLevelUp.asStateFlow()

    // Sprint 7: Daily 3-goal system
    private val _dailyGoals = MutableStateFlow<DailyGoalTracker.DailyGoals?>(null)
    val dailyGoals: StateFlow<DailyGoalTracker.DailyGoals?> = _dailyGoals.asStateFlow()

    // Sprint 7: Makeup card mechanic
    private val _makeupState = MutableStateFlow<MakeupCardManager.MakeupState?>(null)
    val makeupState: StateFlow<MakeupCardManager.MakeupState?> = _makeupState.asStateFlow()

    private val _showMakeupPrompt = MutableStateFlow(false)
    val showMakeupPrompt: StateFlow<Boolean> = _showMakeupPrompt.asStateFlow()

    private var readAlongAttempts = 0

    private var currentGrade: String = "Grade 4"
    private var sessionStartTimeMs: Long = System.currentTimeMillis()
    private var taskDayIndex: Int = 1

    fun setGrade(grade: String, dayIndex: Int = 1) {
        currentGrade = grade
        sessionStartTimeMs = System.currentTimeMillis()
        taskDayIndex = dayIndex.coerceIn(1, 7)
        viewModelScope.launch {
            // Sprint 7: load adaptive difficulty from last quiz accuracy
            val lastAccuracy = learningRecordRepository.getMonthlyAccuracy() / 100f
            val adjustment = adaptiveDifficultyEngine.sentenceLengthAdjustment(lastAccuracy)
            val reviewQuestions = try {
                errorBookRepository.getReviewQuestionsForQuiz(2)
            } catch (e: Exception) { emptyList() }
            try {
                val loadedSession = weeklyPlanRepository.getCachedLearningSession(
                    dayIndex = dayIndex.coerceIn(1, 7),
                    grade = currentGrade,
                    reviewQuestions = reviewQuestions,
                    sentenceLengthAdjustment = adjustment
                )
                _session.value = loadedSession
                audioPlayer.loadSubtitles(loadedSession.subtitleLines)
                prepareImmersiveAudio(loadedSession.subtitleLines)
            } catch (e: Exception) {
                audioPlayer.loadSubtitles(_session.value.subtitleLines)
                prepareImmersiveAudio(_session.value.subtitleLines)
            }
            // Derive weekly day number from stored task day
            val taskDayStr = TaskStatePrefs.getTaskDay(context)
            taskDayIndex = try {
                val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val taskDay = fmt.parse(taskDayStr)
                val diff = ((System.currentTimeMillis() - taskDay.time) / (86400000)).toInt()
                (diff + 1).coerceIn(1, 7)
            } catch (_: Exception) { 1 }
        }
    }

    //region Emotional Intervention

    private fun incrementNegativeSignal() {
        consecutiveNegativeSignals++
        if (consecutiveNegativeSignals >= NEGATIVE_THRESHOLD) {
            _showIntervention.value = true
        }
    }

    private fun resetNegativeSignal() {
        consecutiveNegativeSignals = 0
    }

    /** User chooses to take a break — saves checkpoint and emits exit signal. */
    fun acceptRest() {
        _showIntervention.value = false
        resetNegativeSignal()
        saveCheckpoint()
        pauseTask()
    }

    /** User chooses to continue — dismisses dialog, resets counter, no penalty. */
    fun acceptContinue() {
        _showIntervention.value = false
        resetNegativeSignal()
    }

    /** User chooses to skip current stage — advances and resets counter. */
    fun acceptSkipStage() {
        _showIntervention.value = false
        resetNegativeSignal()
        when (_stage.value) {
            LearningStage.PRE_TEACH -> { _stage.value = LearningStage.IMMERSION }
            LearningStage.IMMERSION -> { _stage.value = LearningStage.PRACTICE; _practicePhase.value = PracticePhase.READ_ALONG }
            LearningStage.PRACTICE -> { _stage.value = LearningStage.QUIZ }
            LearningStage.QUIZ -> { _stage.value = LearningStage.COMPLETE; computeSummary() }
            LearningStage.COMPLETE -> {}
        }
    }

    //endregion

    //region Lingo Observation Agent (Sprint 6)

    /** Dismisses the currently showing observation bubble. */
    fun dismissObservation() {
        _observation.value = null
    }

    /**
     * Persists a lightweight per-attempt learning record so the Observation Agent
     * has word-level history to compare against across days.
     */
    private suspend fun recordAttempt(word: String, taskType: String, accuracy: Float) {
        learningRecordRepository.saveSessionRecord(
            LearningRecord(
                id = java.util.UUID.randomUUID().toString(),
                taskId = word,
                timestamp = System.currentTimeMillis(),
                taskType = taskType,
                accuracy = accuracy,
                duration = 0L,
                score = 0,
                streakDays = 0,
                lastModified = System.currentTimeMillis()
            )
        )
    }

    /**
     * Runs the longitudinal observation rules against the last 7 days of records.
     * Only one bubble at a time; matched observations are persisted to the decision log.
     */
    private suspend fun evaluateObservation(
        word: String,
        score: Int?,
        questionType: String?,
        attemptCount: Int,
        responseTimeMs: Long? = null
    ) {
        if (_observation.value != null) return
        val recentRecords = learningRecordRepository.getRecordsSince(
            System.currentTimeMillis() - 7 * 24 * 3600 * 1000L
        )
        val matched = observationTriggerEngine.checkObservation(
            currentWord = word,
            currentScore = score,
            questionType = questionType,
            attemptCount = attemptCount,
            recentRecords = recentRecords,
            responseTimeMs = responseTimeMs
        ) ?: return

        _observation.value = matched
        agentDecisionLogRepository.insert(
            AgentDecisionLog(
                id = java.util.UUID.randomUUID().toString(),
                decisionType = "OBSERVATION_MADE",
                title = "Lingo observed: ${matched.message}",
                description = matched.message,
                metadata = """{"type":"${matched.type.name}","word":"${matched.word}"}""",
                confidence = 0.85f
            )
        )
    }

    //endregion

    init {
        loadGamificationState()
        refreshMakeupState()
        refreshOfflineMode()
        setGrade(currentGrade)
        // Check for PAUSED checkpoint from previous session
        val savedState = TaskStatePrefs.getTaskState(context)
        if (savedState == TaskState.PAUSED) {
            val ck = TaskStatePrefs.getCheckpoint(context)
            _stage.value = try { LearningStage.valueOf(ck.stage) } catch (e: Exception) { LearningStage.IMMERSION }
            if (_stage.value == LearningStage.PRACTICE && ck.phase != null) {
                _practicePhase.value = try { PracticePhase.valueOf(ck.phase) } catch (e: Exception) { PracticePhase.GAME }
            }
            _restoredFromCheckpoint.value = true
            _checkpointIndex.value = ck.questionIndex
        }
        // Mark as in-progress on first load
        if (savedState != TaskState.IN_PROGRESS && savedState != TaskState.PAUSED) {
            TaskStatePrefs.setTaskState(context, TaskState.IN_PROGRESS)
            TaskStatePrefs.setTaskDay(context, java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()))
        }
        // Speak subtitle lines via TTS as they become active during immersion playback.
        // Sprint 13: only fire-and-forget in simulated mode; real audio mode plays
        // TTS files directly via the AudioPlaybackEngine inside AudioPlayerController.
        viewModelScope.launch {
            audioPlayer.state
                .filter { !it.isRealAudio }
                .map { it.currentSubtitleIndex }
                .distinctUntilChanged()
                .collect { index ->
                    if (index >= 0) {
                        val line = _session.value.subtitleLines.getOrNull(index)
                        if (line != null) {
                            val speed = audioPlayer.state.value.speed
                            speakWithTts(line.text, speed * 0.85f)
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

    /**
     * Sprint 13: Pre-synthesizes TTS audio for each subtitle line and configures
     * the audio player for real audio playback. Falls back to simulated mode
     * (with per-line system TTS) when cloud TTS is unavailable or synthesis
     * fails for all lines. Lines that fail synthesis individually degrade to
     * simulated playback (mixed mode) within the controller.
     */
    private fun prepareImmersiveAudio(subtitles: List<SubtitleLine>) {
        val token = configRepository.getAuthToken()
        if (token.length < 10) {
            // Offline - no cloud TTS; use simulated mode with system TTS per-line
            audioPlayer.clearRealAudio()
            return
        }
        audioPlayer.setPreparingAudio(true)
        viewModelScope.launch {
            val files = subtitles.map { line ->
                try {
                    val result = ttsRepository.getSpeech(line.text, 1.0f)
                    result.getOrNull()
                } catch (_: Exception) {
                    null
                }
            }
            val hasAnyReal = files.any { it != null && it.exists() }
            if (hasAnyReal) {
                audioPlayer.configureRealAudio(audioEngine, files)
            } else {
                audioPlayer.clearRealAudio()
            }
            audioPlayer.setPreparingAudio(false)
        }
    }

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
        speakWithTts(word)
    }

    /**
     * Sprint 10.5: speaks text using cloud TTS when configured, falling back to
     * Android system TTS otherwise. This fixes "no sound" when no TTS model is
     * configured or the device has no TTS engine data.
     */
    fun speakWithTts(text: String, rate: Float = 0.85f) {
        val token = configRepository.getAuthToken()
        if (token.length < 10) {
            // Not configured — use offline system TTS.
            if (!systemTtsHelper.speak(text, rate)) {
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
            if (!systemTtsHelper.speak(text, rate)) {
                showTtsUnavailableWarning()
            }
        }
    }

    // Sprint 20: explicit parent-facing warning instead of silent no-op
    // when the device has no English TTS engine or voice pack installed.
    private fun showTtsUnavailableWarning() {
        android.widget.Toast.makeText(
            context,
            "⚠️ English voice engine is not ready. Please configure your API Key or install a TTS voice pack.",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    /** Plays a TTS audio file via MediaPlayer (fire-and-forget). */
    private fun playAudioFile(file: java.io.File) {
        try {
            val player = android.media.MediaPlayer()
            player.setDataSource(file.absolutePath)
            player.setOnPreparedListener { it.start() }
            player.setOnCompletionListener { it.release() }
            player.prepareAsync()
        } catch (_: Exception) {
            // Ignore playback errors; caller already degraded to system TTS on failure.
        }
    }

    /** Sprint 10.5: refreshes the offline-mode flag from current config. */
    private fun refreshOfflineMode() {
        val token = configRepository.getAuthToken()
        _isOfflineMode.value = token.length < 10
        _isChallengeMode.value = configRepository.isChallengeModeEnabled()
    }

    /** Advances from Stage 0 (pre-teach) to Stage 1 (immersion). */
    fun proceedToImmersion() {
        _stage.value = LearningStage.IMMERSION
    }

    /** Saves the current learning checkpoint to persistent storage. */
    private fun saveCheckpoint() {
        val stage = _stage.value
        val phase = if (stage == LearningStage.PRACTICE) _practicePhase.value.name else null
        val score = _quizState.value.score * 10 + _gameState.value.score
        TaskStatePrefs.saveCheckpoint(
            context,
            Checkpoint(
                stage = stage.name,
                phase = phase,
                questionIndex = when (stage) {
                    LearningStage.PRACTICE -> if (_practicePhase.value == PracticePhase.GAME) _gameState.value.currentIndex else _readAlongState.value.currentIndex
                    LearningStage.QUIZ -> _quizState.value.currentIndex
                    else -> 0
                },
                score = score,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    /** Pauses the current task (called when app goes to background). */
    fun pauseTask() {
        val currentState = TaskStatePrefs.getTaskState(context)
        if (currentState == TaskState.IN_PROGRESS) {
            TaskStatePrefs.setTaskState(context, TaskState.PAUSED)
            saveCheckpoint()
            audioPlayer.pause()
        }
    }

    /** Dismisses the checkpoint restoration and resets to start. */
    fun dismissCheckpoint() {
        _restoredFromCheckpoint.value = false
        TaskStatePrefs.clearCheckpoint(context)
        TaskStatePrefs.setTaskState(context, TaskState.IN_PROGRESS)
        _stage.value = LearningStage.IMMERSION
    }

    /** Advances from Stage 1 (immersion) to Stage 2 (practice: read-along). */
    fun proceedToPractice() {
        audioPlayer.pause()
        _stage.value = LearningStage.PRACTICE
        _practicePhase.value = PracticePhase.READ_ALONG
        _gameState.value = _gameState.value.copy(questionStartMs = System.currentTimeMillis())
        resetNegativeSignal()
        saveCheckpoint()
    }

    //endregion

    //region Stage 2a: Read-along

    /** Starts recording the child's voice for the current read-along sentence. */
    fun startRecording() {
        voiceRecorder.startRecording()
        _readAlongState.value = _readAlongState.value.copy(isRecording = true, result = null, asrErrorMessage = null)
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
            val pronunciationResult = result.getOrNull()
            if (pronunciationResult == null) {
                // ASR credentials exist but the cloud ASR call failed. Do NOT fabricate
                // a score or record the attempt — surface the error so the parent/child
                // can retry instead of being rewarded with a fake "85".
                _readAlongState.value = _readAlongState.value.copy(
                    isEvaluating = false,
                    asrErrorMessage = "Voice check couldn't reach the ASR service. Please try again."
                )
                return@launch
            }
            readAlongAttempts++
            if (pronunciationResult.overallScore < 60) {
                incrementNegativeSignal()
                errorBookRepository.upsertError(
                    extractMispronouncedWord(referenceText, pronunciationResult),
                    "SPEAKING_MISPRONOUNCED",
                    "SPEAK_ALOUD"
                )
            } else {
                resetNegativeSignal()
            }
            val aggregatedHints = (_readAlongState.value.phonemeHints + pronunciationResult.phonemeHints)
                .distinctBy { it.phonemeLabel }
                .take(2)
            _readAlongState.value = _readAlongState.value.copy(
                isEvaluating = false,
                result = pronunciationResult,
                completedCount = _readAlongState.value.completedCount + 1,
                cumulativeScore = _readAlongState.value.cumulativeScore + pronunciationResult.overallScore,
                evaluationsCount = _readAlongState.value.evaluationsCount + 1,
                phonemeHints = aggregatedHints
            )
            // Fallback results (offline estimate / ASR unavailable) must not be recorded
            // as real attempts — they would corrupt streaks, observations and reports.
            if (!pronunciationResult.isFromFallback) {
                recordAttempt(referenceText, "SPEAKING", pronunciationResult.overallScore / 100f)
                evaluateObservation(referenceText, pronunciationResult.overallScore, "SPEAKING", readAlongAttempts)
            }
        }
    }

    fun playReadAlongDemo() {
        val sentence = _session.value.readAlongSentences[_readAlongState.value.currentIndex]
        sentence.audioPath?.let {
            _readAlongState.value = _readAlongState.value.copy(isPlayingDemo = true)
            // Sprint 10.5: use cloud TTS when configured, system TTS otherwise
            speakWithTts(sentence.text, 0.9f)
            // Reset state after a delay or on completion callback (mocked here)
            _readAlongState.value = _readAlongState.value.copy(isPlayingDemo = false)
        } ?: run {
            speakWithTts(sentence.text, 0.9f)
        }
    }

    /** Toggles shadow mode on/off. When activated, starts the auto-shadow flow. */
    fun toggleShadowMode() {
        val current = _readAlongState.value
        if (current.isShadowMode) {
            _readAlongState.value = current.copy(isShadowMode = false)
        } else {
            _readAlongState.value = current.copy(isShadowMode = true)
            startShadowFlow()
        }
    }

    /** Auto-recording flow for shadow mode: TTS play → countdown → auto-mic → wait → evaluate. */
    private fun startShadowFlow() {
        val sentence = _session.value.readAlongSentences[_readAlongState.value.currentIndex]
        val shadowDelay = configRepository.getShadowDelayMs()

        viewModelScope.launch {
            speakWithTts(sentence.text, 0.9f)

            _readAlongState.value = _readAlongState.value.copy(isCountdownActive = true)
            for (i in 3 downTo 1) {
                _readAlongState.value = _readAlongState.value.copy(countdownValue = i)
                delay(1000)
            }
            _readAlongState.value = _readAlongState.value.copy(countdownValue = 0, isCountdownActive = false)

            startRecording()

            val sentenceDuration = (sentence.text.length * 80L) + 1500L
            delay(sentenceDuration + shadowDelay)

            stopRecording()

            delay(2000)
            nextReadAlongSentence()
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
        incrementNegativeSignal()
        readAlongAttempts++
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
            readAlongAttempts = 0
        } else {
            // All read-along sentences completed; move to mini-games
            _practicePhase.value = PracticePhase.GAME
        }
        saveCheckpoint()
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

        if (isCorrect) resetNegativeSignal()
        else incrementNegativeSignal()

        val newCombo = if (isCorrect) current.combo + 1 else 0
        val newScore = current.score + if (isCorrect) 10 else 0
        val showCombo = isCorrect && newCombo >= 3

        _gameState.value = current.copy(
            score = newScore,
            combo = newCombo,
            lastAnswerCorrect = isCorrect,
            showComboEffect = showCombo
        )

        val word = question.audioText
            ?.takeIf { it.isSingleWord() }
            ?: question.options.getOrNull(question.correctIndex) ?: question.prompt
        if (isCorrect) addXp("GAME")
        val responseMs = System.currentTimeMillis() - _gameState.value.questionStartMs
        viewModelScope.launch {
            if (!isCorrect) {
                errorBookRepository.upsertError(word, "GAME_WRONG_ANSWER", question.type.name)
            }
            recordAttempt(word, "GAME", if (isCorrect) 1.0f else 0.0f)
            evaluateObservation(word, if (isCorrect) 100 else 0, question.type.name, attemptCount = 1, responseTimeMs = responseMs)
        }
    }

    /** Advances to the next game question, or transitions to quiz. */
    fun nextGameQuestion() {
        val current = _gameState.value
        val total = _session.value.gameQuestions.size

        _gameState.value = current.copy(
            currentIndex = current.currentIndex + 1,
            lastAnswerCorrect = null,
            showComboEffect = false,
            questionStartMs = System.currentTimeMillis()
        )

        saveCheckpoint()
        if (current.currentIndex >= total - 1) {
            // All game questions completed; move to quiz
            _stage.value = LearningStage.QUIZ
            _quizState.value = _quizState.value.copy(questionStartMs = System.currentTimeMillis())
        }
    }

    /** Plays the audio for a listen-choose-image game question via system TTS. */
    fun playGameAudio(text: String) {
        speakWithTts(text, 0.85f)
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

        if (isCorrect) resetNegativeSignal()
        else incrementNegativeSignal()

        current.selectedAnswers.add(selectedIndex)
        val newScore = current.score + if (isCorrect) 1 else 0

        _quizState.value = current.copy(
            score = newScore,
            lastAnswerCorrect = isCorrect
        )

        val word = extractErrorWord(question)
        val responseMs = System.currentTimeMillis() - _quizState.value.questionStartMs
        viewModelScope.launch {
            if (!isCorrect) {
                errorBookRepository.upsertError(word, "QUIZ_WRONG_ANSWER", question.type.name)
            } else if (question.isFromErrorBook) {
                errorBookRepository.markCorrect(word)
                addXp(question.type.name)
            } else {
                addXp(question.type.name)
            }
            recordAttempt(word, "QUIZ", if (isCorrect) 1.0f else 0.0f)
            evaluateObservation(word, if (isCorrect) 100 else 0, question.type.name, attemptCount = 1, responseTimeMs = responseMs)
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

    /**
     * Submits free-text production answers (SPELLING / DICTATION / SENTENCE_WRITING)
     * using the [ProductionTaskScorer] for graded, deterministic feedback (Sprint 7 A5).
     */
    fun submitProductionAnswer(textInput: String) {
        val current = _quizState.value
        val question = _session.value.quizQuestions.getOrNull(current.currentIndex) ?: return
        val expected = question.options.getOrNull(question.correctIndex) ?: question.audioText ?: ""

        val score = when (question.type) {
            QuizQuestionType.SPELLING -> productionTaskScorer.scoreSpelling(textInput, question.audioText ?: expected)
            QuizQuestionType.DICTATION -> productionTaskScorer.scoreDictation(textInput, expected)
            QuizQuestionType.SENTENCE_WRITING -> productionTaskScorer.scoreSentenceWriting(textInput, question.audioText ?: expected)
            else -> return
        }

        if (score.isCorrect) resetNegativeSignal() else incrementNegativeSignal()
        current.selectedAnswers.add(if (score.isCorrect) question.correctIndex else -1)
        _quizState.value = current.copy(
            score = current.score + if (score.isCorrect) 1 else 0,
            lastAnswerCorrect = score.isCorrect
        )

        val word = extractErrorWord(question)
        viewModelScope.launch {
            if (!score.isCorrect) {
                errorBookRepository.upsertError(word, "PRODUCTION_WRONG", question.type.name)
            } else if (question.isFromErrorBook) {
                errorBookRepository.markCorrect(word)
                addXp(question.type.name)
            } else {
                addXp(question.type.name)
            }
            recordAttempt(word, "QUIZ", if (score.isCorrect) 1.0f else 0.0f)
            evaluateObservation(word, if (score.isCorrect) 100 else 0, question.type.name, attemptCount = 1)
        }
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
            val pronunciationResult = result.getOrNull()
            if (pronunciationResult == null) {
                // ASR credentials exist but the cloud ASR call failed. Do NOT fabricate
                // a score or record the attempt — surface the error so the child can
                // retry instead of being rewarded with a fake "85".
                _readAlongState.value = _readAlongState.value.copy(
                    isEvaluating = false,
                    asrErrorMessage = "Voice check couldn't reach the ASR service. Please try again."
                )
                return@launch
            }
            val isCorrect = pronunciationResult.overallScore >= 60

            val current = _quizState.value
            current.selectedAnswers.add(if (isCorrect) 0 else -1)
            _quizState.value = current.copy(
                score = current.score + if (isCorrect) 1 else 0,
                lastAnswerCorrect = isCorrect
            )
            val aggregatedHints = (_readAlongState.value.phonemeHints + pronunciationResult.phonemeHints)
                .distinctBy { it.phonemeLabel }
                .take(2)
            _readAlongState.value = _readAlongState.value.copy(
                isEvaluating = false,
                result = pronunciationResult,
                cumulativeScore = _readAlongState.value.cumulativeScore + pronunciationResult.overallScore,
                evaluationsCount = _readAlongState.value.evaluationsCount + 1,
                phonemeHints = aggregatedHints
            )

            if (!isCorrect) {
                errorBookRepository.upsertError(
                    extractMispronouncedWord(referenceText, pronunciationResult),
                    "SPEAKING_MISPRONOUNCED",
                    "SPEAK_ALOUD"
                )
            } else {
                errorBookRepository.markCorrect(extractMispronouncedWord(referenceText, pronunciationResult))
            }
            recordAttempt(referenceText, "SPEAKING", pronunciationResult.overallScore / 100f)
            evaluateObservation(referenceText, pronunciationResult.overallScore, "SPEAKING", attemptCount = 1)
        }
    }

    /** Starts recording for a read-aloud quiz question. */
    fun startQuizRecording() {
        voiceRecorder.startRecording()
        _readAlongState.value = _readAlongState.value.copy(isRecording = true, result = null, asrErrorMessage = null)
    }

    /** Advances to the next quiz question or shows the result page. */
    fun nextQuizQuestion() {
        val current = _quizState.value
        val total = _session.value.quizQuestions.size
        _readAlongState.value = _readAlongState.value.copy(result = null, isRecording = false, asrErrorMessage = null)

        if (current.currentIndex < total - 1) {
            _quizState.value = current.copy(
                currentIndex = current.currentIndex + 1,
                lastAnswerCorrect = null,
                hintLevel = 0,
                dynamicHint = null,
                isGeneratingHint = false,
                questionStartMs = System.currentTimeMillis()
            )
            saveCheckpoint()
        } else {
            // All quiz questions answered; proceed directly to completion
            _stage.value = LearningStage.COMPLETE
            computeSummary()
        }
    }

    /** Plays audio for a listening quiz question via system TTS. */
    fun playQuizAudio(text: String) {
        speakWithTts(text, 0.85f)
    }

    //endregion

    //region Sprint 7 + 10: Gamification (XP / Level / Daily Goals / Makeup Cards)

    /**
     * Loads persisted gamification state (XP, makeup cards, daily-goal snapshot)
     * into the ViewModel flows.
     */
    private fun loadGamificationState() {
        viewModelScope.launch {
            val state = gamificationRepository.getState()
            _totalXp.value = state.totalXp
            _levelInfo.value = xpRewardSystem.levelInfo(state.totalXp)
            _makeupState.value = makeupCardManager.stateForMonth(
                nowMs = System.currentTimeMillis(),
                storedMonthKey = state.makeupMonthKey,
                cardsUsedPreviously = state.makeupCardsUsed
            )
        }
    }

    private fun addXp(questionType: String?, sessionBonus: Boolean = false) {
        val gained = if (sessionBonus) xpRewardSystem.xpForSessionCompletion()
        else xpRewardSystem.xpForCorrect(questionType ?: "")
        val before = _totalXp.value
        val after = before + gained
        _totalXp.value = after
        _levelInfo.value = xpRewardSystem.levelInfo(after)
        viewModelScope.launch {
            val state = gamificationRepository.getState()
            gamificationRepository.saveState(state.copy(totalXp = after, lastModified = System.currentTimeMillis()))
        }

        if (xpRewardSystem.crossesLevelBoundary(before, after)) {
            _showLevelUp.value = true
        }
    }

    /** Dismisses the full-screen level-up celebration. */
    fun dismissLevelUp() {
        _showLevelUp.value = false
    }

    /** Resolves and persists the makeup-card allowance for the current month. */
    private fun refreshMakeupState() {
        val state = _makeupState.value
            ?: makeupCardManager.stateForMonth(System.currentTimeMillis(), null, 0)
        _makeupState.value = state
        viewModelScope.launch {
            val current = gamificationRepository.getState()
            gamificationRepository.saveState(
                current.copy(
                    makeupMonthKey = state.monthKey,
                    makeupCardsUsed = state.cardsUsed,
                    lastModified = System.currentTimeMillis()
                )
            )
        }
    }

    /** Shows the streak-break makeup card prompt (called when a streak break is detected). */
    fun promptMakeupCard() {
        val state = _makeupState.value ?: return
        if (makeupCardManager.canUseCard(state)) {
            _showMakeupPrompt.value = true
        }
    }

    /** Child/parent actively chooses to use a makeup card to preserve the streak. */
    fun useMakeupCard() {
        val state = _makeupState.value ?: return
        val updated = makeupCardManager.useCard(state)
        _makeupState.value = updated
        viewModelScope.launch {
            val current = gamificationRepository.getState()
            gamificationRepository.saveState(
                current.copy(
                    makeupMonthKey = updated.monthKey,
                    makeupCardsUsed = updated.cardsUsed,
                    lastModified = System.currentTimeMillis()
                )
            )
        }
        _showMakeupPrompt.value = false
    }

    /** Child/parent declines the makeup card — streak breaks naturally. */
    fun declineMakeupCard() {
        _showMakeupPrompt.value = false
    }

    /** Evaluates and stores the daily 3-goal state for the completion screen. */
    private suspend fun refreshDailyGoals(newWords: Int) {
        val accuracy = _session.value.quizQuestions.size.takeIf { it > 0 }?.let { _quizState.value.score.toFloat() / it } ?: 0f
        val goals = dailyGoalTracker.evaluate(
            sessionCompleted = true,
            quizAccuracy = accuracy,
            newWordsLearned = newWords
        )
        _dailyGoals.value = goals

        // Sprint 10: persist the daily-goal snapshot so it survives re-installs
        // and can be surfaced on the Dashboard.
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val state = gamificationRepository.getState()
        gamificationRepository.saveState(
            state.copy(
                dailyGoalsDate = today,
                sessionGoalAchieved = goals.goals.getOrNull(0)?.achieved ?: false,
                accuracyGoalAchieved = goals.goals.getOrNull(1)?.achieved ?: false,
                wordsGoalAchieved = goals.goals.getOrNull(2)?.achieved ?: false,
                lastModified = System.currentTimeMillis()
            )
        )
    }

    /** Dismisses the gamification overlay state. */
    fun dismissGamification() {
        _showLevelUp.value = false
        _showMakeupPrompt.value = false
    }

    /**
     * Detects a streak break (yesterday was not completed but the child studied today)
     * and offers the makeup-card active choice rather than silently auto-using one.
     */
    private suspend fun refreshMakeupPromptOnStreak() {
        val streak = learningRecordRepository.getStreakDays()
        if (streak <= 1) return // first day or still growing — nothing to rescue
        val todayDone = StreakPrefs.isTodayDone(context)
        if (!todayDone) return
        // Streak grew (>=2) meaning consecutive days exist; no break to repair.
        if (_makeupState.value?.cardsLeft ?: 0 <= 0) return
        // Only prompt if the previous day was genuinely missed (streak == 1 after a gap).
        if (streak == 1) {
            _showMakeupPrompt.value = true
        }
    }

    //endregion

    //region Completion

    private fun computeSummary() {
        val session = _session.value
        val quizScore = _quizState.value.score
        val quizTotal = session.quizQuestions.size
        val readState = _readAlongState.value
        val pronunciationScore = if (readState.evaluationsCount > 0) readState.cumulativeScore / readState.evaluationsCount else null
        val accuracy = if (quizTotal > 0) quizScore.toFloat() / quizTotal else 1.0f

        viewModelScope.launch {
            val record = org.akj.lingo.learn.domain.model.LearningRecord(
                id = java.util.UUID.randomUUID().toString(),
                taskId = "DAILY_${System.currentTimeMillis()}",
                timestamp = System.currentTimeMillis(),
                taskType = "DAILY_PRACTICE",
                accuracy = accuracy,
                duration = ((System.currentTimeMillis() - sessionStartTimeMs) / 1000).coerceIn(60, 3600),
                score = quizScore * 20,
                streakDays = 1,
                lastModified = System.currentTimeMillis()
            )
            learningRecordRepository.saveSessionRecord(record)
            val currentStreak = learningRecordRepository.getStreakDays()

            TaskStatePrefs.setTaskState(context, TaskState.COMPLETED)
            TaskStatePrefs.clearCheckpoint(context)
            StreakPrefs.saveStreakData(context, currentStreak, todayDone = true)

            addXp(questionType = null, sessionBonus = true)
            refreshDailyGoals(session.targetNewWords.size)
            refreshMakeupPromptOnStreak()

            _summary.value = SessionSummary(
                newWordsLearned = session.targetNewWords.size,
                totalNewWords = session.targetNewWords.size,
                streakDays = currentStreak,
                weeklyDayNumber = taskDayIndex,
                weeklyTotalDays = 7,
                quizScore = quizScore,
                quizTotal = quizTotal,
                pronunciationScore = pronunciationScore,
                phonemeHints = readState.phonemeHints
            )
        }
    }

    //endregion

    /**
     * Extracts the real target word from a quiz question for the error book,
     * instead of storing synthetic ids ("vocab_123") or a whole sentence.
     * Priority: single-word correct option, single-word audioText, word
     * embedded in the prompt, then the longest content word of the prompt.
     */
    private fun extractErrorWord(question: QuizQuestion): String {
        // Phonics CVC_BUILD: the built word is the ordered letter tiles.
        if (question.type == QuizQuestionType.CVC_BUILD && question.correctOrder.isNotEmpty()) {
            return question.correctOrder.joinToString("")
        }

        // Choice-based questions carry the correct word in options.
        question.options.getOrNull(question.correctIndex)
            ?.takeIf { it.isSingleWord() }
            ?.let { return it }

        // Listen/spell questions carry the word in audioText.
        question.audioText
            ?.takeIf { it.isSingleWord() }
            ?.let { return it }

        // SPELL_FILL_BLANK: word embedded in the prompt, e.g. "Complete the word: s_chool".
        Regex("word: (\\S+)").find(question.question)
            ?.groupValues?.get(1)
            ?.filter { it.isLetter() }
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        // SENTENCE_ORDER: word quoted in the prompt, e.g. "Order: 'apple' in a sentence".
        Regex("'([^']+)'").find(question.question)
            ?.groupValues?.get(1)
            ?.takeIf { it.isSingleWord() }
            ?.let { return it }

        // DICTATION / READ_ALOUD: fall back to the longest content word.
        val contentWord = extractLongestWord(question.audioText ?: question.question)
        return contentWord.ifBlank { question.question.take(24) }
    }

    private fun String.isSingleWord(): Boolean =
        matches(Regex("^[A-Za-z][A-Za-z'-]*$"))

    private fun extractLongestWord(sentence: String): String =
        sentence.split(Regex("[^A-Za-z]+"))
            .filter { it.isNotBlank() && it.length >= 2 }
            .maxByOrNull { it.length }
            ?: ""

    /**
     * Picks the worst-scoring word (below 60) from an ASR evaluation to store
     * in the error book, falling back to the longest word of the reference.
     */
    private fun extractMispronouncedWord(referenceText: String, result: PronunciationResult): String {
        val worstWord = result.wordScores
            .filter { it.score < 60 }
            .minByOrNull { it.score }
            ?.word
        if (!worstWord.isNullOrBlank() && worstWord.isSingleWord()) return worstWord
        return extractLongestWord(referenceText).ifBlank { referenceText.take(24) }
    }

    override fun onCleared() {
        super.onCleared()
        systemTtsHelper.shutdown()
        voiceRecorder.cancelRecording()
    }
}
