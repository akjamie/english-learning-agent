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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lingo.learn.domain.model.GameQuestion
import com.lingo.learn.domain.model.GameType
import com.lingo.learn.ui.components.LingoAvatar
import com.lingo.learn.ui.components.LingoExpression
import com.lingo.learn.ui.components.MicButton
import com.lingo.learn.ui.components.WordHighlightText
import com.lingo.learn.ui.dashboard.GradeTheme

/**
 * Stage 2: Consolidation Practice (read-along + mini-games).
 *
 * Per design spec 2.4.2, this stage has two sub-phases:
 * - READ_ALONG: TTS demonstrates the sentence, child holds mic to record, ASR
 *   evaluates with word-level green/orange highlighting (loose 60-point threshold).
 * - GAME: Tap-to-select mini-games (drag-match + listen-choose-image) with
 *   immediate correct/wrong feedback, particle effects, and 3-combo streak.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PracticeScreen(
    viewModel: LearningViewModel,
    theme: GradeTheme,
    onProceedToQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    val practicePhase by viewModel.practicePhase.collectAsState()

    AnimatedContent(
        targetState = practicePhase,
        transitionSpec = {
            slideInHorizontally { width -> width } + fadeIn() with
                    slideOutHorizontally { width -> -width } + fadeOut()
        },
        label = "PracticePhase",
        modifier = modifier
            .fillMaxSize()
            .background(theme.surfaceColor)
    ) { phase ->
        when (phase) {
            PracticePhase.READ_ALONG -> ReadAlongContent(viewModel, theme)
            PracticePhase.GAME -> GameContent(viewModel, theme)
        }
    }
}

//region Read-Along Sub-Phase

@Composable
private fun ReadAlongContent(
    viewModel: LearningViewModel,
    theme: GradeTheme
) {
    val readAlongState by viewModel.readAlongState.collectAsState()
    val session by viewModel.session.collectAsState()
    val currentSentence = session.readAlongSentences[readAlongState.currentIndex]
    val total = session.readAlongSentences.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress indicator
        Text(
            text = "Read Aloud ${readAlongState.currentIndex + 1} / $total",
            fontSize = 14.sp,
            color = Color(0xFF7F8C8D),
            modifier = Modifier.padding(top = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Lingo avatar
        val pronunciationResult = readAlongState.result
        val lingoExpr = when {
            readAlongState.isEvaluating -> LingoExpression.THINKING
            pronunciationResult != null && pronunciationResult.overallScore >= 80 -> LingoExpression.CELEBRATING
            pronunciationResult != null -> LingoExpression.HAPPY
            else -> LingoExpression.HAPPY
        }
        LingoAvatar(expression = lingoExpr, modifier = Modifier.size(80.dp))

        Spacer(modifier = Modifier.height(16.dp))

        // Sentence card
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
                    text = currentSentence.chineseHint,
                    fontSize = 14.sp,
                    color = Color(0xFF7F8C8D)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Show highlighted result or plain text
                if (pronunciationResult != null) {
                    WordHighlightText(
                        text = currentSentence.text,
                        wordScores = pronunciationResult.wordScores
                    )
                } else {
                    Text(
                        text = currentSentence.text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // TTS demo button
                TextButton(onClick = { viewModel.speakWord(currentSentence.text) }) {
                    Text("🔊 Listen first", color = theme.primaryColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Score display (when result available)
        if (pronunciationResult != null) {
            val score = pronunciationResult.overallScore
            val scoreColor = if (score >= 80) Color(0xFF52D68A) else Color(0xFFFFA726)

            Text(
                text = "Score: $score",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
            Text(
                text = pronunciationResult.feedback,
                fontSize = 14.sp,
                color = Color(0xFF7F8C8D),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.retryReadAlong() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("🔄 Try Again", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { viewModel.nextReadAlongSentence() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.primaryColor,
                        contentColor = theme.buttonContentColor
                    )
                ) {
                    Text(
                        text = if (readAlongState.currentIndex < total - 1) "Next ->" else "Games ->",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
            // Ergonomic bottom mic button for recording
            MicButton(
                isRecording = readAlongState.isRecording,
                onPressDown = { viewModel.startRecording() },
                onPressUp = { viewModel.stopRecording() }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

//endregion

//region Game Sub-Phase

@Composable
private fun GameContent(
    viewModel: LearningViewModel,
    theme: GradeTheme
) {
    val gameState by viewModel.gameState.collectAsState()
    val session by viewModel.session.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Check if all games are done
    if (gameState.currentIndex >= session.gameQuestions.size) {
        // Transition to quiz
        LaunchedEffect(Unit) {
            viewModel.nextGameQuestion() // This will set stage to QUIZ
        }
    }

    val currentQuestion = session.gameQuestions.getOrNull(gameState.currentIndex) ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: score and combo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Score: ${gameState.score}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            if (gameState.combo >= 2) {
                Text(
                    text = "🔥 ${gameState.combo} Combo!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF7052)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lingo avatar
        val lingoExpr = when {
            gameState.showComboEffect -> LingoExpression.EXCITED
            gameState.lastAnswerCorrect == true -> LingoExpression.CELEBRATING
            gameState.lastAnswerCorrect == false -> LingoExpression.SAD
            else -> LingoExpression.HAPPY
        }
        LingoAvatar(expression = lingoExpr, modifier = Modifier.size(70.dp))

        Spacer(modifier = Modifier.height(8.dp))

        // Combo effect animation
        if (gameState.showComboEffect) {
            Text(
                text = "✨ ${gameState.combo} Streak! ✨",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF7052)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Game question card
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
                    text = currentQuestion.prompt,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center
                )

                // Audio button for LISTEN_CHOOSE_IMAGE type
                val audioText = currentQuestion.audioText
                if (currentQuestion.type == GameType.LISTEN_CHOOSE_IMAGE && audioText != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.playGameAudio(audioText) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.primaryColor,
                            contentColor = theme.buttonContentColor
                        )
                    ) {
                        Text("🔊 Tap to Listen", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Answer options grid (2x2)
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            currentQuestion.options.chunked(2).forEach { rowOptions ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowOptions.forEachIndexed { indexInRow, option ->
                        val absoluteIndex = currentQuestion.options.indexOf(option)
                        GameOptionButton(
                            text = option,
                            theme = theme,
                            isSelected = gameState.lastAnswerCorrect != null,
                            isCorrect = absoluteIndex == currentQuestion.correctIndex,
                            isThisSelected = false,
                            enabled = gameState.lastAnswerCorrect == null,
                            onClick = {
                                viewModel.submitGameAnswer(absoluteIndex)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Next button (after answering)
        if (gameState.lastAnswerCorrect != null) {
            Button(
                onClick = { viewModel.nextGameQuestion() },
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
                    text = if (gameState.currentIndex < session.gameQuestions.size - 1) "Next ->" else "Quiz ->",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun RowScope.GameOptionButton(
    text: String,
    theme: GradeTheme,
    isSelected: Boolean,
    isCorrect: Boolean,
    isThisSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        !enabled && isCorrect -> Color(0xFF52D68A).copy(alpha = 0.2f)
        !enabled -> Color(0xFFFFA726).copy(alpha = 0.15f)
        else -> Color.White
    }
    val borderColor = when {
        !enabled && isCorrect -> Color(0xFF52D68A)
        !enabled -> Color(0xFFFFA726)
        else -> Color(0xFFECEFF1)
    }

    val scale by animateFloatAsState(
        targetValue = if (!enabled && isCorrect) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "OptionScale"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .height(72.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
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

//endregion
