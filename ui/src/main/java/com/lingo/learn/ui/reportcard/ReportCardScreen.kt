package org.akj.lingo.learn.ui.reportcard

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportCardScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportCardViewModel = hiltViewModel()
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
                    Text("📄", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Growth Report", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
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
                    Text(uiState.loadError!!, fontSize = 14.sp, color = Color(0xFF7F8C8D))
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadReportCard() }) { Text("Retry") }
                }
            }
        } else if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF5C6FF2))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "${uiState.childName} · ${uiState.grade}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                        Spacer(Modifier.height(6.dp))
                        if (!uiState.cefrLabel.isNullOrEmpty()) {
                            Text(
                                text = "🌍 ${uiState.cefrLabel}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5C6FF2)
                            )
                            Spacer(Modifier.height(16.dp))
                        }

                        Text("Core Stats", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatChip("🔥", "${uiState.streakDays} days", Modifier.weight(1f))
                            StatChip("📚", "${uiState.totalSessions}", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatChip("📖", "${uiState.totalWordsLearned}", Modifier.weight(1f))
                            StatChip("🎯", "${(uiState.weeklyAccuracy * 100).toInt()}%", Modifier.weight(1f))
                        }

                        Spacer(Modifier.height(18.dp))
                        Text("Top 5 Mastered Words", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (uiState.masteredWords.isNotEmpty()) uiState.masteredWords.joinToString(" · ")
                            else "Keep practicing to master your first words!",
                            fontSize = 14.sp,
                            color = Color(0xFF7F8C8D)
                        )

                        Spacer(Modifier.height(18.dp))
                        Text("Top 3 Words to Practice", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (uiState.weakWords.isNotEmpty()) uiState.weakWords.joinToString(" · ")
                            else "No words in the error book — awesome job!",
                            fontSize = 14.sp,
                            color = Color(0xFF7F8C8D)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF5C6FF2).copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = uiState.encouragement ?: "",
                        modifier = Modifier.padding(20.dp),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C6FF2)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.shareReportCard() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6FF2))
                ) {
                    Text("📤 Export PNG & Share", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatChip(emoji: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFFECEFF1).copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 16.sp)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
        }
    }
}
