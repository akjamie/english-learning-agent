package org.akj.lingo.learn.ui.weeklyplan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import org.akj.lingo.learn.ui.R
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlanScreen(
    grade: String,
    onBack: () -> Unit,
    onViewReport: () -> Unit,
    onStartLearning: (dayIndex: Int) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: WeeklyPlanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val today = viewModel.getDayOfWeek()

    // Sprint 10.5: read the child's diagnostic level so plan generation can match difficulty
    // to the selected grade AND the measured level (A/B/C).
    val diagnosticLevel = LocalContext.current
        .getSharedPreferences("lingo_app_prefs", android.content.Context.MODE_PRIVATE)
        .getString("diagnostic_level", "B") ?: "B"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF5))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📅", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.weekly_plan_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF2C3E50))
                }
            },
            actions = {
                TextButton(onClick = onViewReport) {
                    Text(stringResource(R.string.view_report), fontWeight = FontWeight.Bold, color = Color(0xFF5C6FF2))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFFDF5))
        )

        if (uiState.loadError != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🦊", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        uiState.loadError!!,
                        fontSize = 14.sp,
                        color = Color(0xFF7F8C8D),
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadPlan() }) {
                        Text("Retry")
                    }
                }
            }
        } else if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF5C6FF2))
            }
        } else {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date range header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (uiState.theme.isNotBlank()) "📅 Weekly Plan · ${uiState.theme} Theme"
                                else "📅 Weekly Plan",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C3E50),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                viewModel.getFormattedDateRange(),
                                fontSize = 13.sp,
                                color = Color(0xFF7F8C8D)
                            )
                        }
                        if (uiState.difficultyCoefficient > 0) {
                            Badge(
                                containerColor = Color(0xFF5C6FF2).copy(alpha = 0.15f),
                                contentColor = Color(0xFF5C6FF2)
                            ) {
                                Text("Level ${uiState.difficultyCoefficient}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Day cards
            if (uiState.days.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📋", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("No plan yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                            Spacer(Modifier.height(4.dp))
                            Text("Tap below to generate a plan", fontSize = 14.sp, color = Color(0xFF7F8C8D))
                        }
                    }
                }
            } else {
                items(uiState.days, key = { it.day }) { dayItem ->
                    PlanDayCard(dayItem = dayItem, isToday = dayItem.day == today, onStart = onStartLearning)
                }
            }

            // Actions
            item {
                Spacer(Modifier.height(8.dp))

                if (uiState.days.isEmpty()) {
                    // No plan yet: show full-width primary CTA
                    Button(
                        onClick = { viewModel.generateNewPlan(grade, diagnosticLevel) },
                        enabled = !uiState.isGenerating,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD449),
                            contentColor = Color(0xFF2C3E50)
                        )
                    ) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF2C3E50)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (uiState.isGenerating) "Generating..." else "✨ Generate Plan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    // Plan exists: show subtle secondary TextButton so user knows it's optional
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = { viewModel.generateNewPlan(grade, diagnosticLevel) },
                            enabled = !uiState.isGenerating
                        ) {
                            if (uiState.isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF7F8C8D)
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                if (uiState.isGenerating) "Regenerating..." else "🔄 Regenerate Plan",
                                fontSize = 13.sp,
                                color = Color(0xFF7F8C8D)
                            )
                        }
                    }
                }

                uiState.generateError?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFECE5),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            error,
                            modifier = Modifier.padding(12.dp),
                            color = Color(0xFFFF7052),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanDayCard(dayItem: PlanDayItem, isToday: Boolean, onStart: (Int) -> Unit = {}, getFocusEmoji: (String) -> String = ::getFocusEmoji) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (dayItem.isCompleted) 1f else 0f,
        animationSpec = tween(600),
        label = "day_progress"
    )
    var rationaleExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Day circle indicator
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (dayItem.isCompleted) Color(0xFF52D68A) else if (isToday) Color(0xFF5C6FF2) else Color(0xFFECEFF1)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayItem.isCompleted) {
                            Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        } else {
                            Text("${dayItem.day}", color = if (isToday) Color.White else Color(0xFF2C3E50), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            "Day ${dayItem.day}",
                            fontSize = 13.sp,
                            color = Color(0xFF7F8C8D)
                        )
                        Text(
                            "${getFocusEmoji(dayItem.focus)} ${dayItem.focus}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isToday) {
                        Badge(containerColor = Color(0xFF5C6FF2), contentColor = Color.White) {
                            Text("Today", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (dayItem.rationale.isNotBlank()) {
                        Spacer(Modifier.width(4.dp))
                        // Sprint 15: 48dp touch target for accessibility (was 24dp)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { rationaleExpanded = !rationaleExpanded }
                                .semantics { contentDescription = "Why this arrangement?" },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (rationaleExpanded) Color(0xFF5C6FF2) else Color(0xFFF0F4FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "i",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rationaleExpanded) Color.White else Color(0xFF5C6FF2)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Reference sentence
            if (dayItem.referenceSentence.isNotBlank()) {
                Text(
                    "\"${dayItem.referenceSentence}\"",
                    fontSize = 14.sp,
                    color = Color(0xFF7F8C8D),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
            }

            // Target words as chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                dayItem.targetWords.take(6).forEach { word ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF0F4FF))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(word, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF5C6FF2))
                    }
                }
                if (dayItem.targetWords.size > 6) {
                    Text("+${dayItem.targetWords.size - 6}", fontSize = 12.sp, color = Color(0xFF7F8C8D), modifier = Modifier.align(Alignment.CenterVertically))
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⏱️", fontSize = 14.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("${dayItem.durationMinutes} min", fontSize = 13.sp, color = Color(0xFF7F8C8D))
                }
                // Sprint 10.5: Start is available for today AND any future/past day.
                // Previously gated to !isCompleted && !isToday, which hid today's card.
                OutlinedButton(
                    onClick = { onStart(dayItem.day) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (dayItem.isCompleted) "Review" else if (isToday) "Start Today" else "Start",
                        fontSize = 12.sp,
                        color = Color(0xFF5C6FF2)
                    )
                }
            }

            // Progress bar for completed status
            if (dayItem.isCompleted) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFECEFF1))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(animatedProgress).fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF52D68A))
                    )
                }
            }

            // "Why this arrangement?" expandable annotation (Sprint 6)
            AnimatedVisibility(
                visible = rationaleExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFF0F4FF),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "💡 Why this arrangement?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5C6FF2)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                dayItem.rationale,
                                fontSize = 13.sp,
                                color = Color(0xFF2C3E50),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


