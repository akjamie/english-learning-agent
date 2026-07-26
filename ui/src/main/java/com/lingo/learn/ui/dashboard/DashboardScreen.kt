package com.lingo.learn.ui.dashboard

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
import com.lingo.learn.ui.R
import com.lingo.learn.ui.components.LingoAvatar
import com.lingo.learn.ui.components.LingoExpression
import com.lingo.learn.ui.components.ProgressRing
import com.lingo.learn.ui.components.StreakCounter

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
    modifier: Modifier = Modifier
) {
    var streakDays by remember { mutableStateOf(4) }
    var todayProgress by remember { mutableStateOf(0.0f) }
    val errorCount = 12

    val themeName = "School Life"
    val taskDuration = "15 Mins"
    val taskTarget = "5 Words + 2 Speech"

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
        // 1. Header (grade title and streak flame)
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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
            }

            StreakCounter(streakDays = streakDays)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Main Daily Task Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.today_goal),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7F8C8D)
                )

                // Progress ring containing Lingo mascot
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ProgressRing(
                        progress = todayProgress,
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 12f,
                        activeColor = gradeTheme.activeRingColor
                    )
                    LingoAvatar(
                        expression = lingoExpr,
                        modifier = Modifier.size(100.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = themeName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Est. time $taskDuration | Goal: $taskTarget",
                        fontSize = 13.sp,
                        color = Color(0xFF7F8C8D)
                    )
                }

                // Action start learning button
                Button(
                    onClick = onStartLearning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gradeTheme.primaryColor,
                        contentColor = gradeTheme.buttonContentColor
                    )
                ) {
                    Text(
                        text = if (todayProgress >= 1.0f) stringResource(R.string.completed_review) else stringResource(R.string.start_study),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Bottom shortcut card group
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(onClick = onPlanClick),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "📅", fontSize = 28.sp)
                    Column {
                        Text(text = stringResource(R.string.weekly_plan), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = stringResource(R.string.track_daily_progress), fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(onClick = onErrorBookClick),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📝", fontSize = 28.sp)
                        // Outstanding error counts badge
                        if (errorCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFF7052))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$errorCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Column {
                        Text(text = stringResource(R.string.error_book), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = stringResource(R.string.target_weak_areas), fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
