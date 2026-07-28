package org.akj.lingo.learn.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isTokenVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF5))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "⚙️ Model & API Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF2C3E50)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFFDF5))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Card 1: API Server Credentials
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "🌐 API Connection & Credentials",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C6FF2)
                    )

                    OutlinedTextField(
                        value = uiState.baseUrl,
                        onValueChange = { viewModel.updateBaseUrl(it) },
                        label = { Text("Base URL") },
                        placeholder = { Text("https://ark.cn-beijing.volces.com/api/plan") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = uiState.authToken,
                        onValueChange = { viewModel.updateAuthToken(it) },
                        label = { Text("Auth Token / API Key") },
                        placeholder = { Text("Enter your Ark / Volcengine API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { isTokenVisible = !isTokenVisible }) {
                                Text(if (isTokenVisible) "Hide" else "Show", fontSize = 12.sp)
                            }
                        }
                    )

                    OutlinedTextField(
                        value = uiState.groupId,
                        onValueChange = { viewModel.updateGroupId(it) },
                        label = { Text("Group ID / Tenant ID (Optional)") },
                        placeholder = { Text("Leave blank if not required") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // Card 2: AI Model Selection (Three Channels)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "🤖 AI Models (Three Channels)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C6FF2)
                    )

                    OutlinedTextField(
                        value = uiState.primaryModel,
                        onValueChange = { viewModel.updatePrimaryModel(it) },
                        label = { Text("Primary LLM Model") },
                        placeholder = { Text("glm-5.2") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = uiState.fallbackModel,
                        onValueChange = { viewModel.updateFallbackModel(it) },
                        label = { Text("Fallback LLM Model") },
                        placeholder = { Text("deepseek-v4-flash") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = uiState.ttsModel,
                        onValueChange = { viewModel.updateTtsModel(it) },
                        label = { Text("TTS Speech Synthesis Model") },
                        placeholder = { Text("seed-tts-2.0") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = uiState.asrModel,
                        onValueChange = { viewModel.updateAsrModel(it) },
                        label = { Text("ASR Voice Recognition Model") },
                        placeholder = { Text("volc.seedasr.sauc.duration") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // Language preference card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "🌐 App Language",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C6FF2)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val languages = listOf("en" to "English", "zh" to "中文")
                        languages.forEach { (code, label) ->
                            val isSelected = uiState.language == code
                            val bgColor = if (isSelected) Color(0xFF5C6FF2) else Color(0xFFECEFF1)
                            val textColor = if (isSelected) Color.White else Color(0xFF2C3E50)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bgColor)
                                    .clickable { viewModel.updateLanguage(code) }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text(label, fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                            }
                        }
                    }
                    Text(
                        text = "Restart required to apply language change",
                        fontSize = 12.sp,
                        color = Color(0xFF7F8C8D)
                    )
                }
            }

            // Card 3: Evaluation Threshold & Token Budget
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "🎯 Evaluation & Budget Thresholds",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C6FF2)
                    )

                    Column {
                        Text(
                            text = "ASR Minimum Passing Threshold: ${uiState.asrScoreThreshold} pts",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2C3E50)
                        )
                        Slider(
                            value = uiState.asrScoreThreshold.toFloat(),
                            onValueChange = { viewModel.updateAsrScoreThreshold(it.toInt()) },
                            valueRange = 30f..90f,
                            steps = 6
                        )
                    }

                    OutlinedTextField(
                        value = uiState.monthlyTokenLimit.toString(),
                        onValueChange = {
                            val limit = it.filter { char -> char.isDigit() }.toIntOrNull() ?: 50000
                            viewModel.updateMonthlyTokenLimit(limit)
                        },
                        label = { Text("Monthly Token Budget Limit") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // Connection Test Result Banner
            uiState.connectionTestResult?.let { resultText ->
                val isSuccess = uiState.connectionTestSuccess == true
                val bgColor = if (isSuccess) Color(0xFFE8F8F0) else Color(0xFFFFECE5)
                val textColor = if (isSuccess) Color(0xFF2ECC71) else Color(0xFFFF7052)
                val borderColor = if (isSuccess) Color(0xFF52D68A) else Color(0xFFFF7052)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor)
                        .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = resultText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }

            if (uiState.isSaved) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE8F8F0))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Settings Saved Successfully! ✅",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2ECC71)
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.testApiConnection() },
                    enabled = !uiState.isTestingConnection,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (uiState.isTestingConnection) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("🧪 Test Connection", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { viewModel.saveSettings() },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD449),
                        contentColor = Color(0xFF2C3E50)
                    )
                ) {
                    Text("💾 Save Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
