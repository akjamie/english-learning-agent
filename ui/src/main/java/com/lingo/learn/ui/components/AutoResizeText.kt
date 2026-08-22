package org.akj.lingo.learn.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Text that automatically shrinks its font size (bounded by [minFontSize] and
 * [maxFontSize]) until the content fits the available width/height without
 * clipping or wrapping beyond [maxLines]. Used across the learning flow so
 * words and sentences stay fully visible on high-DPI screens like the Mate 80.
 *
 * Fitting is measured once per layout pass: the text is laid out at the maximum
 * size and stepped down 1sp at a time until it no longer overflows.
 */
@Composable
fun AutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 16.sp,
    maxFontSize: TextUnit = 26.sp,
    maxLines: Int = 2,
    softWrap: Boolean = true,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Center,
    lineHeight: TextUnit = TextUnit.Unspecified
) {
    BoxWithConstraints(modifier = modifier) {
        val fontSize = rememberFitTextSize(
            text = text,
            minFontSize = minFontSize,
            maxFontSize = maxFontSize,
            maxLines = maxLines,
            fontWeight = fontWeight,
            lineHeight = lineHeight
        )
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            fontSize = fontSize.sp,
            fontWeight = fontWeight,
            color = color,
            textAlign = textAlign,
            lineHeight = lineHeight,
            maxLines = maxLines,
            softWrap = softWrap,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Measures [text] against the current BoxWithConstraints bounds and returns the
 * largest font size (in sp, between [minFontSize] and [maxFontSize]) whose
 * layout fits the available width/height and stays within [maxLines] lines.
 * Shared by AutoResizeText and WordHighlightText so both scale identically.
 */
@Composable
internal fun BoxWithConstraintsScope.rememberFitTextSize(
    text: String,
    minFontSize: TextUnit = 16.sp,
    maxFontSize: TextUnit = 26.sp,
    maxLines: Int = Int.MAX_VALUE,
    fontWeight: FontWeight = FontWeight.Normal,
    lineHeight: TextUnit = TextUnit.Unspecified
): Float {
    val textMeasurer = rememberTextMeasurer()
    val maxWidthPx = constraints.maxWidth
    val maxHeightPx = constraints.maxHeight
    return remember(text, maxWidthPx, maxHeightPx, maxLines, minFontSize, maxFontSize, fontWeight, lineHeight) {
        // A single word (no whitespace) must fit on ONE line — a lineCount > 1
        // means the word was broken mid-word ("celebrat\ne"), so keep shrinking.
        val singleWord = text.none { it.isWhitespace() }
        var size = maxFontSize.value
        while (size > minFontSize.value) {
            val layout = textMeasurer.measure(
                text = text,
                style = TextStyle(fontSize = size.sp, fontWeight = fontWeight, lineHeight = lineHeight),
                constraints = Constraints(maxWidth = maxWidthPx),
                overflow = TextOverflow.Visible
            )
            val fitsHeight = maxHeightPx == Constraints.Infinity || layout.size.height <= maxHeightPx
            val fitsLines = if (singleWord) layout.lineCount <= 1 else layout.lineCount <= maxLines
            if (!layout.hasVisualOverflow && fitsHeight && fitsLines) break
            size -= 1f
        }
        size.coerceAtLeast(10f)  // absolute floor: 10sp prevents text vanishing entirely
    }
}
