package org.akj.lingo.learn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A horizontal row of progress dots representing quiz progression.
 *
 * Per design spec 2.4.2: "Screen top shows Quiz progress dots (●●●○○), clearly
 * showing how many questions remain." Filled dots = answered, hollow = pending.
 *
 * @param total Total number of quiz questions.
 * @param current The index of the current question (0-based).
 * @param activeColor Color for answered/current dots.
 */
@Composable
fun QuizProgressBar(
    total: Int,
    current: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFFFFD449),
    inactiveColor: Color = Color(0xFFECEFF1)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until total) {
            val isAnswered = i < current
            val isCurrent = i == current

            val dotColor by animateColorAsState(
                targetValue = when {
                    isAnswered -> activeColor
                    isCurrent -> activeColor.copy(alpha = 0.7f)
                    else -> inactiveColor
                },
                animationSpec = tween(300),
                label = "DotColor_$i"
            )

            val dotSize = if (isCurrent) 12.dp else 10.dp

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}
