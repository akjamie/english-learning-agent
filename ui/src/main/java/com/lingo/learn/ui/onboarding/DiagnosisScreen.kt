package com.lingo.learn.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lingo.learn.ui.R
import com.lingo.learn.ui.components.LingoAvatar
import com.lingo.learn.ui.components.LingoExpression
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Diagnostic questions type and data models
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

@Composable
fun DiagnosisScreen(
    onDiagnosisFinished: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val questions = remember {
        listOf(
            DiagnosticQuestion(
                id = 1,
                type = QuestionType.LISTENING_EMOJI,
                title = "Listen and Choose",
                description = "Tap the speaker, then select the correct word you heard:",
                voicePrompt = "apple",
                options = listOf("🍎 Apple", "🍌 Banana", "🐱 Cat"),
                correctAnswer = "🍎 Apple"
            ),
            DiagnosticQuestion(
                id = 2,
                type = QuestionType.VOCABULARY,
                title = "Opposite Word Select",
                description = "Choose the opposite word of 'Hot':",
                options = listOf("Cold", "Warm", "Big", "Dry"),
                correctAnswer = "Cold"
            ),
            DiagnosticQuestion(
                id = 3,
                type = QuestionType.PHONICS,
                title = "Phonics & Sound",
                description = "Which word starts with the /p/ sound?",
                options = listOf("Pig", "Big", "Dig", "Wig"),
                correctAnswer = "Pig"
            ),
            DiagnosticQuestion(
                id = 4,
                type = QuestionType.SORT_WORDS,
                title = "Sentence Ordering",
                description = "Tap the word cards to arrange them into a correct sentence:",
                wordsForSort = listOf("like", "apples", "I"),
                correctAnswer = "I like apples"
            ),
            DiagnosticQuestion(
                id = 5,
                type = QuestionType.SPEAK_ALOUD,
                title = "Speak Aloud",
                description = "Long press the mic and read the sentence clearly:",
                voicePrompt = "It is a sunny day."
            )
        )
    }

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
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
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

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LingoAvatar(
                expression = lingoExpr,
                modifier = Modifier.size(90.dp)
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
                        true -> stringResource(R.string.correct_prompt)
                        false -> stringResource(R.string.incorrect_prompt)
                        else -> if (currentQuestion.type == QuestionType.SPEAK_ALOUD) stringResource(R.string.speak_aloud_prompt) else stringResource(R.string.default_question_prompt)
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2C3E50)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = currentQuestion.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5C6FF2)
                )
                
                Text(
                    text = currentQuestion.description,
                    fontSize = 15.sp,
                    color = Color(0xFF7F8C8D),
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    when (currentQuestion.type) {
                        QuestionType.LISTENING_EMOJI -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(if (isPlayingVoice) Color(0xFFFFECE5) else Color(0xFFFF7052))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            coroutineScope.launch {
                                                isPlayingVoice = true
                                                delay(1000)
                                                isPlayingVoice = false
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isPlayingVoice) "🔊" else "🔈",
                                        fontSize = 32.sp
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                ChoiceGroup(
                                    options = currentQuestion.options,
                                    selectedOption = selectedOption,
                                    onOptionSelected = { selectedOption = it }
                                )
                            }
                        }
                        QuestionType.VOCABULARY, QuestionType.PHONICS -> {
                            ChoiceGroup(
                                options = currentQuestion.options,
                                selectedOption = selectedOption,
                                onOptionSelected = { selectedOption = it }
                            )
                        }
                        QuestionType.SORT_WORDS -> {
                            val availableWords = currentQuestion.wordsForSort.filter { !sortedWords.contains(it) }
                            
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceAround,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF8F9FA))
                                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                                        .padding(8.dp),
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
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    availableWords.forEach { word ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFFFD449))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    sortedWords = sortedWords + word
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(word, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
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
                                    text = currentQuestion.voicePrompt ?: "",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2C3E50),
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                if (isEvaluated) {
                                    Text(
                                        text = "Speech Score: $evaluationScore!",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2ECC71),
                                        fontSize = 16.sp
                                    )
                                } else if (isRecording) {
                                    Text(
                                        text = stringResource(R.string.recording_prompt),
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFFF7052),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            if (currentQuestion.type == QuestionType.SPEAK_ALOUD) {
                val scale by animateFloatAsState(if (isRecording) 1.25f else 1.0f, label = "MicScale")
                
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(if (isRecording) Color(0xFFFFE0E0) else Color(0xFFFFD449))
                        .clickable(
                            onClick = {
                                if (!isEvaluated) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    coroutineScope.launch {
                                        isRecording = true
                                        delay(1500)
                                        isRecording = false
                                        isEvaluated = true
                                        evaluationScore = (75..95).random()
                                        totalScore += evaluationScore
                                        answerState = true
                                        lingoExpr = LingoExpression.CELEBRATING
                                    }
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRecording) "🟥" else "🎤",
                        fontSize = 28.sp
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
                            val isCorrect = userAns == currentQuestion.correctAnswer
                            answerState = isCorrect
                            if (isCorrect) {
                                totalScore += 20
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
                        text = if (answerState == null) stringResource(R.string.verify) else stringResource(R.string.next_question),
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
