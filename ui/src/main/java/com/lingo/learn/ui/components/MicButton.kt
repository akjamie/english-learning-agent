package org.akj.lingo.learn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * An ergonomic press-and-hold microphone button with glowing pulse animations.
 *
 * Designed for comfortable bottom-screen thumb reach per design spec 2.4.2.
 */
@Composable
fun MicButton(
    isRecording: Boolean,
    onPressDown: () -> Unit,
    onPressUp: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MicPulse")
    
    // Smooth pulse ring scale during recording
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRecording) 1.45f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "MicRingScale"
    )

    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = if (isRecording) 0.0f else 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "MicRingAlpha"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isRecording) 1.15f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ButtonScale"
    )

    val mainColor = if (isRecording) Color(0xFFFF5E5E) else Color(0xFFFFD449)
    val ringColor = if (isRecording) Color(0xFFFF5E5E).copy(alpha = ringAlpha) else Color.Transparent

    Column(
        modifier = modifier.padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(96.dp)
        ) {
            // Outer glowing animated pulse ring
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .scale(ringScale)
                        .clip(CircleShape)
                        .background(ringColor)
                )
            }

            // Main interactive mic button
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .scale(buttonScale)
                    .semantics { contentDescription = if (isRecording) "Stop recording" else "Start recording" }
                    .clip(CircleShape)
                    .background(mainColor)
                    .border(3.dp, if (isRecording) Color(0xFFFF8A8A) else Color(0xFFFFF0B3), CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onPressDown()
                                try {
                                    awaitRelease()
                                } finally {
                                    onPressUp()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRecording) "🎙️" else "🎤",
                    fontSize = 34.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label ?: if (isRecording) "Recording... Release when done!" else "Press & Hold to Speak 🎤",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isRecording) Color(0xFFFF5E5E) else Color(0xFF5C6FF2)
        )
    }
}
