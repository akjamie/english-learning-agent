package org.akj.lingo.learn.ui.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.akj.lingo.learn.ui.components.AutoResizeText
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
    // ESA Engage: whether the child has matched the picture to the word yet.
    var engaged by remember { mutableStateOf(false) }

    val emojiMap = mapOf(
        "apple" to "🍎", "banana" to "🍌", "cat" to "🐱", "dog" to "🐶",
        "school" to "🏫", "teacher" to "👩‍🏫", "book" to "📚", "pen" to "🖊️",
        "weather" to "☀️", "rain" to "🌧️", "shopping" to "🛒", "family" to "👨‍👩‍👧‍👦",
        "hello" to "👋", "goodbye" to "👋", "thank you" to "🙏", "sorry" to "🙇",
        "classroom" to "🏫", "student" to "🎒", "friend" to "🤝", "play" to "⚽",
        "cake" to "🎂", "milk" to "🥛", "bread" to "🍞", "egg" to "🥚",
        "bird" to "🐦", "fish" to "🐟", "tree" to "🌳", "flower" to "🌸",
        "car" to "🚗", "bus" to "🚌", "house" to "🏠", "park" to "🌳"
    )

    // The Box + align(Center) + verticalScroll pattern keeps the content centered on
    // small/normal screens and scrollable when the flashcard + choices + button exceed
    // the viewport on high-density screens (where it previously clipped off-screen).
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        if (targetWords.isEmpty()) {
            // Fallback if no target words
            LaunchedEffect(Unit) { onProceed() }
            return
        }

        val currentWord = targetWords[currentIndex]

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                        if (engaged) {
                            // ESA: reveal the word only after the child matches it.
                            AutoResizeText(
                                text = currentWord,
                                minFontSize = 16.sp,
                                maxFontSize = 26.sp,
                                maxLines = 2,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = "❓",
                                fontSize = 40.sp,
                                color = Color(0xFFBDC3C7)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tap to listen 🔊",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // ESA Engage: picture-to-word matching choices (tap the correct word).
            if (!engaged) {
                Spacer(modifier = Modifier.height(20.dp))
                val choices = remember(currentWord) {
                    (targetWords.filter { it != currentWord }.take(2) + currentWord).shuffled()
                }
                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    choices.forEach { choice ->
                        val isCorrect = choice == currentWord
                        OutlinedButton(
                            onClick = {
                                if (isCorrect) {
                                    engaged = true
                                    viewModel.speakWord(currentWord)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            AutoResizeText(
                                text = choice,
                                minFontSize = 12.sp,
                                maxFontSize = 18.sp,
                                maxLines = 2,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (currentIndex < targetWords.size - 1) {
                        currentIndex++
                        engaged = false
                    } else {
                        onProceed()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                enabled = engaged
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
}
