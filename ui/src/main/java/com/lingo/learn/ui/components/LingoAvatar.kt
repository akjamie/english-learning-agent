package org.akj.lingo.learn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.min
import kotlin.math.sin

enum class LingoExpression {
    HAPPY,
    CELEBRATING,
    SAD,
    THINKING,
    SLEEPY,
    EXCITED
}

@Composable
fun LingoAvatar(
    expression: LingoExpression,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(targetState = expression, label = "LingoExpression")

    val earTiltLeft by transition.animateFloat(label = "EarTiltLeft") { state ->
        when (state) {
            LingoExpression.SAD -> 25f
            LingoExpression.THINKING -> -5f
            else -> 0f
        }
    }
    val earTiltRight by transition.animateFloat(label = "EarTiltRight") { state ->
        when (state) {
            LingoExpression.SAD -> -25f
            LingoExpression.THINKING -> 15f
            else -> 0f
        }
    }

    val eyeHeightScale by transition.animateFloat(label = "EyeHeightScale") { state ->
        when (state) {
            LingoExpression.CELEBRATING, LingoExpression.SLEEPY -> 0f
            else -> 1.0f
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "LingoBreathing")
    val breatheOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Breathe"
    )

    val celebrateOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (expression == LingoExpression.CELEBRATING) -15f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = EaseOutBounce),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CelebrateJump"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val baseSize = 150f
            val s = min(canvasW, canvasH) / baseSize
            val cx = canvasW / 2
            val cy = canvasH / 2 + (breatheOffset + celebrateOffset) * s

            fun xoff(offset: Float) = cx + offset * s
            fun yoff(offset: Float) = cy + offset * s

            val orangeColor = Color(0xFFFF7052)
            val whiteColor = Color(0xFFFFFDF5)
            val darkColor = Color(0xFF2C3E50)
            val blushColor = Color(0xFFFFB3A7)
            val yellowAccent = Color(0xFFFFD449)

            val leftEarPath = Path().apply {
                moveTo(xoff(-50f), yoff(-40f))
                lineTo(xoff(-90f), yoff(-110f))
                lineTo(xoff(-10f), yoff(-70f))
                close()
            }
            drawPath(leftEarPath, orangeColor)
            val leftInnerEarPath = Path().apply {
                moveTo(xoff(-47f), yoff(-45f))
                lineTo(xoff(-78f), yoff(-95f))
                lineTo(xoff(-20f), yoff(-65f))
                close()
            }
            drawPath(leftInnerEarPath, blushColor)

            val rightEarPath = Path().apply {
                moveTo(xoff(50f), yoff(-40f))
                lineTo(xoff(90f), yoff(-110f))
                lineTo(xoff(10f), yoff(-70f))
                close()
            }
            drawPath(rightEarPath, orangeColor)
            val rightInnerEarPath = Path().apply {
                moveTo(xoff(47f), yoff(-45f))
                lineTo(xoff(78f), yoff(-95f))
                lineTo(xoff(20f), yoff(-65f))
                close()
            }
            drawPath(rightInnerEarPath, blushColor)

            drawOval(
                color = orangeColor,
                topLeft = Offset(xoff(-70f), yoff(-60f)),
                size = Size(140f * s, 120f * s)
            )

            val leftCheekPath = Path().apply {
                moveTo(xoff(-70f), yoff(0f))
                quadraticBezierTo(xoff(-50f), yoff(30f), xoff(-20f), yoff(50f))
                quadraticBezierTo(xoff(-50f), yoff(50f), xoff(-70f), yoff(20f))
                close()
            }
            drawPath(leftCheekPath, whiteColor)
            val rightCheekPath = Path().apply {
                moveTo(xoff(70f), yoff(0f))
                quadraticBezierTo(xoff(50f), yoff(30f), xoff(20f), yoff(50f))
                quadraticBezierTo(xoff(50f), yoff(50f), xoff(70f), yoff(20f))
                close()
            }
            drawPath(rightCheekPath, whiteColor)

            drawCircle(
                color = blushColor.copy(alpha = 0.8f),
                radius = 12f * s,
                center = Offset(xoff(-40f), yoff(15f))
            )
            drawCircle(
                color = blushColor.copy(alpha = 0.8f),
                radius = 12f * s,
                center = Offset(xoff(40f), yoff(15f))
            )

            val eyeY = cy - 10f * s
            val eyeWidth = 14f * s
            val eyeHeight = 22f * s

            if (expression == LingoExpression.CELEBRATING) {
                val lep = Path().apply {
                    moveTo(xoff(-40f), eyeY + 5f * s)
                    quadraticBezierTo(xoff(-30f), eyeY - 8f * s, xoff(-20f), eyeY + 5f * s)
                }
                drawPath(lep, darkColor, style = Stroke(width = 5f * s))
                val rep = Path().apply {
                    moveTo(xoff(20f), eyeY + 5f * s)
                    quadraticBezierTo(xoff(30f), eyeY - 8f * s, xoff(40f), eyeY + 5f * s)
                }
                drawPath(rep, darkColor, style = Stroke(width = 5f * s))
            } else if (expression == LingoExpression.SLEEPY) {
                drawLine(darkColor, Offset(xoff(-42f), eyeY), Offset(xoff(-22f), eyeY), strokeWidth = 6f * s)
                drawLine(darkColor, Offset(xoff(22f), eyeY), Offset(xoff(42f), eyeY), strokeWidth = 6f * s)
            } else if (expression == LingoExpression.THINKING) {
                drawOval(
                    color = darkColor,
                    topLeft = Offset(xoff(-38f) - eyeWidth / 2, eyeY - eyeHeight / 2),
                    size = Size(eyeWidth, eyeHeight)
                )
                val rep = Path().apply {
                    moveTo(xoff(20f), eyeY + 3f * s)
                    quadraticBezierTo(xoff(30f), eyeY - 3f * s, xoff(40f), eyeY + 3f * s)
                }
                drawPath(rep, darkColor, style = Stroke(width = 5f * s))
            } else {
                drawOval(
                    color = darkColor,
                    topLeft = Offset(xoff(-38f) - eyeWidth / 2, eyeY - (eyeHeight * eyeHeightScale) / 2),
                    size = Size(eyeWidth, eyeHeight * eyeHeightScale)
                )
                drawOval(
                    color = darkColor,
                    topLeft = Offset(xoff(38f) - eyeWidth / 2, eyeY - (eyeHeight * eyeHeightScale) / 2),
                    size = Size(eyeWidth, eyeHeight * eyeHeightScale)
                )
                if (eyeHeightScale > 0.5f) {
                    drawCircle(Color.White, 4f * s, Offset(xoff(-36f), eyeY - 4f * s))
                    drawCircle(Color.White, 4f * s, Offset(xoff(40f), eyeY - 4f * s))
                }
            }

            val noseY = cy + 10f * s
            val nosePath = Path().apply {
                moveTo(xoff(-8f), noseY)
                lineTo(xoff(8f), noseY)
                lineTo(xoff(0f), noseY + 6f * s)
                close()
            }
            drawPath(nosePath, darkColor)

            if (expression == LingoExpression.SAD) {
                val smp = Path().apply {
                    moveTo(xoff(-10f), noseY + 16f * s)
                    quadraticBezierTo(xoff(0f), noseY + 8f * s, xoff(10f), noseY + 16f * s)
                }
                drawPath(smp, darkColor, style = Stroke(width = 4f * s))
            } else if (expression == LingoExpression.HAPPY || expression == LingoExpression.CELEBRATING || expression == LingoExpression.EXCITED) {
                val mp = Path().apply {
                    moveTo(xoff(-12f), noseY + 10f * s)
                    quadraticBezierTo(xoff(-6f), noseY + 18f * s, xoff(0f), noseY + 10f * s)
                    quadraticBezierTo(xoff(6f), noseY + 18f * s, xoff(12f), noseY + 10f * s)
                }
                drawPath(mp, darkColor, style = Stroke(width = 4.5f * s))
            } else {
                drawLine(darkColor, Offset(xoff(-6f), noseY + 12f * s), Offset(xoff(6f), noseY + 12f * s), strokeWidth = 4f * s)
            }

            if (expression == LingoExpression.EXCITED) {
                drawCircle(yellowAccent, 6f * s, Offset(xoff(-85f), yoff(-60f)))
                drawCircle(yellowAccent, 4f * s, Offset(xoff(-95f), yoff(-30f)))
                drawCircle(yellowAccent, 6f * s, Offset(xoff(85f), yoff(-60f)))
                drawCircle(yellowAccent, 4f * s, Offset(xoff(95f), yoff(-30f)))
            }
        }
    }
}
