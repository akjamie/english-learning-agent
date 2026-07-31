package org.akj.lingo.learn.ui.aigrowthnotes

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.akj.lingo.learn.domain.model.AgentDecisionLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sprint 6 (Enhancement 6) - parent-oriented timeline of every agent judgment Lingo made:
 * date + type tag + description. Backed by the persisted AgentDecisionLog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiGrowthNotesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AiGrowthNotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                    Text("🌱", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("AI Growth Notes", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF2C3E50))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFFDF5))
        )

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF5C6FF2))
                }
            }
            uiState.entries.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🌱", fontSize = 56.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("No AI notes yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Complete learning sessions and Lingo's observations will appear here.",
                            fontSize = 14.sp,
                            color = Color(0xFF7F8C8D),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 40.dp)
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Every time Lingo noticed something or adjusted the plan, it's logged here — transparently, for parents.",
                            fontSize = 13.sp,
                            color = Color(0xFF7F8C8D),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(uiState.entries, key = { it.id }) { entry ->
                        GrowthNoteCard(
                            entry = entry,
                            typeLabel = viewModel.typeLabel(entry.decisionType),
                            typeColor = Color(viewModel.typeColor(entry.decisionType))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GrowthNoteCard(
    entry: AgentDecisionLog,
    typeLabel: String,
    typeColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(typeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(typeLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = typeColor)
                }
                Text(
                    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(entry.timestamp)),
                    fontSize = 12.sp,
                    color = Color(0xFF7F8C8D)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                entry.description.ifBlank { entry.title },
                fontSize = 14.sp,
                color = Color(0xFF2C3E50),
                lineHeight = 20.sp
            )
        }
    }
}
