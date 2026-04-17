package com.college.quizapp.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.college.quizapp.data.model.Quiz
import com.college.quizapp.data.model.QuizResult
import com.college.quizapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizResultDetailScreen(
    result: QuizResult,
    quiz: Quiz?,
    isLoading: Boolean,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detailed Feedback", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Purple40)
            }
        } else if (quiz == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Failed to load quiz details.", color = ErrorRed)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Header card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Score: ${result.score} / ${result.totalQuestions}",
                                style = MaterialTheme.typography.titleLarge,
                                color = Purple60,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "MCQ score only. Subjective answers are evaluated by AI below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                items(quiz.questions.size) { index ->
                    val question = quiz.questions[index]
                    val studentAnswerRaw = result.answers[index.toString()] ?: ""
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Q${index + 1}: ${question.text}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (question.type == "MCQ") {
                                val selectedOptionIndex = studentAnswerRaw.toIntOrNull()
                                val studentAnswerText = if (selectedOptionIndex != null && selectedOptionIndex < question.options.size) {
                                    question.options[selectedOptionIndex]
                                } else "No answer"

                                val isCorrect = selectedOptionIndex == question.correctOptionIndex

                                Text(
                                    "Your Answer: $studentAnswerText",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isCorrect) SuccessGreen else ErrorRed
                                )
                                if (!isCorrect) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Correct Answer: ${question.options.getOrNull(question.correctOptionIndex) ?: ""}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SuccessGreen
                                    )
                                }
                            } else {
                                // Subjective Question
                                Text(
                                    "Your Answer:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                                Text(
                                    if (studentAnswerRaw.isBlank()) "No answer provided." else studentAnswerRaw,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // AI Feedback section
                                val feedback = result.aiFeedback[index.toString()]
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = InfoBlue.copy(alpha = 0.1f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = "AI",
                                                tint = InfoBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "AI Feedback",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = InfoBlue,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            feedback ?: "No feedback available.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
