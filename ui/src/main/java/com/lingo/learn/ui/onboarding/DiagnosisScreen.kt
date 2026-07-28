package org.akj.lingo.learn.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.akj.lingo.learn.ui.R
import org.akj.lingo.learn.ui.components.LingoAvatar
import org.akj.lingo.learn.ui.components.LingoExpression
import org.akj.lingo.learn.ui.components.MicButton
import org.akj.lingo.learn.ui.learning.SystemTtsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DiagnosticQuestion(
    val id: Int,
    val type: QuestionType,
    val title: String,
    val description: String,
    val voicePrompt: String? = null,
    val options: List<String> = emptyList(),
    val correctAnswer: String = "",
    val wordsForSort: List<String> = emptyList()
)

enum class QuestionType {
    LISTENING_EMOJI,
    VOCABULARY,
    PHONICS,
    SORT_WORDS,
    SPEAK_ALOUD
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DiagnosisScreen(
    onDiagnosisFinished: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiagnosisViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    val ttsHelper = remember { SystemTtsHelper(context) }
    DisposableEffect(Unit) {
        onDispose { ttsHelper.shutdown() }
    }

    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val questions by viewModel.questions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDiagnosticQuestions("Grade 3") // Hardcoded grade for now, ideally passed via nav arg
    }

