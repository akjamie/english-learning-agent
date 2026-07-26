package com.lingo.learn.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lingo.learn.ui.components.LingoAvatar
import com.lingo.learn.ui.components.LingoExpression

@Composable
fun DiagnosisResultScreen(
    level: String, // "A", "B", "C"
    onStartLearning: () -> Unit,
    modifier: Modifier = Modifier
) {
    val levelTitle = when (level) {
        "A" -> "Beginner Level (初级段)"
        "B" -> "Intermediate Level (中级段)"
        else -> "Advanced Level (高级段)"
    }

    val levelDesc = when (level) {
        "A" -> "You've mastered basic phonics and high frequency words. Lingo recommends starting with thematic scenes like Family and Animals!"
        "B" -> "You have a solid vocabulary base. We'll focus on conversation applications, reading comprehensions, and systematic syntax!"
        else -> "Excellent! You have a great grasp of complex sentences. We will tackle advanced reading, essay writing, and voice coaching directly!"
    }

    val dailyTaskTheme = when (level) {
        "A" -> "School Life"
        "B" -> "Shopping & Food"
        else -> "Environmental Science"
    }

    val dailyTaskDuration = when (level) {
        "A" -> "15 Mins"
        "B" -> "20 Mins"
        else -> "25 Mins"
    }

    val dailyTaskVocabs = when (level) {
        "A" -> "5 Words + 2 Sentences"
        "B" -> "8 Words + 3 Sentences"
        else -> "12 Words + 1 Topic Discuss"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF5))
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // 1. Avatar and greetings header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LingoAvatar(
                expression = LingoExpression.CELEBRATING,
                modifier = Modifier.size(130.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Evaluation Done! 🎉",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Level tag display
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFECE5))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = levelTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF7052)
                )
            }
        }

        // 2. Customized diagnostic analysis text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .border(1.5.dp, Color(0xFFECEFF1), RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Text(
                text = levelDesc,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Center
            )
        }

        // 3. Today's task preview card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6)),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "👇 Today's Task",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7F8C8D)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = dailyTaskTheme,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Divider(color = Color(0xFFECEFF1), thickness = 1.dp)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Est. Duration", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(dailyTaskDuration, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Target Goal", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(dailyTaskVocabs, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                    }
                }
            }
        }

        // 4. Main action button
        Button(
            onClick = onStartLearning,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD449),
                contentColor = Color(0xFF2C3E50)
            )
        ) {
            Text(
                text = "Let's Go! 🚀",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
