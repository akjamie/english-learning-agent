package org.akj.lingo.learn.ui.roleplay

import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.akj.lingo.learn.domain.model.ChatMessage
import org.akj.lingo.learn.ui.components.LingoAvatar
import org.akj.lingo.learn.ui.components.LingoExpression
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleplayScreen(
    onNavigateBack: () -> Unit,
    viewModel: RoleplayViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val audioToPlay by viewModel.audioToPlay.collectAsState()

    val listState = rememberLazyListState()

    // Auto-scroll to bottom when messages change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Audio Player for TTS
    DisposableEffect(audioToPlay) {
        var mediaPlayer: MediaPlayer? = null
        if (audioToPlay != null && audioToPlay!!.exists()) {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioToPlay!!.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    viewModel.onAudioPlaybackComplete()
                }
            }
        }
        onDispose {
            mediaPlayer?.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekend Roleplay") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Avatar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val expression = when {
                    isSpeaking -> LingoExpression.HAPPY
                    isThinking -> LingoExpression.THINKING
                    isRecording -> LingoExpression.EXCITED
                    else -> LingoExpression.HAPPY
                }
                LingoAvatar(
                    expression = expression,
                    modifier = Modifier.size(100.dp)
                )
            }

            // Chat History
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages.filter { it.role != "system" }) { message ->
                    ChatBubble(message = message)
                }
                
                if (isThinking) {
                    item {
                        TypingIndicatorBubble()
                    }
                }
            }

            // Record Button Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                RecordButton(
                    isRecording = isRecording,
                    onStartRecording = { viewModel.startRecording() },
                    onStopRecording = { viewModel.stopRecordingAndSend() }
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val backgroundColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isUser) {
        RoundedCornerShape(20.dp, 20.dp, 0.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 0.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            shape = shape,
            color = backgroundColor,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    lineHeight = 24.sp
                ),
                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun TypingIndicatorBubble() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 0.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.widthIn(max = 100.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypingDot(delayMillis = 0)
                TypingDot(delayMillis = 150)
                TypingDot(delayMillis = 300)
            }
        }
    }
}

@Composable
fun TypingDot(delayMillis: Int) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing, delayMillis = delayMillis),
            repeatMode = RepeatMode.Reverse
        )
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = alpha))
    )
}

@Composable
fun RecordButton(
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(80.dp)
            .clip(CircleShape)
            .background(if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onStartRecording()
                        tryAwaitRelease()
                        onStopRecording()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🎤",
            fontSize = 40.sp
        )
    }
}