    if (isLoading) {
        Box(modifier = modifier.fillMaxSize().background(Color(0xFFFFFDF5)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LingoAvatar(expression = LingoExpression.THINKING, modifier = Modifier.size(120.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("🦊 Lingo is preparing your quiz...", fontSize = 18.sp, color = Color(0xFF2C3E50), fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    if (questions.isEmpty()) return

    var currentQuestionIndex by remember { mutableStateOf(0) }
    val currentQuestion = questions[currentQuestionIndex]

    var selectedOption by remember { mutableStateOf<String?>(null) }
    var sortedWords by remember { mutableStateOf<List<String>>(emptyList()) }
    var isRecording by remember { mutableStateOf(false) }
    var isEvaluated by remember { mutableStateOf(false) }
    var evaluationScore by remember { mutableStateOf(0) }

    var lingoExpr by remember { mutableStateOf(LingoExpression.THINKING) }
    var answerState by remember { mutableStateOf<Boolean?>(null) }

    var isPlayingVoice by remember { mutableStateOf(false) }
    var totalScore by remember { mutableStateOf(0) }

    fun resetQuestionState() {
        selectedOption = null
        sortedWords = emptyList()
        isRecording = false
        isEvaluated = false
        evaluationScore = 0
        lingoExpr = LingoExpression.THINKING
        answerState = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF5))
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            questions.forEachIndexed { index, _ ->
                val isCompleted = index < currentQuestionIndex
                val isCurrent = index == currentQuestionIndex
                val color = if (isCompleted) Color(0xFFFFD449) else if (isCurrent) Color(0xFF5C6FF2) else Color(0xFFE0E0E0)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Avatar & Dialog speech bubble
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LingoAvatar(
                expression = lingoExpr,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                    .background(Color.White)
                    .border(1.5.dp, Color(0xFFECEFF1), RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = when (answerState) {
                        true -> "Awesome! That's correct! 🎉"
                        false -> "Nice try! Let's keep going! 💪"
                        else -> if (currentQuestion.type == QuestionType.SPEAK_ALOUD) "Press and hold the mic button below to record!" else "Select your answer below!"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2C3E50)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Question Card with Smooth Directional Horizontal Slide Animation
        AnimatedContent(
            targetState = currentQuestionIndex,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeOut()
                } else {
                    slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeIn() togetherWith
                            slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeOut()
                }
            },
            label = "QuestionSlide",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { qIndex ->
            val q = questions[qIndex]

            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = q.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C6FF2)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = q.description,
                        fontSize = 14.sp,
                        color = Color(0xFF7F8C8D),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        when (q.type) {
                            QuestionType.LISTENING_EMOJI -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(CircleShape)
                                            .background(if (isPlayingVoice) Color(0xFFFFECE5) else Color(0xFFFF7052))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                val prompt = q.voicePrompt ?: "apple"
                                                ttsHelper.speak(prompt, 0.85f)
                                                coroutineScope.launch {
                                                    isPlayingVoice = true
                                                    delay(1200)
                                                    isPlayingVoice = false
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isPlayingVoice) "🔊" else "🔈",
                                            fontSize = 34.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    ChoiceGroup(
                                        options = q.options,
                                        selectedOption = selectedOption,
                                        onOptionSelected = { selectedOption = it }
                                    )
                                }
                            }
                            QuestionType.VOCABULARY, QuestionType.PHONICS -> {
                                ChoiceGroup(
                                    options = q.options,
                                    selectedOption = selectedOption,
                                    onOptionSelected = { selectedOption = it }
                                )
                            }
                            QuestionType.SORT_WORDS -> {
                                val availableWords = q.wordsForSort.filter { !sortedWords.contains(it) }

                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Sentence Target Area:", fontSize = 13.sp, color = Color.Gray)

                                    // Target sentence box
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(72.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFFF8F9FA))
                                            .border(1.5.dp, Color(0xFF5C6FF2).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (sortedWords.isEmpty()) {
                                            Text(stringResource(R.string.tap_cards_to_sort), color = Color.Gray, fontSize = 14.sp)
                                        } else {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                sortedWords.forEach { word ->
                                                    ChipItem(text = word, onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        sortedWords = sortedWords - word
                                                    })
                                                }
                                            }
                                        }
                                    }

                                    Text("Tap words below to arrange:", fontSize = 13.sp, color = Color.Gray)

                                    // Available words options
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        availableWords.forEach { word ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(52.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFFFFD449))
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        sortedWords = sortedWords + word
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(word, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2C3E50))
                                            }
                                        }
                                    }
                                }
                            }
                            QuestionType.SPEAK_ALOUD -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = q.voicePrompt ?: "",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2C3E50),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (isEvaluated) {
                                        Text(
                                            text = "Speech Score: $evaluationScore / 100! 🎉",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2ECC71),
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ergonomic Bottom Control Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp),
            contentAlignment = Alignment.Center
        ) {
            if (currentQuestion.type == QuestionType.SPEAK_ALOUD) {
                if (isEvaluated) {
                    Button(
                        onClick = {
                            val rating = when {
                                totalScore >= 80 -> "C"
                                totalScore >= 50 -> "B"
                                else -> "A"
                            }
                            onDiagnosisFinished(rating)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2ECC71),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Finish Evaluation 🚀",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    MicButton(
                        isRecording = isRecording,
                        onPressDown = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isRecording = true
                        },
                        onPressUp = {
                            if (isRecording) {
                                isRecording = false
                                isEvaluated = true
                                lingoExpr = LingoExpression.THINKING
                                
                                coroutineScope.launch {
                                    val result = viewModel.evaluateSpeaking(java.io.File("dummy.wav"), currentQuestion.voicePrompt ?: "")
                                    evaluationScore = result.overallScore
                                    totalScore += (evaluationScore / 10)
                                    answerState = evaluationScore >= 60
                                    lingoExpr = if (answerState == true) LingoExpression.CELEBRATING else LingoExpression.SAD
                                }
                            }
                        }
                    )
                }
            } else {
                val isAnswered = when (currentQuestion.type) {
                    QuestionType.SORT_WORDS -> sortedWords.size == currentQuestion.wordsForSort.size
                    else -> selectedOption != null
                }

                Button(
                    onClick = {
                        if (answerState == null) {
                            val userAns = when (currentQuestion.type) {
                                QuestionType.SORT_WORDS -> sortedWords.joinToString(" ")
                                else -> selectedOption ?: ""
                            }
                            val isCorrect = userAns.trim().equals(currentQuestion.correctAnswer.trim(), ignoreCase = true)
                            answerState = isCorrect
                            if (isCorrect) {
                                totalScore += 10
                                lingoExpr = LingoExpression.CELEBRATING
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else {
                                lingoExpr = LingoExpression.SAD
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        } else {
                            if (currentQuestionIndex < questions.size - 1) {
                                currentQuestionIndex++
                                resetQuestionState()
                            } else {
                                val rating = when {
                                    totalScore >= 80 -> "C"
                                    totalScore >= 50 -> "B"
                                    else -> "A"
                                }
                                onDiagnosisFinished(rating)
                            }
                        }
                    },
                    enabled = isAnswered,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (answerState == null) Color(0xFFFFD449) else if (answerState == true) Color(0xFF2ECC71) else Color(0xFFFF7052),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (answerState == null) "Check Answer 🚀" else if (currentQuestionIndex < questions.size - 1) "Next Question ➡️" else "Finish Evaluation 🚀",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (answerState == null) Color(0xFF2C3E50) else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ChoiceGroup(
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) Color(0xFFECEFFF) else Color.White)
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) Color(0xFF5C6FF2) else Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onOptionSelected(option) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = option,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
            }
        }
    }
}

@Composable
fun ChipItem(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFECEFF1))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
    }
}

