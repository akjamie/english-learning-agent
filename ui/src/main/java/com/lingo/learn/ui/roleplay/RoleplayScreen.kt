package org.akj.lingo.learn.ui.roleplay

import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    viewModel: RoleplayViewModel? = null,
    stateOverride: RoleplayScreenState? = null,
    onSendMessage: ((String) -> Unit)? = null,
    onStartRecording: (() -> Unit)? = null,
    onStopRecording: (() -> Unit)? = null
) {
    val activeViewModel = viewModel ?: if (stateOverride == null) hiltViewModel() else null
    val liveMessages by (activeViewModel?.messages?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val liveScenarios by (activeViewModel?.scenarios?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val liveScenario by (activeViewModel?.scenario?.collectAsState() ?: remember { mutableStateOf(null) })
    val liveRecording by (activeViewModel?.isRecording?.collectAsState() ?: remember { mutableStateOf(false) })
    val liveThinking by (activeViewModel?.isThinking?.collectAsState() ?: remember { mutableStateOf(false) })
    val liveSpeaking by (activeViewModel?.isSpeaking?.collectAsState() ?: remember { mutableStateOf(false) })
    val liveAudio by (activeViewModel?.audioToPlay?.collectAsState() ?: remember { mutableStateOf(null) })
    var submittedMessages by remember { mutableStateOf(emptyList<ChatMessage>()) }
    val messages = stateOverride?.messages?.plus(submittedMessages) ?: liveMessages
    val scenarios = stateOverride?.scenarios ?: liveScenarios
    val scenario = stateOverride?.scenario ?: liveScenario
    val isRecording = stateOverride?.isRecording ?: liveRecording
    val isThinking = stateOverride?.isThinking ?: liveThinking
    val isSpeaking = stateOverride?.isSpeaking ?: liveSpeaking
    val audioToPlay = if (stateOverride != null) null else liveAudio
    var textInput by remember { mutableStateOf("") }

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
                    activeViewModel?.onAudioPlaybackComplete()
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
                title = {
                    Column {
                        Text(
                            if (scenario != null) "${scenario!!.emoji} ${scenario!!.title}" else "Roleplay"
                        )
                        Text(
                            "Practice speaking with Lingo Fox",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { activeViewModel?.resetConversation() }) {
                        Text("Reset")
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
            // Sprint 11: scenario picker row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                scenarios.forEach { s ->
                    val selected = scenario?.id == s.id
                    FilterChip(
                        selected = selected,
                        onClick = { activeViewModel?.selectScenario(s.id) },
                        label = { Text("${s.emoji} ${s.title}") }
                    )
                }
            }

            // Header Avatar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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
                    modifier = Modifier.size(76.dp)
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

            // Sprint 11: text-input row + record button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message…") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )
                FilledIconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            val submitted = textInput.trim()
                            if (stateOverride != null) submittedMessages = submittedMessages + ChatMessage("user", submitted)
                            (onSendMessage ?: activeViewModel?.let { vm -> { text: String -> vm.sendUserMessage(text) } })?.invoke(submitted)
                            textInput = ""
                        }
                    },
                    enabled = textInput.isNotBlank() && !isThinking,
                    modifier = Modifier.semantics { contentDescription = "Send message" }
                ) {
                    Text("➤", fontSize = 16.sp)
                }
            }

            // Record Button Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                RecordButton(
                    isRecording = isRecording,
                    onStartRecording = { (onStartRecording ?: activeViewModel?.let { vm -> { vm.startRecording() } })?.invoke() },
                    onStopRecording = { (onStopRecording ?: activeViewModel?.let { vm -> { vm.stopRecordingAndSend() } })?.invoke() }
                )
            }
        }
    }
}

data class RoleplayScreenState(
    val messages: List<ChatMessage>,
    val scenarios: List<org.akj.lingo.learn.domain.model.RoleplayScenario>,
    val scenario: org.akj.lingo.learn.domain.model.RoleplayScenario?,
    val isRecording: Boolean = false,
    val isThinking: Boolean = false,
    val isSpeaking: Boolean = false
)

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
            .size(64.dp)
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
            }
            .semantics { contentDescription = if (isRecording) "Stop voice response" else "Record voice response" },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🎤",
            fontSize = 40.sp
        )
    }
}
