package org.akj.lingo.learn.ui.planning

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.akj.lingo.learn.ui.components.LingoAvatar
import org.akj.lingo.learn.ui.components.LingoExpression

@Composable
fun PlanGeneratingScreen(
    grade: String,
    diagnosticLevel: String,
    onPlanGenerated: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlanGeneratingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.generatePlan(grade, diagnosticLevel)
    }

    // Show persistent Snackbar with Retry when generation fails
    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage
        if (msg != null) {
            val result = snackbarHostState.showSnackbar(
                message = "❌ $msg",
                actionLabel = "Retry",
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.generatePlan(grade, diagnosticLevel)
            }
        }
    }

    // Brief success Snackbar before navigating away
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            snackbarHostState.showSnackbar(
                message = "🎉 Your AI plan is ready!",
                duration = SnackbarDuration.Short
            )
            onPlanGenerated()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    Scaffold(
        modifier = modifier,
        containerColor = Color(0xFFFFFDF5),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (state.errorMessage != null) Color(0xFFE53935) else Color(0xFF43A047),
                    contentColor = Color.White,
                    actionColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))

            Text(text = "🧠", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Crafting Your AI Plan",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            Spacer(Modifier.height(8.dp))

            LingoAvatar(
                expression = if (state.isGenerating) LingoExpression.THINKING else LingoExpression.HAPPY,
                modifier = Modifier.size(100.dp)
            )

            Spacer(Modifier.height(24.dp))

            if (state.errorMessage != null) {
                // Inline error card — Snackbar above is the primary alert
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.errorMessage!!,
                            fontSize = 14.sp,
                            color = Color(0xFFE67E22),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.generatePlan(grade, diagnosticLevel) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Retry")
                            }
                            Button(
                                onClick = { onBack() },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Back")
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = state.progressText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF5C6FF2),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
                Spacer(Modifier.height(40.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
                        val delay = i * 200
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, delayMillis = delay, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dot_$i"
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .alpha(alpha)
                                .background(
                                    color = Color(0xFF5C6FF2),
                                    shape = RoundedCornerShape(6.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}
