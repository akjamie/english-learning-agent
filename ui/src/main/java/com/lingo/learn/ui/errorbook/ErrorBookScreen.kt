package org.akj.lingo.learn.ui.errorbook

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.akj.lingo.learn.domain.model.ErrorBookEntry
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorBookScreen(
    onBack: () -> Unit,
    onEntryClick: (ErrorBookEntry) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ErrorBookViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val explanationState by viewModel.explanationState.collectAsState()
    val currentGrade = LocalContext.current.getSharedPreferences("lingo_app_prefs", Context.MODE_PRIVATE)
        .getString("grade", "Grade 4") ?: "Grade 4"
    
    var selectedEntry by remember { mutableStateOf<ErrorBookEntry?>(null) }
    var showExplanationDialog by remember { mutableStateOf(false) }

    // Sprint 20: refresh every time the tab re-enters composition.
    // The ViewModel is Activity-scoped, so init{} alone would only
    // load once per process and miss errors written during learning.
    LaunchedEffect(Unit) { viewModel.loadErrors() }

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
                    Text("📝", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Error Book", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF2C3E50))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFFDF5))
        )

        if (uiState.loadError != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🦊", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(uiState.loadError!!, fontSize = 14.sp, color = Color(0xFF7F8C8D), lineHeight = 20.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadErrors() }) { Text("Retry") }
                }
            }
        } else if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF5C6FF2))
            }
        } else if (uiState.totalCount == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("No errors yet!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                    Spacer(Modifier.height(8.dp))
                    Text("Great job! All words mastered.", fontSize = 14.sp, color = Color(0xFF7F8C8D))
                }
            }
        } else {

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)
        ) {
            StatsBar(
                total = uiState.totalCount,
                review = uiState.reviewCount,
                consolidated = uiState.consolidatedCount
            )
            Spacer(Modifier.height(12.dp))
            SortChips(mode = uiState.sortMode, onModeChange = viewModel::setSortMode)
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (uiState.observationEntries.isNotEmpty()) {
                    item(key = "observation_header") {
                        SectionHeader(
                            emoji = "🔵",
                            title = "Graduation Watch",
                            subtitle = "Almost there! Keep practicing to graduate these words.",
                            count = uiState.observationEntries.size
                        )
                    }
                    items(uiState.observationEntries, key = { "obs_${it.id}" }) { entry ->
                        ErrorCard(
                            entry = entry,
                            onClick = {
                                selectedEntry = entry
                                showExplanationDialog = true
                                viewModel.fetchExplanation(entry.vocabId, entry.errorType, currentGrade)
                                onEntryClick(entry)
                            },
                            viewModel = viewModel
                        )
                    }
                    if (uiState.entries.isNotEmpty()) {
                        item(key = "practice_header") {
                            SectionHeader(
                                emoji = "📖",
                                title = "Needs Practice",
                                subtitle = "Keep going, you will get there!",
                                count = uiState.entries.size
                            )
                        }
                    }
                }
                items(uiState.entries, key = { it.id }) { entry ->
                    ErrorCard(
                        entry = entry,
                        onClick = {
                            selectedEntry = entry
                            showExplanationDialog = true
                            viewModel.fetchExplanation(entry.vocabId, entry.errorType, currentGrade)
                            onEntryClick(entry)
                        },
                        viewModel = viewModel
                    )
                }
            }
        }
        }
    }
    
    if (showExplanationDialog && selectedEntry != null) {
        ModalBottomSheet(
            onDismissRequest = { showExplanationDialog = false },
            containerColor = Color(0xFFFFFDF5)
        ) {
            val entry = selectedEntry!!
            val explanation = explanationState[entry.vocabId]
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                org.akj.lingo.learn.ui.components.LingoAvatar(
                    expression = if (explanation == null || explanation == "Thinking...") org.akj.lingo.learn.ui.components.LingoExpression.THINKING else org.akj.lingo.learn.ui.components.LingoExpression.HAPPY,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Lingo's Tip for '${entry.vocabId}'",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                if (explanation == null || explanation == "Thinking...") {
                    CircularProgressIndicator(color = Color(0xFF5C6FF2))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Lingo is thinking...", color = Color.Gray, fontSize = 14.sp)
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = explanation,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = Color(0xFF2C3E50)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
@Composable
private fun SectionHeader(emoji: String, title: String, subtitle: String, count: Int) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
            Spacer(Modifier.width(8.dp))
            Surface(
                color = Color(0xFFECEFF1),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    count.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5C6FF2)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(subtitle, fontSize = 12.sp, color = Color(0xFF7F8C8D))
    }
}

@Composable
private fun StatsBar(total: Int, review: Int, consolidated: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatChip("Total", total, 0xFF5C6FF2)
        StatChip("Review", review, 0xFFFF7052)
        StatChip("Done", consolidated, 0xFF2ECC71)
    }
}

