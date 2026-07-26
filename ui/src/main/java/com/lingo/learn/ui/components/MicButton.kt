package com.lingo.learn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A press-and-hold microphone button for voice recording.
 *
 * Per design spec 2.4.2: the child long-presses the mic to record, releases to
 * stop and trigger evaluation. The button scales up with a pulse animation while
 * recording to give strong visual feedback.
 *
 * @param isRecording Whether recording is currently active.
 * @param onPressDown Called when the user presses the button (start recording).
 * @param onPressUp Called when the user releases the button (stop + evaluate).
 */
@Composable
fun MicButton(
    isRecording: Boolean,
    onPressDown: () -> Unit,
    onPressUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MicPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRecording) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MicPulseScale"
    )

    val buttonColor = if (isRecording) Color(0xFFFF5E5E) else Color(0xFFFF7052)
    val backgroundColor = if (isRecording) Color(0xFFFFECE5) else Color(0xFFFFFDF5)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(backgroundColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onPressDown()
                            tryAwaitRelease()
                            onPressUp()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isRecording) "🎙️" else "🎤",
                fontSize = 36.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isRecording) "Recording... Speak now!" else "Hold to speak",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isRecording) Color(0xFFFF5E5E) else Color(0xFF7F8C8D)
        )
    }
}
