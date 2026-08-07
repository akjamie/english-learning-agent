package org.akj.lingo.learn.ui.learning

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.akj.lingo.learn.domain.model.QuizQuestion
import org.akj.lingo.learn.domain.model.QuizQuestionType
import org.akj.lingo.learn.ui.components.AutoResizeText
import org.akj.lingo.learn.ui.components.LingoAvatar
import org.akj.lingo.learn.ui.components.LingoExpression
import org.akj.lingo.learn.ui.components.MicButton
import org.akj.lingo.learn.ui.components.QuizProgressBar
import org.akj.lingo.learn.ui.dashboard.GradeTheme

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
                hintLevel = quizState.hintLevel,
                dynamicHint = quizState.dynamicHint,
                isGeneratingHint = quizState.isGeneratingHint,
                readAlongState = readAlongState,
                onPlayAudio = { text -> viewModel.playQuizAudio(text) },
                onSelectAnswer = { index -> viewModel.submitQuizAnswer(index) },
                onProductionSubmit = { text -> viewModel.submitProductionAnswer(text) },
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
        } else {
            // Action Buttons: Hint and Skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.incrementHint() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "💡 Hint (${quizState.hintLevel}/3)",
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(
                    onClick = { viewModel.skipQuizQuestion() },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("⏭️ Skip (No Penalty)", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
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
    hintLevel: Int,
    dynamicHint: String?,
    isGeneratingHint: Boolean,
    readAlongState: ReadAlongState,
    onPlayAudio: (String) -> Unit,
    onSelectAnswer: (Int) -> Unit,
    onProductionSubmit: (String) -> Unit,
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
            AutoResizeText(
                text = question.question,
                minFontSize = 14.sp,
                maxFontSize = 20.sp,
                maxLines = 3,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Progressive Hint Display
            if (isGeneratingHint) {
                Surface(color = Color(0xFFFFF9E6), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFFD48806), strokeWidth = 2.dp)
                        Text(text = "Lingo Fox is thinking...", color = Color(0xFFD48806))
                    }
                }
            } else if (dynamicHint != null) {
                Surface(color = Color(0xFFFFF9E6), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Text(text = "💡 $dynamicHint", modifier = Modifier.padding(12.dp), color = Color(0xFFD48806))
                }
            } else if (hintLevel > 0) {
                val hintText = when (hintLevel) {
                    1 -> "Translation: [中文翻译: ${question.audioText ?: question.options.getOrNull(question.correctIndex) ?: '?'}]"
                    2 -> "First letter: ${question.options.getOrNull(question.correctIndex)?.firstOrNull() ?: '?'}"
                    3 -> "Answer: ${question.options.getOrNull(question.correctIndex) ?: '?'}"
                    else -> ""
                }
                Surface(color = Color(0xFFFFF9E6), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Text(text = "💡 $hintText", modifier = Modifier.padding(12.dp), color = Color(0xFFD48806))
                }
            }

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
                    AutoResizeText(
                        text = question.question,
                        minFontSize = 16.sp,
                        maxFontSize = 28.sp,
                        maxLines = 2,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50),
                        modifier = Modifier.fillMaxWidth()
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

                QuizQuestionType.SPELLING, QuizQuestionType.DICTATION -> {
                    var textInput by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (question.type == QuizQuestionType.SPELLING) "Type the word" else "Type the sentence") },
                        singleLine = true,
                        enabled = lastAnswerCorrect == null
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (lastAnswerCorrect == null) {
                        Button(
                            onClick = {
                                onSelectAnswer(-1) // fallback; real scoring happens in ViewModel
                                onProductionSubmit(textInput)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                        ) {
                            Text("Submit", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                QuizQuestionType.SENTENCE_WRITING -> {
                    var textInput by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Write a sentence with '${question.audioText ?: ""}'") },
                        enabled = lastAnswerCorrect == null
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (lastAnswerCorrect == null) {
                        Button(
                            onClick = { onProductionSubmit(textInput) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                        ) {
                            Text("Submit", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                QuizQuestionType.CVC_BUILD -> {
                    AutoResizeText(
                        text = "Listen: ${question.audioText ?: ""}",
                        minFontSize = 13.sp,
                        maxFontSize = 16.sp,
                        maxLines = 2,
                        color = Color(0xFF7F8C8D),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = question.question,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Reuse letter-tile grid; the child taps tiles in the correct order.
                    var tappedTiles by remember { mutableStateOf(listOf<String>()) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        question.options.forEachIndexed { index, tile ->
                            val isSelected = tappedTiles.contains(tile)
                            LetterOptionButton(
                                letter = tile,
                                isCorrect = false,
                                answered = false,
                                onClick = {
                                    if (lastAnswerCorrect == null && !isSelected) {
                                        tappedTiles = tappedTiles + tile
                                        if (tappedTiles.size == question.correctOrder.size) {
                                            val isCorrect = tappedTiles == question.correctOrder
                                            onSelectAnswer(if (isCorrect) 0 else -1)
                                        }
                                    }
                                }
                            )
                        }
                    }
                    if (tappedTiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tapped: ${tappedTiles.joinToString("")}",
                            fontSize = 14.sp,
                            color = Color(0xFF7F8C8D)
                        )
                    }
                }

                QuizQuestionType.ONSET_RIME -> {
                    AutoResizeText(
                        text = "Listen: ${question.audioText ?: ""}",
                        minFontSize = 13.sp,
                        maxFontSize = 16.sp,
                        maxLines = 2,
                        color = Color(0xFF7F8C8D),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = question.question,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OptionGrid(
                        options = question.options,
                        correctIndex = question.correctIndex,
                        answered = lastAnswerCorrect != null,
                        theme = theme,
                        onSelect = onSelectAnswer
                    )
                }

                QuizQuestionType.MINIMAL_PAIRS -> {
                    AutoResizeText(
                        text = question.question,
                        minFontSize = 14.sp,
                        maxFontSize = 18.sp,
                        maxLines = 3,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { question.audioText?.let { onPlayAudio(it) } },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.primaryColor,
                            contentColor = theme.buttonContentColor
                        )
                    ) {
                        Text("Listen again", fontWeight = FontWeight.Bold)
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
            .clickable(enabled = !answered, onClick = onClick)
            .semantics { contentDescription = "Answer: $text" },
        contentAlignment = Alignment.Center
    ) {
        AutoResizeText(
            text = text,
            minFontSize = 13.sp,
            maxFontSize = 16.sp,
            maxLines = 2,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
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
            .clickable(enabled = !answered, onClick = onClick)
            .semantics { contentDescription = "Letter: $letter" },
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


