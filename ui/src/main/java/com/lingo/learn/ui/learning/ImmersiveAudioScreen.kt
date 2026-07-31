package org.akj.lingo.learn.ui.learning

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.akj.lingo.learn.domain.model.SubtitleLine
import org.akj.lingo.learn.ui.R
import org.akj.lingo.learn.ui.components.LingoAvatar
import org.akj.lingo.learn.ui.components.LingoExpression
import org.akj.lingo.learn.ui.dashboard.GradeTheme
import kotlinx.coroutines.launch

/**
 * Stage 1: Immersive Audio Import.
 *
 * Per design spec 2.4.2:
 * - Top: theme image card (full-width, atmosphere)
 * - Audio player with progress bar, play/pause, 0.75x/1x speed toggle
 * - Subtitle area with sentence-by-sentence highlighting synced to playback
 * - New word popup cards (half-screen, dismissible, non-interrupting)
 * - New word bubbles collected at top corner
 * - Lingo mascot reacts to playback state
 */
@Composable
fun ImmersiveAudioScreen(
    viewModel: LearningViewModel,
    theme: GradeTheme,
    onProceed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val audioState by viewModel.audioPlayer.state.collectAsState()
    val session by viewModel.session.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Track which new word popup is currently shown (one at a time for clarity)
    var activePopupWord by remember { mutableStateOf<String?>(null) }

    // Auto-show popup when new words appear
    LaunchedEffect(audioState.poppedNewWords) {
        if (activePopupWord == null && audioState.poppedNewWords.isNotEmpty()) {
            activePopupWord = audioState.poppedNewWords.first()
        }
    }

    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll subtitle list to the active line
    LaunchedEffect(audioState.currentSubtitleIndex) {
        if (audioState.currentSubtitleIndex >= 0) {
            scope.launch {
                lazyListState.animateScrollToItem(audioState.currentSubtitleIndex)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.surfaceColor)
            .padding(horizontal = 20.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Header with theme image and close hint
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = theme.primaryColor.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🏫", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = session.theme,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                }
            }
        }

        // 2. Lingo avatar reacts to playback
        val lingoExpr = when {
            audioState.isFinished -> LingoExpression.CELEBRATING
            audioState.isPlaying -> LingoExpression.HAPPY
            else -> LingoExpression.THINKING
        }
        LingoAvatar(
            expression = lingoExpr,
            modifier = Modifier.size(80.dp).padding(vertical = 8.dp)
        )

        // 3. New word bubbles (collected at top, dismissible)
        if (audioState.poppedNewWords.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                audioState.poppedNewWords.forEach { word ->
                    AssistChip(
                        onClick = { viewModel.speakWord(word) },
                        label = { Text(text = word, fontSize = 13.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = theme.primaryColor.copy(alpha = 0.2f)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 4. Subtitle list with highlighting
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(session.subtitleLines) { line ->
                SubtitleLineItem(
                    line = line,
                    isActive = line.id - 1 == audioState.currentSubtitleIndex,
                    theme = theme
                )
            }
        }

        // 5. Audio player controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Progress bar
                val progress = if (audioState.durationMs > 0) {
                    (audioState.positionMs.toFloat() / audioState.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = theme.primaryColor,
                    trackColor = Color(0xFFECEFF1)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(audioState.positionMs),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = formatTime(audioState.durationMs),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Control row: speed toggle, play/pause, skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed toggle (0.75x / 1.0x)
                    TextButton(
                        onClick = {
                            val newSpeed = if (audioState.speed == 1.0f) 0.75f else 1.0f
                            viewModel.setAudioSpeed(newSpeed)
                        }
                    ) {
                        Text(
                            text = if (audioState.speed == 1.0f) "1.0x" else "0.75x",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (audioState.speed != 1.0f) theme.primaryColor else Color.Gray
                        )
                    }

                    // Play/Pause main button
                    Button(
                        onClick = { viewModel.toggleAudioPlayback() },
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.primaryColor,
                            contentColor = theme.buttonContentColor
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (audioState.isPlaying) "⏸" else "▶",
                            fontSize = 24.sp
                        )
                    }

                    // Proceed button (visible when finished)
                    if (audioState.isFinished) {
                        TextButton(onClick = onProceed) {
                            Text(
                                text = "Next →",
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryColor
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    // 6. New word popup card (half-screen, dismissible)
    if (activePopupWord != null) {
        NewWordPopupCard(
            word = activePopupWord!!,
            onSpeak = { viewModel.speakWord(activePopupWord!!) },
            onDismiss = {
                viewModel.dismissNewWord(activePopupWord!!)
                activePopupWord = null
            }
        )
    }
}

@Composable
private fun SubtitleLineItem(
    line: SubtitleLine,
    isActive: Boolean,
    theme: GradeTheme
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) theme.primaryColor.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(300),
        label = "SubtitleBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF2C3E50) else Color(0xFFBDC3C7),
        animationSpec = tween(300),
        label = "SubtitleText"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = line.text,
            fontSize = if (isActive) 18.sp else 16.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NewWordPopupCard(
    word: String,
    onSpeak: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .clickable(enabled = false) {}, // Prevent click-through dismiss
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // New word label
                Text(
                    text = "✨ New Word!",
                    fontSize = 14.sp,
                    color = Color(0xFFFF7052),
                    fontWeight = FontWeight.Bold
                )
                // The word
                Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    Text(
                        text = word,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50),
                        maxLines = 1,
                        softWrap = false
                    )
                }
                // Speak button
                Button(
                    onClick = onSpeak,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF52D68A),
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "🔊 Listen", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                // Dismiss hint
                TextButton(onClick = onDismiss) {
                    Text(text = "Got it! ↓", color = Color.Gray)
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
