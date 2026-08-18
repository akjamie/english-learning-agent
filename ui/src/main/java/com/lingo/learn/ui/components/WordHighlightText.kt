package org.akj.lingo.learn.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
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
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 16.sp,
    maxFontSize: TextUnit = 22.sp,
    maxLines: Int = 4
) {
    val words = text.split(" ").filter { it.isNotBlank() }

    BoxWithConstraints(modifier = modifier) {
        val fontSize = rememberFitTextSize(
            text = text,
            minFontSize = minFontSize,
            maxFontSize = maxFontSize,
            maxLines = maxLines,
            fontWeight = FontWeight.Bold
        )

        val annotated = remember(text, wordScores, fontSize) {
            buildAnnotatedString {
                words.forEachIndexed { index, word ->
                    val score = wordScores.getOrNull(index)?.score ?: 85
                    val isCorrect = score >= 60

                    val color = if (isCorrect) Color(0xFF52D68A) else Color(0xFFFFA726)

                    if (index > 0) append(" ")
                    withStyle(
                        SpanStyle(
                            color = color,
                            fontSize = fontSize.sp,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(word)
                    }
                }
            }
        }

        Text(
            text = annotated,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            softWrap = true,
            maxLines = maxLines,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            lineHeight = (fontSize + 8f).sp
        )

    }
}
