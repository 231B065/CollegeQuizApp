package com.college.quizapp.ui.teacher

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.college.quizapp.ui.theme.*
import com.college.quizapp.viewmodel.AiQuizUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiQuizInputScreen(
    uiState: AiQuizUiState,
    onTopicChange: (String) -> Unit,
    onDifficultyChange: (String) -> Unit,
    onQuestionCountChange: (Int) -> Unit,
    onGenerate: () -> Unit,
    onClearError: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val difficulties = listOf("Easy", "Medium", "Hard")
    val scrollState = rememberScrollState()

    // Animated rotation for the sparkle icon while generating
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
    val sparkleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_rotation"
    )

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onClearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✨ ", fontSize = 20.sp)
                        Text(
                            "AI Quiz Generator",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Hero header card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Purple40.copy(alpha = 0.3f),
                                    Teal60.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            "Generate Quiz Questions",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Enter a topic and let AI create high-quality MCQs for your students. Review and select questions before creating a quiz.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Topic Input
            SectionHeader("Topic")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = uiState.topic,
                        onValueChange = onTopicChange,
                        label = { Text("Enter topic") },
                        placeholder = { Text("e.g., Operating Systems, DBMS, Java") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = Purple60
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Purple40,
                            unfocusedBorderColor = TextMuted,
                            cursorColor = Purple60
                        )
                    )
                }
            }

            // Difficulty Level
            SectionHeader("Difficulty Level")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        difficulties.forEach { level ->
                            val isSelected = uiState.difficulty == level
                            val chipColor = when (level) {
                                "Easy" -> SuccessGreen
                                "Medium" -> WarningOrange
                                "Hard" -> ErrorRed
                                else -> Purple40
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onDifficultyChange(level) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) chipColor.copy(alpha = 0.2f)
                                else DarkBackground.copy(alpha = 0.5f),
                                border = if (isSelected) null else null
                            ) {
                                Box(
                                    modifier = Modifier
                                        .then(
                                            if (isSelected) Modifier.border(
                                                1.5.dp,
                                                chipColor,
                                                RoundedCornerShape(12.dp)
                                            )
                                            else Modifier.border(
                                                1.dp,
                                                TextMuted.copy(alpha = 0.3f),
                                                RoundedCornerShape(12.dp)
                                            )
                                        )
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            when (level) {
                                                "Easy" -> "🟢"
                                                "Medium" -> "🟡"
                                                "Hard" -> "🔴"
                                                else -> ""
                                            },
                                            fontSize = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            level,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSelected) chipColor else TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Number of Questions
            SectionHeader("Number of Questions")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Questions to generate",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Purple40.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "${uiState.questionCount}",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = Purple60,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = uiState.questionCount.toFloat(),
                        onValueChange = { onQuestionCountChange(it.toInt()) },
                        valueRange = 5f..20f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = Purple40,
                            activeTrackColor = Purple40,
                            inactiveTrackColor = TextMuted.copy(alpha = 0.3f)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("5", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("20", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
            }

            // Generate Button
            AnimatedContent(
                targetState = uiState.isGenerating,
                label = "generate_button"
            ) { isGenerating ->
                if (isGenerating) {
                    // Generating state
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Purple40.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Purple60,
                                modifier = Modifier
                                    .size(48.dp)
                                    .rotate(sparkleRotation)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Generating Questions...",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "AI is crafting ${uiState.questionCount} ${uiState.difficulty.lowercase()} questions on \"${uiState.topic}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Purple40,
                                trackColor = TextMuted.copy(alpha = 0.2f)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onGenerate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = uiState.topic.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple40,
                            disabledContainerColor = Purple40.copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Generate with AI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
