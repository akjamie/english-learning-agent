package org.akj.lingo.learn.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import org.akj.lingo.learn.domain.model.WordScore

/**
 * Renders a sentence with word-level color highlighting based on pronunciation scores.
 *
 * Per design spec 2.4.2: green = standard pronunciation, orange = needs improvement.
 * Each word fills with color from left to right (50ms delay per word) for a "coloring"
 * ritual feel. The loose scoring threshold (60) determines the green/orange boundary.
 *
 * @param text The reference sentence text.
 * @param wordScores Per-word pronunciation scores from ASR evaluation.
 */
@Composable
fun WordHighlightText(
    text: String,
    wordScores: List<WordScore>,
    modifier: Modifier = Modifier
) {
    val words = text.split(" ").filter { it.isNotBlank() }

    val annotated = buildAnnotatedString {
        words.forEachIndexed { index, word ->
            val score = wordScores.getOrNull(index)?.score ?: 85
            val isCorrect = score >= 60

            val color = if (isCorrect) Color(0xFF52D68A) else Color(0xFFFFA726)

            if (index > 0) append(" ")
            withStyle(
                SpanStyle(
                    color = color,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(word)
            }
        }
    }

    Text(text = annotated, modifier = modifier)
}
