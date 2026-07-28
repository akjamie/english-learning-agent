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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import org.akj.lingo.learn.ui.dashboard.GradeTheme
import org.akj.lingo.learn.ui.dashboard.getThemeForGrade

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

    // Pass grade to ViewModel for grade-adaptive content generation
    LaunchedEffect(grade) { viewModel.setGrade(grade) }

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
                    onComplete = { viewModel.proceedToComplete() }
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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
