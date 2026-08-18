package org.akj.lingo.learn.ui.modelconfiggate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

@Composable
fun ModelConfigGateScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModelConfigGateViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var isTokenVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF5))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Text("🧠", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Configure Lingo's AI Brain",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Set up your API connection to unlock Lingo's full AI features.",
            fontSize = 14.sp,
            color = Color(0xFF7F8C8D),
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(32.dp))

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
                OutlinedTextField(
                    value = state.baseUrl,
                    onValueChange = { viewModel.updateBaseUrl(it) },
                    label = { Text("Base URL") },
                    placeholder = { Text("https://ark.cn-beijing.volces.com/api/plan/v3") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.authToken,
                        onValueChange = { viewModel.updateAuthToken(it) },
                        label = { Text("Auth Token / API Key") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = if (isTokenVisible) VisualTransformation.None
                        else PasswordVisualTransformation()
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { isTokenVisible = !isTokenVisible }) {
                        Text(if (isTokenVisible) "Hide" else "Show", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = { viewModel.testConnection() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !state.isTesting && state.baseUrl.isNotBlank() && state.authToken.isNotBlank()
                ) {
                    if (state.isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("🔌 Test Connection", fontWeight = FontWeight.Bold)
                }

                if (state.testResult != null) {
                    val isSuccess = state.testSuccess == true
                    Surface(
                        color = if (isSuccess) Color(0xFF52D68A).copy(alpha = 0.1f)
                        else Color(0xFFFF7052).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = state.testResult!!,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = if (isSuccess) Color(0xFF52D68A) else Color(0xFFFF7052)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🤖 Models in Use",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5C6FF2)
                )
                ModelRow(label = "Primary LLM", value = state.primaryModel)
                ModelRow(label = "TTS", value = state.ttsModel)
                ModelRow(label = "ASR", value = state.asrModel)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        var voiceExpanded by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FFF4)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎙️ Voice Endpoints",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF27AE60),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { voiceExpanded = !voiceExpanded }) {
                        Text(if (voiceExpanded) "Hide" else "Edit", fontSize = 12.sp)
                    }
                }
                if (voiceExpanded) {
                    OutlinedTextField(
                        value = state.ttsBaseUrl,
                        onValueChange = { viewModel.updateTtsBaseUrl(it) },
                        label = { Text("TTS URL") },
                        placeholder = { Text("https://openspeech.bytedance.com/api/v3/plan/tts/unidirectional") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state.ttsResourceId,
                        onValueChange = { viewModel.updateTtsResourceId(it) },
                        label = { Text("TTS Resource ID") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state.ttsSpeaker,
                        onValueChange = { viewModel.updateTtsSpeaker(it) },
                        label = { Text("TTS Speaker") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state.asrWsUrl,
                        onValueChange = { viewModel.updateAsrWsUrl(it) },
                        label = { Text("ASR WebSocket URL") },
                        placeholder = { Text("wss://openspeech.bytedance.com/api/v3/plan/sauc/bigmodel_async") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state.asrResourceId,
                        onValueChange = { viewModel.updateAsrResourceId(it) },
                        label = { Text("ASR Resource ID") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = { viewModel.testVoice() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !state.isVoiceTesting && state.authToken.isNotBlank()
                    ) {
                        if (state.isVoiceTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("🔊 Test Voice", fontWeight = FontWeight.Bold)
                    }
                    if (state.voiceTestResult != null) {
                        val isVoiceSuccess = state.voiceTestSuccess == true
                        Surface(
                            color = if (isVoiceSuccess) Color(0xFF52D68A).copy(alpha = 0.1f)
                            else Color(0xFFFF7052).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = state.voiceTestResult!!,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = if (isVoiceSuccess) Color(0xFF52D68A) else Color(0xFFFF7052)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = state.isConfigured,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5C6FF2),
                disabledContainerColor = Color(0xFFB0BEC5)
            )
        ) {
            Text("✅ Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ModelRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF7F8C8D),
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2C3E50)
        )
    }
}
