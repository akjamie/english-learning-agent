package org.akj.lingo.learn.ui.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.akj.lingo.learn.ui.dashboard.GradeTheme

@Composable
fun PreTeachScreen(
    viewModel: LearningViewModel,
    theme: GradeTheme,
    onProceed: () -> Unit
) {
    val session by viewModel.session.collectAsState()
    val targetWords = session.targetNewWords

    var currentIndex by remember { mutableIntStateOf(0) }

    val emojiMap = mapOf(
        "apple" to "🍎", "banana" to "🍌", "cat" to "🐱", "dog" to "🐶",
        "school" to "🏫", "teacher" to "👩‍🏫", "book" to "📚", "pen" to "🖊️",
        "weather" to "☀️", "rain" to "🌧️", "shopping" to "🛒", "family" to "👨‍👩‍👧‍👦",
        "hello" to "👋", "goodbye" to "👋", "thank you" to "🙏", "sorry" to "🙇",
        "classroom" to "🏫", "student" to "🎒", "friend" to "🤝", "play" to "⚽"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (targetWords.isEmpty()) {
            // Fallback if no target words
            LaunchedEffect(Unit) { onProceed() }
            return
        }

        val currentWord = targetWords[currentIndex]

        Text(
            text = "Let's learn new words!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = theme.primaryColor,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Flashcard with animation
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                slideInHorizontally(animationSpec = tween(400)) { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally(animationSpec = tween(400)) { width -> -width } + fadeOut()
            },
            label = "FlashCard"
        ) { index ->
            val currentWord = targetWords[index]
            val emoji = emojiMap[currentWord.lowercase()] ?: "💡"
            
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .clickable {
                        viewModel.speakWord(currentWord)
                    }
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = emoji,
                        fontSize = 80.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = currentWord,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2C3E50)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tap to listen 🔊",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                if (currentIndex < targetWords.size - 1) {
                    currentIndex++
                } else {
                    onProceed()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp)
        ) {
            Text(
                text = if (currentIndex < targetWords.size - 1) "Next Word" else "Start Immersion",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = theme.buttonContentColor
            )
        }
    }
}
