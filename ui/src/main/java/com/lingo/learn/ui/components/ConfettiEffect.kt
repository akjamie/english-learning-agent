package org.akj.lingo.learn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class Particle(
    val id: Int,
    var x: Float,
    var y: Float,
    val color: Color,
    val speedX: Float,
    val speedY: Float,
    val size: Float
)

@Composable
fun ConfettiEffect(modifier: Modifier = Modifier, isVisible: Boolean = true) {
    if (!isVisible) return

    val particles = remember {
        val colors = listOf(Color(0xFF52D68A), Color(0xFFFF7052), Color(0xFFFFD449), Color(0xFF5C6FF2))
        List(100) { id ->
            Particle(
                id = id,
                x = Random.nextFloat() * 1000f, // Initial X
                y = -50f, // Initial Y (above screen)
                color = colors.random(),
                speedX = Random.nextFloat() * 10f - 5f,
                speedY = Random.nextFloat() * 15f + 5f,
                size = Random.nextFloat() * 15f + 10f
            )
        }
    }

    val transitionState = rememberInfiniteTransition(label = "confetti")
    val time by transitionState.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { p ->
            // Use time to drive animation
            val currentY = (p.y + (time * 1000f * (p.speedY / 10f))) % height
            val currentX = (p.x + (time * 500f * (p.speedX / 5f))) % width

            drawCircle(
                color = p.color,
                radius = p.size,
                center = Offset(currentX, currentY)
            )
        }
    }
}
