package org.akj.lingo.learn.ui.errorbook

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.akj.lingo.learn.domain.model.ErrorBookEntry
import org.akj.lingo.learn.ui.components.LingoAvatar
import org.akj.lingo.learn.ui.components.LingoExpression

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorBookDetailScreen(
    entry: ErrorBookEntry,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ErrorBookViewModel? = null,
    explanationOverride: String? = null
) {
    val activeViewModel = viewModel ?: if (explanationOverride == null) hiltViewModel() else null
    val liveExplanationState by (
        activeViewModel?.explanationState?.collectAsState()
            ?: remember { mutableStateOf(emptyMap<String, String>()) }
        )
    val explanation = explanationOverride ?: liveExplanationState[entry.vocabId] ?: "Thinking..."
    val currentGrade = LocalContext.current.getSharedPreferences("lingo_app_prefs", Context.MODE_PRIVATE)
        .getString("grade", "Grade 4") ?: "Grade 4"
    
    LaunchedEffect(activeViewModel, entry.vocabId) {
        activeViewModel?.fetchExplanation(entry.vocabId, entry.errorType, currentGrade)
    }
    var isFlipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(400),
        label = "flip_rotation"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF5))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Text("📖 Error Detail", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF2C3E50))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFFDF5))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 3D Flip Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clickable { isFlipped = !isFlipped }
                    .semantics { contentDescription = "Word detail card" },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        },
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    if (rotation <= 90f) {
                        // Front side
                        FlipCardFront(entry = entry)
                    } else {
                        // Back side - use scale to prevent mirroring
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationY = 180f }
                        ) {
                            FlipCardBack(
                                entry = entry,
                                explanation = explanation
                            )
                        }
                    }
                }
            }

            Text(
                "Tap card to flip",
                fontSize = 12.sp,
                color = Color(0xFF7F8C8D),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(24.dp))

            // Error history info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 Error History", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                    Spacer(Modifier.height(12.dp))
                    HistoryRow("Question Type", entry.questionType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
                    HistoryRow("Error Count", "${entry.errorCount} times")
                    HistoryRow("Consecutive Correct", "${entry.consecutiveCorrectCount} times")
                    HistoryRow("Status", when (entry.status) {
                        "TO_REVIEW" -> "Needs Review 🔴"
                        "CONSOLIDATED" -> "In Progress 🟡"
                        "GRADUATION_OBSERVATION" -> "Almost Mastered 🔵"
                        "GRADUATED" -> "Mastered 🟢"
                        else -> entry.status
                    })
                }
            }

            Spacer(Modifier.height(16.dp))

            // Practice suggestion
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = getPracticeTip(entry.errorType, entry.vocabId),
                        fontSize = 14.sp,
                        color = Color(0xFF2C3E50),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FlipCardFront(entry: ErrorBookEntry) {
    val daysAgo = ((System.currentTimeMillis() - entry.lastErrorTimestamp) / (24 * 3600 * 1000)).toInt()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = entry.vocabId,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Badge(
            containerColor = Color(0xFFFF7052).copy(alpha = 0.15f),
            contentColor = Color(0xFFFF7052)
        ) {
            Text(
                text = when (entry.errorType.uppercase()) {
                    "SPELLED_WRONG" -> "✍️ Spelling"
                    "LISTENING_WRONG" -> "👂 Listening"
                    "GRAMMAR_WRONG" -> "📝 Grammar"
                    "PRONUNCIATION_WRONG" -> "🗣️ Pronunciation"
                    // Sprint 20: types LearningViewModel actually writes today.
                    "QUIZ_WRONG_ANSWER" -> "🎯 Quiz"
                    "GAME_WRONG_ANSWER" -> "🎮 Game"
                    "PRODUCTION_WRONG" -> "✍️ Writing"
                    "SPEAKING_MISPRONOUNCED" -> "🗣️ Speaking"
                    else -> entry.errorType
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Error count: ${entry.errorCount}x",
            fontSize = 16.sp,
            color = Color(0xFF7F8C8D)
        )
        Text(
            if (daysAgo == 0) "Today" else "$daysAgo days ago",
            fontSize = 14.sp,
            color = Color(0xFF7F8C8D)
        )
        Spacer(Modifier.height(20.dp))
        if (entry.consecutiveCorrectCount > 0) {
            Text(
                "✅ ${entry.consecutiveCorrectCount} correct in a row!",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2ECC71)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("Tap for tips →", fontSize = 13.sp, color = Color(0xFFB0BEC5))
    }
}

@Composable
private fun FlipCardBack(entry: ErrorBookEntry, explanation: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LingoAvatar(expression = LingoExpression.HAPPY, modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                "Lingo's Tip for \"${entry.vocabId}\"",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5C6FF2)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = explanation,
            fontSize = 15.sp,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(24.dp))
        Text("Tap to go back →", fontSize = 13.sp, color = Color(0xFFB0BEC5))
    }
}

@Composable
private fun HistoryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color(0xFF7F8C8D))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF2C3E50))
    }
}

private fun getMnemonic(errorType: String, word: String): String = when (errorType.uppercase()) {
    "SPELLED_WRONG" -> "Try breaking \"$word\" into syllables. Say each part slowly: ${word.map { "$it-" }.joinToString("").removeSuffix("-")}"
    "LISTENING_WRONG" -> "Listen carefully to the sounds in \"$word\". Can you hear each letter? Try writing it down while listening."
    "GRAMMAR_WRONG" -> "\"$word\" follows a special grammar rule. Practice using it in a sentence: subject + $word + object."
    "PRONUNCIATION_WRONG" -> "For \"$word\", focus on mouth shape. Say it slowly: ${word.map { "$it " }.joinToString("").trim()}. Practice 3 times!"
    else -> "Keep practicing \"$word\". Try using it in a sentence every day!"
}

private fun getExampleSentence(word: String): String = when (word.lowercase()) {
    "apple" -> "I eat a red apple every morning."
    "school" -> "My school is very big and fun."
    "teacher" -> "My teacher helps me learn new things."
    "book" -> "I read a book before bed."
    "friend" -> "My friend and I play together."
    "hello" -> "Hello! How are you today?"
    else -> "Today I learned the word \"$word\"."
}

private fun getPracticeTip(errorType: String, word: String): String = when (errorType.uppercase()) {
    "SPELLED_WRONG" -> "Try writing \"$word\" 3 times. Then cover it and write from memory!"
    "LISTENING_WRONG" -> "Listen to audio of \"$word\" and try to write what you hear. Check if you got it right!"
    "GRAMMAR_WRONG" -> "Practice using \"$word\" in different tenses. Write a sentence for past, present, and future."
    "PRONUNCIATION_WRONG" -> "Record yourself saying \"$word\" and compare with the correct pronunciation. Practice until it matches!"
    else -> "Review \"$word\" in your daily practice session."
}
