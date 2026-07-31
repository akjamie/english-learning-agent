package org.akj.lingo.learn.ui.learning

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.window.Dialog
import org.akj.lingo.learn.ui.dashboard.GradeTheme
import org.akj.lingo.learn.ui.dashboard.getThemeForGrade
import org.akj.lingo.learn.ui.components.SpeechBubble

/**
 * Top-level container for the Sprint 2 daily learning flow with gamified journey progress.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LearningContainer(
    grade: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LearningViewModel = hiltViewModel()
) {
    val stage by viewModel.stage.collectAsState()
    val theme = remember(grade) { getThemeForGrade(grade) }
    val restoredFromCheckpoint by viewModel.restoredFromCheckpoint.collectAsState()

    // Pass grade to ViewModel for grade-adaptive content generation
    LaunchedEffect(grade) { viewModel.setGrade(grade) }

    // Auto-pause when app goes to background
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.pauseTask()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Resume checkpoint dialog
    if (restoredFromCheckpoint) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Welcome back!") },
            text = { Text("You have an unfinished session. Continue where you left off?") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissCheckpoint() }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.dismissCheckpoint()
                    viewModel.pauseTask()
                }) {
                    Text("Start Over")
                }
            }
        )
    }

    // Emotional intervention dialog
    val showIntervention by viewModel.showIntervention.collectAsState()
    if (showIntervention) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Need a break?") },
            text = {
                Column {
                    Text("You've been working hard! How about taking a short break or switching things up?")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.acceptSkipStage() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Skip This Stage (no penalty)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.acceptContinue() }) {
                    Text("Keep Going")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.acceptRest() }) {
                    Text("Take a Break")
                }
            }
        )
    }

    // Sprint 7: Level-up celebration overlay
    val showLevelUp by viewModel.showLevelUp.collectAsState()
    val levelInfo by viewModel.levelInfo.collectAsState()
    if (showLevelUp) {
        Dialog(
            onDismissRequest = { viewModel.dismissLevelUp() },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = true)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFFFFF8E1))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Level ${levelInfo.level}!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2C3E50)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You're growing fast! Keep it up!",
                        fontSize = 16.sp,
                        color = Color(0xFF7F8C8D)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.dismissLevelUp() },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                    ) {
                        Text("Awesome!", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Sprint 7: Makeup-card active choice dialog (streak break)
    val showMakeupPrompt by viewModel.showMakeupPrompt.collectAsState()
    val makeupState by viewModel.makeupState.collectAsState()
    if (showMakeupPrompt && makeupState != null) {
        AlertDialog(
            onDismissRequest = { viewModel.declineMakeupCard() },
            title = { Text("🎟️ Save your streak?") },
            text = {
                Column {
                    Text("Looks like you missed a day. Use a makeup card to keep your streak going?")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Cards left this month: ${makeupState?.cardsLeft ?: 0}",
                        fontWeight = FontWeight.Bold,
                        color = theme.primaryColor
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.useMakeupCard() }) {
                    Text("Use a Card")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.declineMakeupCard() }) {
                    Text("No thanks")
                }
            }
        )
    }

    // Handle system back button
    BackHandler {
        onExit()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.surfaceColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Gamified Journey Progress Header
        LearningJourneyHeader(
            currentStage = stage,
            theme = theme,
            onClose = onExit
        )

        // Lingo Observation bubble (Sprint 6) — non-blocking, auto-dismisses
        val observation by viewModel.observation.collectAsState()
        val currentObservation = observation
        if (currentObservation != null) {
            SpeechBubble(
                message = currentObservation.message,
                visible = true,
                onDismiss = { viewModel.dismissObservation() }
            )
        }

        // Stage Card Content with Smooth Vertical Spring Transitions
        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { height -> height } + fadeIn() togetherWith
                            slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { height -> -height } + fadeOut()
                } else {
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { height -> -height } + fadeIn() togetherWith
                            slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { height -> height } + fadeOut()
                }
            },
            label = "LearningStage",
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) { currentStage ->
            when (currentStage) {
                LearningStage.PRE_TEACH -> PreTeachScreen(
                    viewModel = viewModel,
                    theme = theme,
                    onProceed = { viewModel.proceedToImmersion() }
                )
                LearningStage.IMMERSION -> ImmersiveAudioScreen(
                    viewModel = viewModel,
                    theme = theme,
                    onProceed = { viewModel.proceedToPractice() }
                )
                LearningStage.PRACTICE -> PracticeScreen(
                    viewModel = viewModel,
                    theme = theme,
                    onProceedToQuiz = { /* handled inside via viewModel state */ }
                )
                LearningStage.QUIZ -> QuizScreen(
                    viewModel = viewModel,
                    theme = theme,
                    onComplete = { /* transitions to COMPLETE handled in ViewModel */ }
                )
                LearningStage.COMPLETE -> TaskCompleteScreen(
                    viewModel = viewModel,
                    theme = theme,
                    onExit = onExit
                )
            }
        }
    }
}

@Composable
fun LearningJourneyHeader(
    currentStage: LearningStage,
    theme: GradeTheme,
    onClose: () -> Unit
) {
    val stages = listOf(
        Pair(LearningStage.PRE_TEACH, "💡 Warm-up"),
        Pair(LearningStage.IMMERSION, "🎧 Immersion"),
        Pair(LearningStage.PRACTICE, "🎤 Practice"),
        Pair(LearningStage.QUIZ, "🧩 Quiz"),
        Pair(LearningStage.COMPLETE, "🏆 Win")
    )

    Surface(
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit Learning",
                    tint = Color(0xFF2C3E50)
                )
            }

            // Stepper Map
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                stages.forEachIndexed { index, (stageItem, label) ->
                    val isPassed = currentStage.ordinal > stageItem.ordinal
                    val isCurrent = currentStage == stageItem

                    val chipColor = when {
                        isCurrent -> theme.primaryColor
                        isPassed -> Color(0xFF2ECC71)
                        else -> Color(0xFFECEFF1)
                    }

                    val textColor = when {
                        isCurrent -> theme.buttonContentColor
                        isPassed -> Color.White
                        else -> Color(0xFF7F8C8D)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(chipColor)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}