@Composable
private fun RowScope.StatChip(label: String, count: Int, color: Long) {
    Surface(
        modifier = Modifier.weight(1f),
        color = Color(color).copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(color))
            Text(label, fontSize = 12.sp, color = Color(0xFF7F8C8D))
        }
    }
}

@Composable
private fun SortChips(mode: ErrorBookSortMode, onModeChange: (ErrorBookSortMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ErrorBookSortMode.entries.forEach { chipMode ->
            val isSelected = chipMode == mode
            val bgColor by animateColorAsState(
                if (isSelected) Color(0xFF5C6FF2) else Color(0xFFECEFF1),
                label = "chip_bg"
            )
            val textColor = if (isSelected) Color.White else Color(0xFF2C3E50)
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .clickable { onModeChange(chipMode) }
                    .padding(horizontal = 16.dp)
                    .semantics { contentDescription = "Sort by ${chipMode.name.lowercase()}" },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (chipMode) {
                        ErrorBookSortMode.PRIORITY -> "Priority"
                        ErrorBookSortMode.DATE -> "Recent"
                        ErrorBookSortMode.ERROR_COUNT -> "Most Errors"
                        ErrorBookSortMode.ERROR_TYPE -> "Type"
                    },
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(entry: ErrorBookEntry, onClick: () -> Unit, viewModel: ErrorBookViewModel) {
    val daysAgo = ((System.currentTimeMillis() - entry.lastErrorTimestamp) / (24 * 3600 * 1000)).toInt()
    val priorityPct = (entry.priorityScore / 10f).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${entry.vocabId}, error type ${entry.errorType}" },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(viewModel.getErrorTypeEmoji(entry.errorType), fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = entry.vocabId,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                }
                Badge(
                    containerColor = Color(viewModel.getStatusColor(entry.status)),
                    contentColor = Color.White
                ) {
                    Text(viewModel.getStatusEmoji(entry.status), fontSize = 10.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        when (entry.status) {
                            "TO_REVIEW" -> "Review"
                            "CONSOLIDATED" -> "Learning"
                            "GRADUATION_OBSERVATION" -> "Almost"
                            else -> entry.status
                        },
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Type: ${viewModel.getErrorTypeDisplay(entry.errorType)}",
                        fontSize = 13.sp,
                        color = Color(0xFF7F8C8D),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Errors: ${entry.errorCount}x | $daysAgo days ago",
                        fontSize = 13.sp,
                        color = Color(0xFF7F8C8D)
                    )
                }
                Text(
                    "Score: ${entry.priorityScore.roundToInt()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5C6FF2)
                )
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFECEFF1))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(priorityPct)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when {
                                priorityPct > 0.7f -> Color(0xFFFF7052)
                                priorityPct > 0.4f -> Color(0xFFFFD449)
                                else -> Color(0xFF52D68A)
                            }
                        )
                )
            }

            Spacer(Modifier.height(12.dp))

            // Sprint 20: explicit retry affordance — makes the word due
            // for the next daily review quiz instead of waiting for its
            // Ebbinghaus schedule to elapse.
            Button(
                onClick = { viewModel.retryWord(entry.vocabId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD449),
                    contentColor = Color(0xFF2C3E50)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("🔁 Practice Again", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
