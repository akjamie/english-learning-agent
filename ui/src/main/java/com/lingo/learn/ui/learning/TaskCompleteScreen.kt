package org.akj.lingo.learn.ui.learning

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.content.Intent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.akj.lingo.learn.ui.components.ConfettiEffect
import org.akj.lingo.learn.ui.components.LingoAvatar
import org.akj.lingo.learn.ui.components.LingoExpression
import org.akj.lingo.learn.ui.components.ProgressRing
import org.akj.lingo.learn.ui.components.StreakCounter
import org.akj.lingo.learn.ui.dashboard.GradeTheme

/**
 * Task Completion Page.
 *
 * Per design spec 2.4.2:
 * - Large text showing today's achievements ("You learned [N] new words!")
 * - Streak flame animation with Lingo standing beside it
 * - Weekly progress arc (today is day X, X/7)
 * - Next day task preview (subtle)
 * - "Share to parent" lightweight text link
 */
@Composable
fun TaskCompleteScreen(
    viewModel: LearningViewModel,
    theme: GradeTheme,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summary.collectAsState()
    // Sprint 10: visible growth — collect persisted XP + daily goals state
    val dailyGoals by viewModel.dailyGoals.collectAsState()
    val levelInfo by viewModel.levelInfo.collectAsState()
    val totalXp by viewModel.totalXp.collectAsState()

    // Animate the new words count from 0 to final
    val animatedWords by animateIntAsState(
        targetValue = summary?.newWordsLearned ?: 0,
        animationSpec = tween(1000, easing = EaseOutBounce),
        label = "WordCount"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.surfaceColor)
                .padding(horizontal = 24.dp)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

        // 1. Lingo celebrating
        LingoAvatar(expression = LingoExpression.CELEBRATING, modifier = Modifier.size(90.dp))

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Today's achievement headline
        Text(
            text = "You learned",
            fontSize = 16.sp,
            color = Color(0xFF7F8C8D)
        )
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "$animatedWords",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = theme.primaryColor
            )
            Text(
                text = " new words!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Achievement stats cards (2x2 grid)
        summary?.let { s ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Quiz Score",
                    value = "${s.quizScore}/${s.quizTotal}",
                    icon = "📝",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Pronunciation",
                    value = s.pronunciationScore?.toString() ?: "--",
                    icon = "🎤",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "New Words",
                    value = "${s.newWordsLearned}/${s.totalNewWords}",
                    icon = "📚",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Day Streak",
                    value = "${s.streakDays} days",
                    icon = "🔥",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sprint 10: Visible growth — XP bar + level badge
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Lv ${levelInfo.level}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.buttonContentColor
                        )
                        Text(text = "⭐", fontSize = 12.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "${levelInfo.xpIntoLevel} / ${levelInfo.xpForNextLevel} XP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                        Text(
                            text = "Total $totalXp XP",
                            fontSize = 11.sp,
                            color = Color(0xFF7F8C8D)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFECEFF1))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(levelInfo.progressToNextLevel.coerceIn(0f, 1f))
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(theme.primaryColor)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sprint 10: Daily 3-goal badges (previously computed but never rendered)
        dailyGoals?.let { goals ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Today's Goals",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        goals.goals.forEach { goal ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (goal.achieved) theme.primaryColor.copy(alpha = 0.9f)
                                            else Color(0xFFECEFF1)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = goal.badge,
                                        fontSize = 22.sp,
                                        modifier = Modifier.alpha(if (goal.achieved) 1f else 0.4f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (goal.achieved) "Done" else "Locked",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (goal.achieved) Color(0xFF2ECC71) else Color(0xFF95A5A6)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Weekly progress arc
        summary?.let { s ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Weekly progress ring
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ProgressRing(
                            progress = s.weeklyDayNumber.toFloat() / s.weeklyTotalDays,
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 8f,
                            activeColor = theme.activeRingColor
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${s.weeklyDayNumber}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C3E50)
                            )
                            Text(
                                text = "/ ${s.weeklyTotalDays}",
                                fontSize = 10.sp,
                                color = Color(0xFF7F8C8D)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "This Week",
                            fontSize = 14.sp,
                            color = Color(0xFF7F8C8D)
                        )
                        Text(
                            text = "Great progress!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                        Text(
                            text = "Keep the streak alive!",
                            fontSize = 12.sp,
                            color = Color(0xFF7F8C8D)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Streak counter
        summary?.let { s ->
            StreakCounter(streakDays = s.streakDays)
        }

        // 6. Parent Companion Card (Sprint 8)
        // Shown when the session has phoneme hints OR accuracy < 70%.
        // Addresses parents in Chinese so they can support without knowing English.
        summary?.let { s ->
            val accuracy = if (s.quizTotal > 0) s.quizScore.toFloat() / s.quizTotal else 1f
            val hasPhonemeHints = s.phonemeHints.isNotEmpty()
            if (hasPhonemeHints || accuracy < 0.7f) {
                Spacer(modifier = Modifier.height(12.dp))
                var isParentCardExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isParentCardExpanded = !isParentCardExpanded },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📩", fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "A Note for Parents",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE67E22),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (isParentCardExpanded) "▲" else "▼",
                                color = Color(0xFFE67E22),
                                fontSize = 12.sp
                            )
                        }
                        if (isParentCardExpanded) {
                            Spacer(Modifier.height(12.dp))
                            if (hasPhonemeHints) {
                                s.phonemeHints.forEach { hint ->
                                    Text(
                                        text = "${hint.emoji} ${hint.tipEnglish}",
                                        fontSize = 14.sp,
                                        color = Color(0xFF5D4037),
                                        lineHeight = 20.sp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            } else {
                                Text(
                                    text = "🦊 Your child worked hard today! If accuracy is below 70%, review today's words together before bed — replaying them slowly with TTS really helps!",
                                    fontSize = 14.sp,
                                    color = Color(0xFF5D4037),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 7. Share to parent (lightweight text link)
        val context = LocalContext.current
        TextButton(onClick = {
            val s = summary ?: return@TextButton
            val shareText = """
Lingo English — Today's Achievement
📝 New Words: ${s.newWordsLearned}
🏆 Quiz Score: ${s.quizScore}/${s.quizTotal}
🎯 Pronunciation: ${s.pronunciationScore?.toString() ?: "--"}/100
🔥 Streak: ${s.streakDays} days
📅 Day ${s.weeklyDayNumber}/${s.weeklyTotalDays}
            """.trimIndent()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(intent, "Share learning report"))
        }) {
            Text(
                text = "Share with parents",
                fontSize = 14.sp,
                color = Color(0xFF7F8C8D)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 7. Return to dashboard
        Button(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = theme.primaryColor,
                contentColor = theme.buttonContentColor
            )
        ) {
            Text(text = "Back to Home", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Confetti overlay
        ConfettiEffect(modifier = Modifier.fillMaxSize(), isVisible = true)
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color(0xFF7F8C8D),
                textAlign = TextAlign.Center
            )
        }
    }
}
