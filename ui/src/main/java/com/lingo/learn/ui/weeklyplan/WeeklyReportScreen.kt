package org.akj.lingo.learn.ui.weeklyplan

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import org.akj.lingo.learn.ui.components.ProgressRing
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeeklyReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF5))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📊", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Weekly Report", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF2C3E50))
                }
            },
            actions = {
                if (!uiState.isLoading) {
                    IconButton(onClick = { shareReport(context, viewModel) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Report", tint = Color(0xFF5C6FF2))
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFFDF5))
        )

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF5C6FF2))
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Grade banner
            val gradeMessage = viewModel.getGradeMessage(uiState.weeklyAccuracy)
            val gradeColor = Color(viewModel.getGradeColor(uiState.weeklyAccuracy))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = gradeColor.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(gradeMessage, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = gradeColor)
                    if (uiState.themeName.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Theme: ${uiState.themeName}", fontSize = 14.sp, color = Color(0xFF7F8C8D))
                    }
                }
            }

            // Weekly accuracy ring
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Weekly Accuracy", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                    Spacer(Modifier.height(16.dp))

                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ProgressRing(
                            progress = uiState.weeklyAccuracy,
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 14f,
                            activeColor = gradeColor
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${(uiState.weeklyAccuracy * 100).toInt()}%",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = gradeColor
                            )
                            Text(
                                "Monthly: ${(uiState.monthlyAccuracy * 100).toInt()}%",
                                fontSize = 12.sp,
                                color = Color(0xFF7F8C8D)
                            )
                        }
                    }
                }
            }

            // Stat cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("🔥 Streak", "${uiState.streakDays} days", modifier = Modifier.weight(1f))
                StatCard("📚 Sessions", "${uiState.totalSessions}", modifier = Modifier.weight(1f))
                StatCard("📖 Words", "${uiState.totalWordsLearned}", modifier = Modifier.weight(1f))
            }

            // Weak categories
            if (uiState.weakCategories.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🎯 Areas to Focus", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.weakCategories.forEach { cat ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFFFFECE5))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(cat, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFFF7052))
                                }
                            }
                        }
                    }
                }
            }

            // Encouragement message
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🦊", fontSize = 36.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = when {
                            uiState.weeklyAccuracy >= 0.9f -> "Amazing week! You're on fire! Keep up the great work!"
                            uiState.weeklyAccuracy >= 0.7f -> "Good progress this week! A few more practice sessions and you'll be unstoppable!"
                            uiState.weeklyAccuracy >= 0.5f -> "You're making progress! Remember, every mistake is a step toward learning."
                            else -> "Don't give up! Let's focus on those weak areas and turn them into strengths!"
                        },
                        fontSize = 14.sp,
                        color = Color(0xFF2C3E50),
                        lineHeight = 20.sp
                    )
                }
            }

            // Share button
            Button(
                onClick = { shareReport(context, viewModel) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD449),
                    contentColor = Color(0xFF2C3E50)
                )
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("📤 Share with Parents", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StatCard(emoji: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
        }
    }
}

private fun shareReport(context: Context, viewModel: WeeklyReportViewModel) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        val state = viewModel.uiState.value
        val message = buildString {
            appendLine("📊 Lingo English Weekly Report")
            appendLine()
            appendLine("Grade: ${viewModel.getGradeMessage(state.weeklyAccuracy)}")
            appendLine("Weekly Accuracy: ${(state.weeklyAccuracy * 100).toInt()}%")
            appendLine("Monthly Accuracy: ${(state.monthlyAccuracy * 100).toInt()}%")
            appendLine("🔥 Streak: ${state.streakDays} days")
            appendLine("📚 Sessions: ${state.totalSessions}")
            appendLine("📖 Words Learned: ${state.totalWordsLearned}")
            if (state.weakCategories.isNotEmpty()) {
                appendLine("🎯 Focus Areas: ${state.weakCategories.joinToString(", ")}")
            }
            appendLine()
            appendLine("Theme: ${state.themeName}")
            appendLine()
            appendLine("Keep practicing with Lingo English! 🦊")
        }
        putExtra(Intent.EXTRA_TEXT, message)
    }
    context.startActivity(Intent.createChooser(intent, "Share Report"))
}
