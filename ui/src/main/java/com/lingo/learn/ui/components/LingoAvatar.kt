package com.lingo.learn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin

enum class LingoExpression {
    HAPPY,          // Default smiling face
    CELEBRATING,    // Celebratory bounce when answer is correct
    SAD,            // Sad expression when answer is wrong
    THINKING,       // Thinking state for loading or diagnostic analysis
    SLEEPY,         // Sleepy expression for bedtime reminder
    EXCITED         // Combo streak visual sparks
}

@Composable
fun LingoAvatar(
    expression: LingoExpression,
    modifier: Modifier = Modifier
) {
    // Animation transition base parameters
    val transition = updateTransition(targetState = expression, label = "LingoExpression")

    // Ear tilting angles
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

    // Eye height ratio (1.0 = round eyes, 0.0 = squinting/closed)
    val eyeHeightScale by transition.animateFloat(label = "EyeHeightScale") { state ->
        when (state) {
            LingoExpression.CELEBRATING, LingoExpression.SLEEPY -> 0f
            else -> 1.0f
        }
    }

    // Breathing offset animations
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

    // Celebrating bounce animations
    val celebrateOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (expression == LingoExpression.CELEBRATING) -15f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = EaseOutBounce),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CelebrateJump"
    )

    Box(modifier = modifier.size(150.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2
            val centerY = height / 2 + breatheOffset + celebrateOffset

            // Fox primary colors
            val orangeColor = Color(0xFFFF7052)
            val whiteColor = Color(0xFFFFFDF5)
            val darkColor = Color(0xFF2C3E50)
            val blushColor = Color(0xFFFFB3A7)
            val yellowAccent = Color(0xFFFFD449)

            // 1. Draw ears
            // Left ear
            val leftEarPath = Path().apply {
                moveTo(centerX - 50f, centerY - 40f)
                lineTo(centerX - 90f, centerY - 110f)
                lineTo(centerX - 10f, centerY - 70f)
                close()
            }
            drawPath(leftEarPath, orangeColor)
            // Left inner ear
            val leftInnerEarPath = Path().apply {
                moveTo(centerX - 47f, centerY - 45f)
                lineTo(centerX - 78f, centerY - 95f)
                lineTo(centerX - 20f, centerY - 65f)
                close()
            }
            drawPath(leftInnerEarPath, blushColor)

            // Right ear
            val rightEarPath = Path().apply {
                moveTo(centerX + 50f, centerY - 40f)
                lineTo(centerX + 90f, centerY - 110f)
                lineTo(centerX + 10f, centerY - 70f)
                close()
            }
            drawPath(rightEarPath, orangeColor)
            // Right inner ear
            val rightInnerEarPath = Path().apply {
                moveTo(centerX + 47f, centerY - 45f)
                lineTo(centerX + 78f, centerY - 95f)
                lineTo(centerX + 20f, centerY - 65f)
                close()
            }
            drawPath(rightInnerEarPath, blushColor)

            // 2. Draw face shape
            drawOval(
                color = orangeColor,
                topLeft = Offset(centerX - 70f, centerY - 60f),
                size = Size(140f, 120f)
            )

            // 3. White cheek accents (Duolingo flat design)
            val leftCheekPath = Path().apply {
                moveTo(centerX - 70f, centerY)
                quadraticBezierTo(centerX - 50f, centerY + 30f, centerX - 20f, centerY + 50f)
                quadraticBezierTo(centerX - 50f, centerY + 50f, centerX - 70f, centerY + 20f)
                close()
            }
            drawPath(leftCheekPath, whiteColor)

            val rightCheekPath = Path().apply {
                moveTo(centerX + 70f, centerY)
                quadraticBezierTo(centerX + 50f, centerY + 30f, centerX + 20f, centerY + 50f)
                quadraticBezierTo(centerX + 50f, centerY + 50f, centerX + 70f, centerY + 20f)
                close()
            }
            drawPath(rightCheekPath, whiteColor)

            // 4. Blush circles
            drawCircle(
                color = blushColor.copy(alpha = 0.8f),
                radius = 12f,
                center = Offset(centerX - 40f, centerY + 15f)
            )
            drawCircle(
                color = blushColor.copy(alpha = 0.8f),
                radius = 12f,
                center = Offset(centerX + 40f, centerY + 15f)
            )

            // 5. Draw eyes
            val eyeY = centerY - 10f
            val eyeWidth = 14f
            val eyeHeight = 22f

            if (expression == LingoExpression.CELEBRATING) {
                // Closed happy eyes (^ ^)
                val leftEyePath = Path().apply {
                    moveTo(centerX - 40f, eyeY + 5f)
                    quadraticBezierTo(centerX - 30f, eyeY - 8f, centerX - 20f, eyeY + 5f)
                }
                drawPath(leftEyePath, darkColor, style = Stroke(width = 5f))

                val rightEyePath = Path().apply {
                    moveTo(centerX + 20f, eyeY + 5f)
                    quadraticBezierTo(centerX + 30f, eyeY - 8f, centerX + 40f, eyeY + 5f)
                }
                drawPath(rightEyePath, darkColor, style = Stroke(width = 5f))
            } else if (expression == LingoExpression.SLEEPY) {
                // Sleepy eyes (- -)
                drawLine(
                    color = darkColor,
                    start = Offset(centerX - 42f, eyeY),
                    end = Offset(centerX - 22f, eyeY),
                    strokeWidth = 6f
                )
                drawLine(
                    color = darkColor,
                    start = Offset(centerX + 22f, eyeY),
                    end = Offset(centerX + 42f, eyeY),
                    strokeWidth = 6f
                )
            } else if (expression == LingoExpression.THINKING) {
                // Thinking eyes (one squinting, one open)
                drawOval(
                    color = darkColor,
                    topLeft = Offset(centerX - 38f - eyeWidth / 2, eyeY - eyeHeight / 2),
                    size = Size(eyeWidth, eyeHeight)
                )
                val rightEyePath = Path().apply {
                    moveTo(centerX + 20f, eyeY + 3f)
                    quadraticBezierTo(centerX + 30f, eyeY - 3f, centerX + 40f, eyeY + 3f)
                }
                drawPath(rightEyePath, darkColor, style = Stroke(width = 5f))
            } else {
                // Open eyes
                drawOval(
                    color = darkColor,
                    topLeft = Offset(centerX - 38f - eyeWidth / 2, eyeY - (eyeHeight * eyeHeightScale) / 2),
                    size = Size(eyeWidth, eyeHeight * eyeHeightScale)
                )
                drawOval(
                    color = darkColor,
                    topLeft = Offset(centerX + 38f - eyeWidth / 2, eyeY - (eyeHeight * eyeHeightScale) / 2),
                    size = Size(eyeWidth, eyeHeight * eyeHeightScale)
                )
                if (eyeHeightScale > 0.5f) {
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(centerX - 36f, eyeY - 4f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(centerX + 40f, eyeY - 4f)
                    )
                }
            }

            // 6. Nose and mouth
            val noseY = centerY + 10f
            val nosePath = Path().apply {
                moveTo(centerX - 8f, noseY)
                lineTo(centerX + 8f, noseY)
                lineTo(centerX, noseY + 6f)
                close()
            }
            drawPath(nosePath, darkColor)

            if (expression == LingoExpression.SAD) {
                // Sad mouth
                val sadMouthPath = Path().apply {
                    moveTo(centerX - 10f, noseY + 16f)
                    quadraticBezierTo(centerX, noseY + 8f, centerX + 10f, noseY + 16f)
                }
                drawPath(sadMouthPath, darkColor, style = Stroke(width = 4f))
            } else if (expression == LingoExpression.HAPPY || expression == LingoExpression.CELEBRATING || expression == LingoExpression.EXCITED) {
                // Smiling open mouth
                val mouthPath = Path().apply {
                    moveTo(centerX - 12f, noseY + 10f)
                    quadraticBezierTo(centerX - 6f, noseY + 18f, centerX, noseY + 10f)
                    quadraticBezierTo(centerX + 6f, noseY + 18f, centerX + 12f, noseY + 10f)
                }
                drawPath(mouthPath, darkColor, style = Stroke(width = 4.5f))
            } else {
                // Simple straight line mouth
                drawLine(
                    color = darkColor,
                    start = Offset(centerX - 6f, noseY + 12f),
                    end = Offset(centerX + 6f, noseY + 12f),
                    strokeWidth = 4f
                )
            }

            // 7. Excited combo spark effects (EXCITED state only)
            if (expression == LingoExpression.EXCITED) {
                val sparkColor = yellowAccent
                drawCircle(sparkColor, 6f, Offset(centerX - 85f, centerY - 60f))
                drawCircle(sparkColor, 4f, Offset(centerX - 95f, centerY - 30f))
                drawCircle(sparkColor, 6f, Offset(centerX + 85f, centerY - 60f))
                drawCircle(sparkColor, 4f, Offset(centerX + 95f, centerY - 30f))
            }
        }
    }
}
