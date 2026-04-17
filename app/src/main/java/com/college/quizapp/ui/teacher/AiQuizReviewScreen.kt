package com.college.quizapp.ui.teacher

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.college.quizapp.data.model.Question
import com.college.quizapp.ui.theme.*
import com.college.quizapp.viewmodel.AiQuizUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiQuizReviewScreen(
    uiState: AiQuizUiState,
    onToggleQuestion: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onRegenerate: () -> Unit,
    onCreateQuiz: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val selectedCount = uiState.selectedIndices.size
    val totalCount = uiState.generatedQuestions.size
    val allSelected = selectedCount == totalCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Review Questions",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "$totalCount generated · $selectedCount selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // Regenerate button
                    IconButton(onClick = onRegenerate) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Regenerate",
                            tint = Purple60
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        bottomBar = {
            // Bottom action bar
            Surface(
                color = DarkSurface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Selection summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "$selectedCount of $totalCount questions selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        TextButton(onClick = { if (allSelected) onDeselectAll() else onSelectAll() }) {
                            Text(
                                if (allSelected) "Deselect All" else "Select All",
                                color = Purple60,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Create Quiz button
                    Button(
                        onClick = onCreateQuiz,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = selectedCount > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple40,
                            disabledContainerColor = Purple40.copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (selectedCount > 0) "Create Quiz with $selectedCount Questions"
                            else "Select at least 1 question",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Info banner
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Teal60.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Teal60,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Tap the eye icon to reveal correct answers. Use checkboxes to select questions for your quiz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Teal80
                        )
                    }
                }
            }

            // Question cards
            itemsIndexed(uiState.generatedQuestions) { index, question ->
                AiQuestionCard(
                    index = index,
                    question = question,
                    isSelected = uiState.selectedIndices.contains(index),
                    onToggleSelect = { onToggleQuestion(index) }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
fun AiQuestionCard(
    index: Int,
    question: Question,
    isSelected: Boolean,
    onToggleSelect: () -> Unit
) {
    var showAnswer by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DarkSurfaceVariant
            else DarkSurfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: number badge + checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Question number badge
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Purple40, Purple60)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${index + 1}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Question ${index + 1}",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Show/hide answer toggle
                    IconButton(
                        onClick = { showAnswer = !showAnswer },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (showAnswer) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = if (showAnswer) "Hide answer" else "Show answer",
                            tint = if (showAnswer) Teal60 else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Selection checkbox
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Purple40,
                            checkmarkColor = TextPrimary,
                            uncheckedColor = TextMuted
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Question text
            Text(
                question.text,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Options
            question.options.forEachIndexed { optIndex, option ->
                val isCorrect = optIndex == question.correctOptionIndex
                val optionLabel = ('A' + optIndex).toString()

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        showAnswer && isCorrect -> SuccessGreen.copy(alpha = 0.12f)
                        else -> DarkBackground.copy(alpha = 0.4f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Option label badge
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        showAnswer && isCorrect -> SuccessGreen.copy(alpha = 0.3f)
                                        else -> TextMuted.copy(alpha = 0.15f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                optionLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = when {
                                    showAnswer && isCorrect -> SuccessGreen
                                    else -> TextSecondary
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            option,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                showAnswer && isCorrect -> SuccessGreen
                                else -> TextSecondary
                            },
                            fontWeight = if (showAnswer && isCorrect) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )

                        // Correct answer icon
                        if (showAnswer && isCorrect) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Correct",
                                tint = SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
