package com.college.quizapp.ui.teacher

import androidx.compose.foundation.background
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
fun QuizResultsScreen(
    quizTitle: String,
    results: List<QuizResult>,
    onNavigateBack: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }
    val averageScore = if (results.isNotEmpty()) {
        results.map { it.score.toFloat() / it.totalQuestions * 100 }.average()
    } else 0.0
    val highestScore = results.maxOfOrNull { it.score } ?: 0
    val lowestScore = results.minOfOrNull { it.score } ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Results", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            quizTitle,
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
            // Stats summary
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Performance Summary",
                            style = MaterialTheme.typography.titleMedium,
                            color = Purple60,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ResultStatItem(
                                label = "Submissions",
                                value = "${results.size}",
                                color = InfoBlue
                            )
                            ResultStatItem(
                                label = "Average",
                                value = "${averageScore.toInt()}%",
                                color = Purple60
                            )
                            ResultStatItem(
                                label = "Highest",
                                value = "$highestScore",
                                color = SuccessGreen
                            )
                            ResultStatItem(
                                label = "Lowest",
                                value = "$lowestScore",
                                color = ErrorRed
                            )
                        }
                    }
                }
            }

            // Results List
            item {
                Text(
                    "Student Submissions",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
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
                            Text("📊", style = MaterialTheme.typography.displayLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No submissions yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(results.sortedByDescending { it.score }) { result ->
                    StudentResultCard(result = result, dateFormat = dateFormat)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun ResultStatItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
fun StudentResultCard(
    result: QuizResult,
    dateFormat: SimpleDateFormat
) {
    val percentage = if (result.totalQuestions > 0)
        (result.score.toFloat() / result.totalQuestions * 100).toInt() else 0

    val scoreColor = when {
        percentage >= 80 -> SuccessGreen
        percentage >= 50 -> WarningOrange
        else -> ErrorRed
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Purple40, Purple60)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    result.studentName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    result.studentName,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        result.submittedAt?.let { dateFormat.format(it.toDate()) } ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    if (result.wasAutoSubmitted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = WarningOrange.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "AUTO",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = WarningOrange,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Score
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${result.score}/${result.totalQuestions}",
                    style = MaterialTheme.typography.titleMedium,
                    color = scoreColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$percentage%",
                    style = MaterialTheme.typography.labelSmall,
                    color = scoreColor
                )
            }
        }
    }
}
