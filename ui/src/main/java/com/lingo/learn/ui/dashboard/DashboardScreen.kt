package org.akj.lingo.learn.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.akj.lingo.learn.ui.R
import org.akj.lingo.learn.ui.components.LingoAvatar
import org.akj.lingo.learn.ui.components.LingoExpression
import org.akj.lingo.learn.ui.components.ProgressRing
import org.akj.lingo.learn.ui.components.StreakCounter

// Define multi-stage visual theme tokens
data class GradeTheme(
    val primaryColor: Color,
    val surfaceColor: Color,
    val activeRingColor: Color,
    val buttonContentColor: Color
)

fun getThemeForGrade(grade: String): GradeTheme {
    return when {
        grade.contains("小学") || grade.contains("Grade") -> GradeTheme(
            primaryColor = Color(0xFFFFD449),      // Primary Macaron Yellow theme
            surfaceColor = Color(0xFFFFFDF5),
            activeRingColor = Color(0xFFFFD449),
            buttonContentColor = Color(0xFF2C3E50)
        )
        grade.contains("初") || grade.contains("Junior") -> GradeTheme(
            primaryColor = Color(0xFF5C6FF2),      // Junior High Purple theme
            surfaceColor = Color(0xFFF7F8FF),
            activeRingColor = Color(0xFF5C6FF2),
            buttonContentColor = Color.White
        )
        else -> GradeTheme(
            primaryColor = Color(0xFF1C2F5E),      // Senior High Dark Blue theme
            surfaceColor = Color(0xFFF2F4F8),
            activeRingColor = Color(0xFF1C2F5E),
            buttonContentColor = Color.White
        )
    }
}

@Composable
fun DashboardScreen(
    grade: String,
    onStartLearning: () -> Unit,
    onPlanClick: () -> Unit,
    onErrorBookClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onRoleplayClick: () -> Unit = {},
    onUpdateLevel: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    val uiState by viewModel.uiState.collectAsState()

    // Sprint 8: Diagnostic Calibration prompt dialog
    uiState.calibrationPrompt?.let { reason ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissCalibrationPrompt() },
            title = { Text("📊 Level Check-In", fontWeight = FontWeight.Bold) },
            text = { Text(reason, lineHeight = 22.sp) },
            confirmButton = {
                Button(onClick = {
                    viewModel.dismissCalibrationPrompt()
                    onUpdateLevel() // Sprint 12: re-run the diagnosis to recalibrate the level
                }) {
                    Text("Update My Level")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCalibrationPrompt() }) {
                    Text("Maybe Later")
                }
            }
        )
    }

    val streakDays = uiState.streakDays
    val todayProgress = uiState.todayProgress
    val errorCount = uiState.errorCount
    val themeName = uiState.themeName
    val taskDuration = uiState.taskDuration
    val taskTarget = uiState.taskTarget

    val lingoExpr = if (todayProgress >= 1.0f) LingoExpression.CELEBRATING else LingoExpression.HAPPY
    
    // Dynamically resolve visual theme based on grade
    val gradeTheme = remember(grade) { getThemeForGrade(grade) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gradeTheme.surfaceColor)
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Header (grade title, streak flame, and settings button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = grade,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7F8C8D)
                )
                Text(
                    text = stringResource(R.string.welcome_back),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
            }

            StreakCounter(streakDays = streakDays)
        }

        // Sprint 10: Visible growth — XP progress bar, level badge & makeup balance
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Level badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(gradeTheme.primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Lv ${uiState.level}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = gradeTheme.buttonContentColor
                        )
                        Text(
                            text = "⭐",
                            fontSize = 12.sp
                        )
                    }
                }

                // XP progress
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "${uiState.xpIntoLevel} / ${uiState.xpForNextLevel} XP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                        Text(
                            text = "Total ${uiState.totalXp} XP",
                            fontSize = 11.sp,
                            color = Color(0xFF7F8C8D)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // Linear XP progress bar
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFECEFF1))
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxWidth(uiState.xpProgress.coerceIn(0f, 1f))
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(gradeTheme.primaryColor)
                        )
                    }
                }

                // Makeup card balance
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎟️", fontSize = 20.sp)
                    Text(
                        text = "${uiState.makeupCardsLeft}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Text(
                        text = "Cards",
                        fontSize = 10.sp,
                        color = Color(0xFF7F8C8D)
                    )
                }
            }
        }

        // Sprint 10.5: offline-mode banner (cloud AI not configured)
        if (uiState.isOfflineMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "⚠️ Offline mode — AI features need a model configured in Settings. Cached plan & practice still work.",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    fontSize = 12.sp,
                    color = Color(0xFFE67E22),
                    lineHeight = 16.sp
                )
            }
        }

        if (!uiState.greetingMessage.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                LingoAvatar(
                    expression = LingoExpression.HAPPY,
                    modifier = Modifier.size(48.dp).padding(top = 4.dp)
                )
                Spacer(Modifier.width(12.dp))
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = gradeTheme.primaryColor.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 20.dp,
                        bottomEnd = 20.dp,
                        bottomStart = 20.dp
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = uiState.greetingMessage!!,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2C3E50),
                        lineHeight = 20.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Main Daily Task Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.White, Color(0xFFF9FAFF))
                        )
                    )
                    .padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.today_goal),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7F8C8D)
                    )

                    // Progress ring containing Lingo mascot
                    Box(
                        modifier = Modifier.size(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ProgressRing(
                            progress = todayProgress,
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 14f,
                            activeColor = gradeTheme.activeRingColor
                        )
                        LingoAvatar(
                            expression = lingoExpr,
                            modifier = Modifier.size(120.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = themeName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Est. time $taskDuration | Goal: $taskTarget",
                            fontSize = 15.sp,
                            color = Color(0xFF7F8C8D)
                        )
                    }

                    // Action start learning button
                    Button(
                        onClick = onStartLearning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = gradeTheme.primaryColor,
                            contentColor = gradeTheme.buttonContentColor
                        )
                    ) {
                        Text(
                            text = if (todayProgress >= 1.0f) stringResource(R.string.completed_review) else stringResource(R.string.start_study),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // 3. Weekend Roleplay Shortcut
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onRoleplayClick() },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎭", fontSize = 32.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Weekend Roleplay",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Text(
                        text = "Practice speaking with Lingo Fox!",
                        fontSize = 14.sp,
                        color = Color(0xFF7F8C8D)
                    )
                }
            }
        }
    }
}
