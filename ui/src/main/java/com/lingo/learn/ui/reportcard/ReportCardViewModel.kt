package org.akj.lingo.learn.ui.reportcard

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import org.akj.lingo.learn.domain.repository.ErrorBookRepository
import org.akj.lingo.learn.domain.repository.LearningRecordRepository
import org.akj.lingo.learn.domain.repository.WeeklyPlanRepository
import org.akj.lingo.learn.domain.usecase.CefrMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class ReportCardUiState(
    val childName: String = "Buddy",
    val grade: String = "Grade 4",
    val cefrLabel: String? = null,
    val streakDays: Int = 0,
    val totalSessions: Int = 0,
    val weeklyAccuracy: Float = 0f,
    val totalWordsLearned: Int = 0,
    val masteredWords: List<String> = emptyList(),
    val weakWords: List<String> = emptyList(),
    val encouragement: String? = null,
    val isLoading: Boolean = false,
    val loadError: String? = null
)

@HiltViewModel
class ReportCardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val learningRecordRepository: LearningRecordRepository,
    private val errorBookRepository: ErrorBookRepository,
    private val weeklyPlanRepository: WeeklyPlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportCardUiState())
    val uiState: StateFlow<ReportCardUiState> = _uiState.asStateFlow()

    init { loadReportCard() }

    fun loadReportCard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, loadError = null)
            try {
                val prefs = context.getSharedPreferences("lingo_app_prefs", Context.MODE_PRIVATE)
                val childName = prefs.getString("child_name", "Buddy") ?: "Buddy"
                val grade = prefs.getString("grade", "Grade 4") ?: "Grade 4"
                val diagnosticLevel = prefs.getString("diagnostic_level", "B") ?: "B"
                val cefrLabel = CefrMapper.badge(CefrMapper.map(grade, diagnosticLevel))

                val streak = learningRecordRepository.getStreakDays()
                val weeklyRecords = learningRecordRepository.getWeeklyRecords()
                val weeklyAccuracy = if (weeklyRecords.isNotEmpty()) {
                    weeklyRecords.map { it.accuracy }.average().toFloat()
                } else 0f

                // Top error entries map to weak words; observation entries to mastered words.
                val topErrors = errorBookRepository.getTopPriorityErrors(10)
                val weakWords = topErrors.take(3).map { it.vocabId }
                val masteredWords = errorBookRepository.getErrorsInObservation().take(5).map { it.vocabId }

                _uiState.value = ReportCardUiState(
                    childName = childName,
                    grade = grade,
                    cefrLabel = cefrLabel,
                    streakDays = streak,
                    totalSessions = weeklyRecords.size,
                    weeklyAccuracy = weeklyAccuracy,
                    totalWordsLearned = (weeklyRecords.size * 3).coerceAtLeast(0),
                    masteredWords = masteredWords,
                    weakWords = weakWords,
                    encouragement = buildEncouragement(weeklyAccuracy),
                    isLoading = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, loadError = "Couldn't load the growth report. Tap retry.")
            }
        }
    }

    private fun buildEncouragement(accuracy: Float): String = when {
        accuracy >= 0.85f -> "Amazing growth! Lingo is so proud of you. Keep soaring! 🦊"
        accuracy >= 0.6f -> "Great progress! Every day you're getting stronger. Keep going! 🦊"
        else -> "Every step counts! Keep practicing and you'll shine. Lingo believes in you! 🦊"
    }

    /** Renders an A4-ratio (1240x1754) report card PNG and opens the Android share sheet. */
    fun shareReportCard() {
        val state = _uiState.value
        val uri = buildReportImageUri(context, state) ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_TEXT, "Lingo English growth report for ${state.childName}")
        }
        context.startActivity(Intent.createChooser(intent, "Share Growth Report"))
    }

    private fun buildReportImageUri(context: Context, state: ReportCardUiState): android.net.Uri? {
        return try {
            val width = 1240
            val height = 1754
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(0xFFFFFDF5.toInt())
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

            // Header
            val headerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF5C6FF2.toInt()
            }
            canvas.drawRect(0f, 0f, width.toFloat(), 280f, headerPaint)
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 60f
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            canvas.drawText("Lingo English — Growth Report", 70f, 140f, paint)
            paint.textSize = 36f
            paint.typeface = android.graphics.Typeface.DEFAULT
            canvas.drawText("${state.childName} · ${state.grade} · ${state.cefrLabel ?: ""}", 70f, 210f, paint)

            // Core stats
            var y = 380f
            paint.textSize = 42f
            paint.color = 0xFF2C3E50.toInt()
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            canvas.drawText("Core Stats", 70f, y, paint)
            y += 80f
            paint.textSize = 38f
            paint.typeface = android.graphics.Typeface.DEFAULT
            val stats = listOf(
                "🔥 Streak: ${state.streakDays} days",
                "📚 Sessions: ${state.totalSessions}",
                "📖 Words Learned: ${state.totalWordsLearned}",
                "🎯 Weekly Accuracy: ${(state.weeklyAccuracy * 100).toInt()}%"
            )
            stats.forEach { line ->
                canvas.drawText(line, 70f, y, paint)
                y += 70f
            }

            // Mastered words
            y += 40f
            paint.textSize = 42f
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            canvas.drawText("Top 5 Mastered Words", 70f, y, paint)
            y += 70f
            paint.textSize = 36f
            paint.typeface = android.graphics.Typeface.DEFAULT
            val mastered = if (state.masteredWords.isNotEmpty()) state.masteredWords.joinToString(" · ")
            else "Keep practicing to master your first words!"
            canvas.drawText(mastered, 70f, y, paint)
            y += 110f

            // Weak words
            paint.textSize = 42f
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            canvas.drawText("Top 3 Words to Practice", 70f, y, paint)
            y += 70f
            paint.textSize = 36f
            paint.typeface = android.graphics.Typeface.DEFAULT
            val weak = if (state.weakWords.isNotEmpty()) state.weakWords.joinToString(" · ")
            else "No words in the error book — awesome job!"
            canvas.drawText(weak, 70f, y, paint)

            // Encouragement
            paint.textSize = 40f
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            paint.color = 0xFF5C6FF2.toInt()
            canvas.drawText(state.encouragement ?: "", 70f, height - 200f, paint)

            val dir = File(context.cacheDir, "report_cards").apply { if (!exists()) mkdirs() }
            val file = File(dir, "growth_report_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { fos -> bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos) }
            androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            null
        }
    }
}
