package com.lingo.learn.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lingo.learn.ui.R

@Composable
fun GradeSelectScreen(
    onGradeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var selectedBand by remember { mutableStateOf<String?>(null) } // "PRIMARY", "JUNIOR", "SENIOR"
    var selectedGrade by remember { mutableStateOf<String?>(null) }

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
            text = stringResource(R.string.select_grade_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.select_grade_desc),
            fontSize = 15.sp,
            color = Color(0xFF7F8C8D)
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // Band 1: Primary School
        GradeBandCard(
            title = stringResource(R.string.grade_band_primary),
            description = stringResource(R.string.grade_band_primary_desc),
            bgColors = listOf(Color(0xFFFFD449), Color(0xFFFFE082)),
            isSelected = selectedBand == "PRIMARY",
            isAnySelected = selectedBand != null,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                selectedBand = "PRIMARY"
                selectedGrade = null
            }
        )
        
        // Secondary expandable: Primary grade selectors
        AnimatedVisibility(
            visible = selectedBand == "PRIMARY",
            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)),
            exit = shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("Grade 4", "Grade 5", "Grade 6").forEach { grade ->
                    GradeSubItem(
                        text = grade,
                        isSelected = selectedGrade == grade,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedGrade = grade
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Band 2: Junior High
        GradeBandCard(
            title = stringResource(R.string.grade_band_junior),
            description = stringResource(R.string.grade_band_junior_desc),
            bgColors = listOf(Color(0xFF5C6FF2), Color(0xFF8B9CF4)),
            isSelected = selectedBand == "JUNIOR",
            isAnySelected = selectedBand != null,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                selectedBand = "JUNIOR"
                selectedGrade = null
            }
        )

        // Secondary expandable: Junior High grade selectors
        AnimatedVisibility(
            visible = selectedBand == "JUNIOR",
            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)),
            exit = shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("Junior 1", "Junior 2", "Junior 3").forEach { grade ->
                    GradeSubItem(
                        text = grade,
                        isSelected = selectedGrade == grade,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedGrade = grade
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Band 3: Senior High
        GradeBandCard(
            title = stringResource(R.string.grade_band_senior),
            description = stringResource(R.string.grade_band_senior_desc),
            bgColors = listOf(Color(0xFF1C2F5E), Color(0xFF2C3E50)),
            isSelected = selectedBand == "SENIOR",
            isAnySelected = selectedBand != null,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                selectedBand = "SENIOR"
                selectedGrade = null
            }
        )

        // Secondary expandable: Senior High grade selectors
        AnimatedVisibility(
            visible = selectedBand == "SENIOR",
            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)),
            exit = shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("Senior 1", "Senior 2", "Senior 3").forEach { grade ->
                    GradeSubItem(
                        text = grade,
                        isSelected = selectedGrade == grade,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedGrade = grade
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Grade confirmation action button
        Button(
            onClick = {
                selectedGrade?.let { onGradeSelected(it) }
            },
            enabled = selectedGrade != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD449),
                contentColor = Color(0xFF2C3E50),
                disabledContainerColor = Color(0xFFBDC3C7),
                disabledContentColor = Color(0xFFECF0F1)
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

@Composable
fun GradeBandCard(
    title: String,
    description: String,
    bgColors: List<Color>,
    isSelected: Boolean,
    isAnySelected: Boolean,
    onClick: () -> Unit
) {
    // Spring scale and alpha bounce animation
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else if (isAnySelected) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "CardScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isSelected || !isAnySelected) 1.0f else 0.5f,
        label = "CardAlpha"
    )

    val contentColor = if (bgColors.first() == Color(0xFFFFD449)) Color(0xFF2C3E50) else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(bgColors))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.9f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor.copy(alpha = alpha)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = contentColor.copy(alpha = alpha * 0.8f)
            )
        }
    }
}

@Composable
fun GradeSubItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "SubScale"
    )

    val containerColor = if (isSelected) Color(0xFFFFD449) else Color.White
    val contentColor = Color(0xFF2C3E50)

    Box(
        modifier = modifier
            .scale(scale)
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1
        )
    }
}
