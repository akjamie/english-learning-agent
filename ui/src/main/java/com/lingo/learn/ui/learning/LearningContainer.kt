package com.lingo.learn.ui.learning

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.lingo.learn.ui.dashboard.GradeTheme
import com.lingo.learn.ui.dashboard.getThemeForGrade

/**
 * Top-level container for the Sprint 2 daily learning flow.
 *
 * Observes [LearningViewModel.stage] and renders the appropriate screen with a
 * directional slide transition (up = forward, down = backward) per the design
 * spec section 2.4.2: "current stage card floats up, next stage bounces in from bottom".
 *
 * @param grade The child's selected grade, used for theme token hot-switching.
 * @param onExit Callback when the user finishes or exits the learning session.
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

    AnimatedContent(
        targetState = stage,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                // Forward: new stage slides up from bottom, old floats up
                slideInVertically { height -> height } + fadeIn() with
                        slideOutVertically { height -> -height } + fadeOut()
            } else {
                slideInVertically { height -> -height } + fadeIn() with
                        slideOutVertically { height -> height } + fadeOut()
            }
        },
        label = "LearningStage",
        modifier = modifier
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
