package com.lingo.learn.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lingo.learn.ui.R

@Composable
fun TextbookConfirmScreen(
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedVersion by remember { mutableStateOf("PEP (人教版)") }
    var isExpanded by remember { mutableStateOf(false) }

    val otherVersions = listOf("FLTRP (外研版)", "Hebei (冀教版)", "BNUP (北师大版)", "Shanghai (沪教版)")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF5))
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.confirm_textbook_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.confirm_textbook_desc),
            fontSize = 15.sp,
            color = Color(0xFF7F8C8D)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Default main card (PEP textbook version)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (selectedVersion == "PEP (人教版)") Color(0xFFFFF5D1) else Color.White)
                .border(
                    width = 2.dp,
                    color = if (selectedVersion == "PEP (人教版)") Color(0xFFFFD449) else Color(0xFFBDC3C7),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { selectedVersion = "PEP (人教版)" }
                .padding(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.pep_textbook_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.pep_textbook_desc),
                        fontSize = 13.sp,
                        color = Color(0xFF7F8C8D)
                    )
                }

                // Selection RadioButton
                RadioButton(
                    selected = selectedVersion == "PEP (人教版)",
                    onClick = { selectedVersion = "PEP (人教版)" },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFD449))
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Expandable other versions header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.other_textbooks_prompt),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7F8C8D)
            )
            Text(
                text = if (isExpanded) stringResource(R.string.hide_text) else stringResource(R.string.expand_text),
                fontSize = 14.sp,
                color = Color(0xFF5C6FF2),
                fontWeight = FontWeight.Bold
            )
        }

        // Expandable versions list
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                otherVersions.forEach { version ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selectedVersion == version) Color(0xFFF2F4F7) else Color.White)
                            .border(
                                width = 1.5.dp,
                                color = if (selectedVersion == version) Color(0xFF5C6FF2) else Color(0xFFECF0F3),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedVersion = version }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = version,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C3E50)
                            )
                            RadioButton(
                                selected = selectedVersion == version,
                                onClick = { selectedVersion = version },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF5C6FF2))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Confirm and proceed action button
        Button(
            onClick = { onConfirm(selectedVersion) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD449),
                contentColor = Color(0xFF2C3E50)
            )
        ) {
            Text(
                text = stringResource(R.string.confirm),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
