package com.lingo.learn.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lingo.learn.ui.R
import com.lingo.learn.ui.components.LingoAvatar
import com.lingo.learn.ui.components.LingoExpression

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Soft vertical gradient background
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFDF5),
            Color(0xFFFFF9E6),
            Color(0xFFFFFFFF)
        )
    )

    // Lingo mascot initial wave and happy expression cycle
    var expression by remember { mutableStateOf(LingoExpression.HAPPY) }
    
    LaunchedEffect(Unit) {
        // Delay to showcase thinking then happy greetings
        kotlinx.coroutines.delay(1000)
        expression = LingoExpression.CELEBRATING
        kotlinx.coroutines.delay(1200)
        expression = LingoExpression.HAPPY
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBrush)
            .padding(24.dp)
            .statusBarsPadding()
    ) {
        // Top right gear button for Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            androidx.compose.material3.IconButton(onClick = onOpenSettings) {
                Text("⚙️", fontSize = 24.sp)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Lingo Companion Avatar
            LingoAvatar(
                expression = expression,
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // App Main Title
            Text(
                text = stringResource(R.string.welcome_title),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle descriptors
            Text(
                text = stringResource(R.string.welcome_subtitle),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF7F8C8D),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1.2f))

            // Flat Duolingo-style yellow action button
            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD449),
                    contentColor = Color(0xFF2C3E50)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text(
                    text = stringResource(R.string.get_started),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
