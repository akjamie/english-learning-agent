package com.lingo.learn.ui.learning

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lingo.learn.domain.model.QuizQuestion
import com.lingo.learn.domain.model.QuizQuestionType
import com.lingo.learn.ui.components.LingoAvatar
import com.lingo.learn.ui.components.LingoExpression
import com.lingo.learn.ui.components.MicButton
import com.lingo.learn.ui.components.ProgressRing
import com.lingo.learn.ui.components.QuizProgressBar
import com.lingo.learn.ui.dashboard.GradeTheme

/**
 * Stage 3: Daily Micro-Quiz.
 *
 * Per design spec 2.4.2 and section 6:
 * - Top: progress dots showing remaining questions
 * - Cards slide in from right, slide out to left
 * - Listening questions show headphone icon + "tap to listen" (prevents loud playback)
 * - Includes 1-2 error-book recurrence questions
 * - Result page: large arc progress + Lingo expression based on score
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun QuizScreen(
    viewModel: LearningViewModel,
    theme: GradeTheme,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quizState by viewModel.quizState.collectAsState()
    val readAlongState by viewModel.readAlongState.collectAsState()
    val session by viewModel.session.collectAsState()

    if (quizState.showResult) {
        QuizResultPage(
            score = quizState.score,
            total = session.quizQuestions.size,
            theme = theme,
            onComplete = onComplete
        )
        return
    }

    val currentQuestion = session.quizQuestions.getOrNull(quizState.currentIndex) ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.surfaceColor)
            .padding(horizontal = 20.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            QuizProgressBar(
                total = session.quizQuestions.size,
                current = quizState.currentIndex,
                activeColor = theme.primaryColor
            )
        }

        // Error book badge
        if (currentQuestion.isFromErrorBook) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color(0xFFFFECE5),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "From Error Book",
                    fontSize = 12.sp,
                    color = Color(0xFFFF7052),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lingo avatar
        val lingoExpr = when {
            quizState.lastAnswerCorrect == true -> LingoExpression.CELEBRATING
            quizState.lastAnswerCorrect == false -> LingoExpression.SAD
            else -> LingoExpression.THINKING
        }
        LingoAvatar(expression = lingoExpr, modifier = Modifier.size(64.dp))

        Spacer(modifier = Modifier.height(12.dp))

        // Question card
        AnimatedContent(
            targetState = quizState.currentIndex,
            transitionSpec = {
                slideInHorizontally { width -> width } + fadeIn() with
                        slideOutHorizontally { width -> -width } + fadeOut()
            },
            label = "QuizCard"
        ) { _ ->
            QuizQuestionCard(
                question = currentQuestion,
                theme = theme,
                lastAnswerCorrect = quizState.lastAnswerCorrect,
                readAlongState = readAlongState,
                onPlayAudio = { text -> viewModel.playQuizAudio(text) },
                onSelectAnswer = { index -> viewModel.submitQuizAnswer(index) },
                onStartRecording = { viewModel.startQuizRecording() },
                onStopRecording = { viewModel.submitQuizReadAloud(currentQuestion.audioText ?: "") }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Next button
        if (quizState.lastAnswerCorrect != null) {
            val isLast = quizState.currentIndex >= session.quizQuestions.size - 1
            Button(
                onClick = { viewModel.nextQuizQuestion() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.primaryColor,
                    contentColor = theme.buttonContentColor
                )
            ) {
                Text(
                    text = if (isLast) "See Results ->" else "Next ->",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun QuizQuestionCard(
    question: QuizQuestion,
    theme: GradeTheme,
    lastAnswerCorrect: Boolean?,
    readAlongState: ReadAlongState,
    onPlayAudio: (String) -> Unit,
    onSelectAnswer: (Int) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = question.question,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (question.type) {
                QuizQuestionType.LISTEN_CHOOSE_WORD -> {
                    Button(
                        onClick = { question.audioText?.let { onPlayAudio(it) } },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.primaryColor,
                            contentColor = theme.buttonContentColor
                        )
                    ) {
                        Text("Listen", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OptionGrid(
                        options = question.options,
                        correctIndex = question.correctIndex,
                        answered = lastAnswerCorrect != null,
                        theme = theme,
                        onSelect = onSelectAnswer
                    )
                }

                QuizQuestionType.IMAGE_CHOOSE_WORD -> {
                    OptionGrid(
                        options = question.options,
                        correctIndex = question.correctIndex,
                        answered = lastAnswerCorrect != null,
                        theme = theme,
                        onSelect = onSelectAnswer
                    )
                }

                QuizQuestionType.SPELL_FILL_BLANK -> {
                    Text(
                        text = "cla__room",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        question.options.forEachIndexed { index, letter ->
                            LetterOptionButton(
                                letter = letter,
                                isCorrect = index == question.correctIndex,
                                answered = lastAnswerCorrect != null,
                                onClick = { onSelectAnswer(index) }
                            )
                        }
                    }
                }

                QuizQuestionType.SENTENCE_ORDER -> {
                    Text(
                        text = "Tap words in the correct order:",
                        fontSize = 14.sp,
                        color = Color(0xFF7F8C8D)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    var selectedOrder by remember { mutableStateOf(listOf<String>()) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        question.options.forEach { word ->
                            val isSelected = selectedOrder.contains(word)
                            AssistChip(
                                onClick = {
                                    if (lastAnswerCorrect == null && !isSelected) {
                                        selectedOrder = selectedOrder + word
                                        if (selectedOrder.size == question.correctOrder.size) {
                                            val isCorrect = selectedOrder == question.correctOrder
                                            onSelectAnswer(if (isCorrect) 0 else -1)
                                        }
                                    }
                                },
                                label = {
                                    Text(
                                        text = if (isSelected) "${selectedOrder.indexOf(word) + 1}. $word" else word,
                                        fontSize = 16.sp
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isSelected) theme.primaryColor.copy(alpha = 0.3f) else Color(0xFFF7F9FA)
                                )
                            )
                        }
                    }
                }

                QuizQuestionType.READ_ALOUD -> {
                    val audioText = question.audioText
                    if (audioText != null) {
                        TextButton(onClick = { onPlayAudio(audioText) }) {
                            Text("Listen first", color = theme.primaryColor)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    MicButton(
                        isRecording = readAlongState.isRecording,
                        onPressDown = { onStartRecording() },
                        onPressUp = { onStopRecording() }
                    )
                    if (readAlongState.isEvaluating) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(
                            color = theme.primaryColor,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionGrid(
    options: List<String>,
    correctIndex: Int,
    answered: Boolean,
    theme: GradeTheme,
    onSelect: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.chunked(2).forEach { rowOptions ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowOptions.forEachIndexed { indexInRow, option ->
                    val absoluteIndex = options.indexOf(option)
                    QuizOptionButton(
                        text = option,
                        isCorrect = absoluteIndex == correctIndex,
                        answered = answered,
                        onClick = { onSelect(absoluteIndex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.QuizOptionButton(
    text: String,
    isCorrect: Boolean,
    answered: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        answered && isCorrect -> Color(0xFF52D68A).copy(alpha = 0.2f)
        answered -> Color(0xFFFFA726).copy(alpha = 0.15f)
        else -> Color(0xFFF7F9FA)
    }
    val borderColor = when {
        answered && isCorrect -> Color(0xFF52D68A)
        answered -> Color(0xFFFFA726)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = !answered, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LetterOptionButton(
    letter: String,
    isCorrect: Boolean,
    answered: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        answered && isCorrect -> Color(0xFF52D68A).copy(alpha = 0.2f)
        answered -> Color(0xFFFFA726).copy(alpha = 0.15f)
        else -> Color(0xFFF7F9FA)
    }
    val borderColor = when {
        answered && isCorrect -> Color(0xFF52D68A)
        answered -> Color(0xFFFFA726)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = !answered, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
    }
}

@Composable
private fun QuizResultPage(
    score: Int,
    total: Int,
    theme: GradeTheme,
    onComplete: () -> Unit
) {
    val progress = if (total > 0) score.toFloat() / total else 0f

    val lingoExpr = when {
        progress >= 0.8f -> LingoExpression.CELEBRATING
        progress >= 0.6f -> LingoExpression.HAPPY
        else -> LingoExpression.SAD
    }

    val resultMessage = when {
        progress >= 0.8f -> "Brilliant work! You're a star!"
        progress >= 0.6f -> "Good job! Keep it up!"
        else -> "Don't worry, practice makes perfect!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.surfaceColor)
            .padding(horizontal = 24.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LingoAvatar(expression = lingoExpr, modifier = Modifier.size(100.dp))

        Spacer(modifier = Modifier.height(16.dp))

        // Score ring
        Box(
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            ProgressRing(
                progress = progress,
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 12f,
                activeColor = theme.activeRingColor
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$score",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                Text(
                    text = "/ $total",
                    fontSize = 16.sp,
                    color = Color(0xFF7F8C8D)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = resultMessage,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = theme.primaryColor,
                contentColor = theme.buttonContentColor
            )
        ) {
            Text(text = "See Today's Achievements ->", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
