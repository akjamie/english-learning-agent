package com.lingo.learn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StreakCounter(
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "FlameBreathing")
    // Flame breath animation scaling
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FlameScale"
    )

    // Milestone card theme styling for 7/30 days
    val isMilestone = streakDays > 0 && (streakDays % 7 == 0 || streakDays % 30 == 0)
    val containerColor = if (isMilestone) Color(0xFFFFECE5) else Color(0xFFF7F9FA)
    val contentColor = if (isMilestone) Color(0xFFFF5230) else Color(0xFF2C3E50)

    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Render breathing flame Emoji
        Text(
            text = "🔥",
            fontSize = 16.sp,
            modifier = Modifier.scale(scale)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$streakDays Days",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}
