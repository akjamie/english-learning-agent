package com.lingo.learn.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

enum class OnboardingStep {
    Welcome,
    GradeSelect,
    TextbookConfirm,
    Diagnosis,
    DiagnosisResult
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingContainer(
    onFinished: (grade: String, textbook: String, level: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.Welcome) }
    var selectedGrade by remember { mutableStateOf("") }
    var selectedTextbook by remember { mutableStateOf("") }
    var calculatedLevel by remember { mutableStateOf("A") }

    // Smooth slide horizontal animation transition
    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                // Slide right: new step enters from right, old exits to left
                slideInHorizontally { width -> width } + fadeIn() with
                        slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                // Slide left: new step enters from left, old exits to right
                slideInHorizontally { width -> -width } + fadeIn() with
                        slideOutHorizontally { width -> width } + fadeOut()
            }
        },
        label = "OnboardingTransition"
    ) { step ->
        when (step) {
            OnboardingStep.Welcome -> {
                WelcomeScreen(
                    onStartClick = { currentStep = OnboardingStep.GradeSelect }
                )
            }
            OnboardingStep.GradeSelect -> {
                GradeSelectScreen(
                    onGradeSelected = {
                        selectedGrade = it
                        currentStep = OnboardingStep.TextbookConfirm
                    }
                )
            }
            OnboardingStep.TextbookConfirm -> {
                TextbookConfirmScreen(
                    onConfirm = {
                        selectedTextbook = it
                        currentStep = OnboardingStep.Diagnosis
                    }
                )
            }
            OnboardingStep.Diagnosis -> {
                DiagnosisScreen(
                    onDiagnosisFinished = {
                        calculatedLevel = it
                        currentStep = OnboardingStep.DiagnosisResult
                    }
                )
            }
            OnboardingStep.DiagnosisResult -> {
                DiagnosisResultScreen(
                    level = calculatedLevel,
                    onStartLearning = {
                        onFinished(selectedGrade, selectedTextbook, calculatedLevel)
                    }
                )
            }
        }
    }
}
