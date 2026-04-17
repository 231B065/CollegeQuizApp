package com.college.quizapp.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // ✅ FIX ADDED
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.college.quizapp.data.model.QuizResult
import com.college.quizapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizHistoryScreen(
    results: List<QuizResult>,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (QuizResult) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault()) }

    val totalQuizzes = results.size
    val averagePercentage = if (results.isNotEmpty()) {
        results.map { it.score.toFloat() / it.totalQuestions * 100 }.average().toInt()
    } else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Quiz History",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Overall Performance",
                            style = MaterialTheme.typography.titleMedium,
                            color = Purple60,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$totalQuizzes",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = InfoBlue,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Quizzes Taken",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$averagePercentage%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = when {
                                        averagePercentage >= 80 -> SuccessGreen
                                        averagePercentage >= 50 -> WarningOrange
                                        else -> ErrorRed
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Average Score",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            if (results.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📋", style = MaterialTheme.typography.displayLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No quiz history yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondary
                            )
                            Text("Complete a quiz to see your results here",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            } else {
                items(results.sortedByDescending { it.submittedAt }) { result ->

                    val percentage = if (result.totalQuestions > 0)
                        (result.score.toFloat() / result.totalQuestions * 100).toInt() else 0

                    val scoreColor = when {
                        percentage >= 80 -> SuccessGreen
                        percentage >= 50 -> WarningOrange
                        else -> ErrorRed
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        modifier = Modifier.clickable { onNavigateToDetails(result) } // ✅ now works
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(scoreColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$percentage%",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = scoreColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(result.quizTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("Score: ${result.score}/${result.totalQuestions}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    result.submittedAt?.let { dateFormat.format(it.toDate()) } ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }

                            if (result.wasAutoSubmitted) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = WarningOrange.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "AUTO",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarningOrange,
                                        fontWeight = FontWeight.Bold
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