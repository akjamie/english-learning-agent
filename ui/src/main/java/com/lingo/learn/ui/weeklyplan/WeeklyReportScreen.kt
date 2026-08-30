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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import org.akj.lingo.learn.ui.R
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
                    Text(stringResource(R.string.weekly_report_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
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

        if (uiState.loadError != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🦊", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        uiState.loadError!!,
                        fontSize = 14.sp,
                        color = Color(0xFF7F8C8D),
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadReport() }) {
                        Text("Retry")
                    }
                }
            }
        } else if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF5C6FF2))
            }
        } else {

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
                StatCard("🔥", "Streak", "${uiState.streakDays} days", modifier = Modifier.weight(1f))
                StatCard("📚", "Sessions", "${uiState.totalSessions}", modifier = Modifier.weight(1f))
                StatCard("📖", "Words", "${uiState.totalWordsLearned}", modifier = Modifier.weight(1f))
            }

            // Sprint 19: estimated CEFR level
            if (!uiState.cefrLabel.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF5C6FF2).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "🌍 Estimated CEFR Level: ${uiState.cefrLabel}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C6FF2)
                    )
                }
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

            // Words to review (Sprint 7 - parent detail report)
            if (uiState.topErrorWords.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🔤 Words To Review", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.topErrorWords.forEach { word ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFFEAF4FF))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(word, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF2E86DE))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "These words from the Error Book are scheduled for spaced review.",
                            fontSize = 12.sp,
                            color = Color(0xFF7F8C8D)
                        )
                    }
                }
            }

            // Time distribution (Sprint 7 - parent detail report)
            if (uiState.timeByTaskType.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("⏱️ Time Spent This Week", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                        Spacer(Modifier.height(12.dp))
                        val totalSec = uiState.timeByTaskType.values.sum().coerceAtLeast(1)
                        uiState.timeByTaskType.forEach { (type, sec) ->
                            val fraction = sec.toFloat() / totalSec
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(type, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF2C3E50), modifier = Modifier.width(90.dp))
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFF5C6FF2),
                                    trackColor = Color(0xFFEDEFF7)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("${sec / 60}m", fontSize = 12.sp, color = Color(0xFF7F8C8D))
                            }
                        }
                    }
                }
            }

            // "What Lingo adjusted this week" section (Sprint 6 - AI Presence)
            if (uiState.agentAdjustments.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF3E7)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🦊 What Lingo adjusted this week", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                        Spacer(Modifier.height(12.dp))
                        uiState.agentAdjustments.forEach { adjustment ->
                            Text(
                                adjustment,
                                fontSize = 14.sp,
                                color = Color(0xFF2C3E50),
                                lineHeight = 20.sp
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(
                            "Based on decisions Lingo logged while teaching you this week.",
                            fontSize = 12.sp,
                            color = Color(0xFF7F8C8D)
                        )
                    }
                }
            }

            // Sprint 12: "Lingo's letter" weekly parent digest
            uiState.lingoLetter?.let { letter ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F8F0)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🦊", fontSize = 28.sp)
                            Spacer(Modifier.width(10.dp))
                            Text("Lingo's Letter to You", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            letter,
                            fontSize = 14.sp,
                            color = Color(0xFF2C3E50),
                            lineHeight = 20.sp
                        )
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
}

@Composable
private fun StatCard(emoji: String, label: String, value: String, modifier: Modifier = Modifier) {
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
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF7F8C8D),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun shareReport(context: Context, viewModel: WeeklyReportViewModel) {
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
        if (state.topErrorWords.isNotEmpty()) {
            appendLine("🔤 Words To Review: ${state.topErrorWords.joinToString(", ")}")
        }
        if (state.timeByTaskType.isNotEmpty()) {
            val totalMin = state.timeByTaskType.values.sum() / 60
            appendLine("⏱️ Study Time: ~$totalMin min this week")
            state.timeByTaskType.forEach { (type, sec) ->
                appendLine("   - $type: ${sec / 60} min")
            }
        }
        appendLine()
        appendLine("Theme: ${state.themeName}")
        appendLine()
        appendLine("Keep practicing with Lingo English! 🦊")
    }

    // Sprint 12: also share an image card for a richer parent experience.
    val imageUri = buildReportImageUri(context, state, viewModel)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = if (imageUri != null) "image/*" else "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
        if (imageUri != null) {
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivity(Intent.createChooser(intent, "Share Report"))
}

/** Renders a compact report summary card as a PNG and returns its FileProvider URI. */
private fun buildReportImageUri(
    context: Context,
    state: WeeklyReportUiState,
    viewModel: WeeklyReportViewModel
): android.net.Uri? {
    return try {
        val width = 1080
        val height = 1350
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Background
        canvas.drawColor(0xFFFFFDF5.toInt())
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        // Header bar
        val headerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF5C6FF2.toInt()
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 240f, headerPaint)

        paint.color = android.graphics.Color.WHITE
        paint.textSize = 56f
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText("Lingo English — Weekly Report", 60f, 130f, paint)
        paint.textSize = 34f
        paint.typeface = android.graphics.Typeface.DEFAULT
        canvas.drawText("Theme: ${state.themeName}", 60f, 190f, paint)

        // Stats
        var y = 320f
        paint.textSize = 40f
        paint.color = 0xFF2C3E50.toInt()
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText("${viewModel.getGradeMessage(state.weeklyAccuracy)}", 60f, y, paint)
        y += 90f
        paint.textSize = 36f
        paint.typeface = android.graphics.Typeface.DEFAULT
        val lines = listOf(
            "Weekly Accuracy: ${(state.weeklyAccuracy * 100).toInt()}%",
            "Monthly Accuracy: ${(state.monthlyAccuracy * 100).toInt()}%",
            "🔥 Streak: ${state.streakDays} days",
            "📚 Sessions: ${state.totalSessions}",
            "📖 Words Learned: ${state.totalWordsLearned}",
            if (state.weakCategories.isNotEmpty()) "🎯 Focus Areas: ${state.weakCategories.joinToString(", ")}" else "",
            if (state.topErrorWords.isNotEmpty()) "🔤 Words To Review: ${state.topErrorWords.joinToString(", ")}" else ""
        )
        lines.filter { it.isNotBlank() }.forEach { line ->
            canvas.drawText(line, 60f, y, paint)
            y += 66f
        }

        // Footer
        paint.textSize = 32f
        paint.color = 0xFF7F8C8D.toInt()
        canvas.drawText("Keep practicing with Lingo English! 🦊", 60f, height - 90f, paint)

        // Write to cache
        val dir = File(context.cacheDir, "shared_reports").apply { if (!exists()) mkdirs() }
        val file = File(dir, "weekly_report.png")
        FileOutputStream(file).use { fos -> bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos) }
        bitmap.recycle()

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (_: Exception) {
        null
    }
}
