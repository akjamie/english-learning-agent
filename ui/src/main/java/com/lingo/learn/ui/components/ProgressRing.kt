package com.lingo.learn.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFFFFD449),
    inactiveColor: Color = Color(0xFFECEFF1),
    strokeWidth: Float = 16f
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "RingProgress"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val radius = (width.coerceAtMost(height) - strokeWidth) / 2

            // 1. Draw inactive background ring
            drawCircle(
                color = inactiveColor,
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            // 2. Draw active progress arc starting from -90 degrees (top center)
            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}
