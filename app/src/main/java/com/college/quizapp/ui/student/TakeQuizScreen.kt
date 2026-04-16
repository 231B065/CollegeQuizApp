package com.college.quizapp.ui.student

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.college.quizapp.data.model.Quiz
import com.college.quizapp.data.model.QuizResult
import com.college.quizapp.data.model.User
import com.college.quizapp.ui.theme.*
import com.college.quizapp.viewmodel.StudentUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeQuizScreen(
    user: User,
    quiz: Quiz,
    uiState: StudentUiState,
    onSelectAnswer: (Int, Int) -> Unit,
    onNextQuestion: () -> Unit,
    onPreviousQuestion: () -> Unit,
    onGoToQuestion: (Int) -> Unit,
    onSubmitQuiz: (Boolean) -> Unit,
    onStartBLEVerification: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onUpdateTime: (Long) -> Unit
) {
    var showSubmitDialog by remember { mutableStateOf(false) }
    var bleCheckStarted by remember { mutableStateOf(false) }
    var timeRemainingSeconds by remember { mutableStateOf(quiz.durationMinutes.toLong() * 60) }

    // Start BLE verification
    LaunchedEffect(quiz.bleSessionId) {
        if (!bleCheckStarted) {
            onStartBLEVerification(quiz.bleSessionId)
            bleCheckStarted = true
        }
    }

    // Timer
    LaunchedEffect(key1 = uiState.quizSubmitted) {
        if (!uiState.quizSubmitted) {
            while (timeRemainingSeconds > 0) {
                delay(1000L)
                timeRemainingSeconds--
                onUpdateTime(timeRemainingSeconds)

                if (timeRemainingSeconds <= 0) {
                    onSubmitQuiz(true) // Auto-submit on time expiry
                }
            }
        }
    }

    // Auto-submit if BLE lost
    LaunchedEffect(uiState.isInBLERange) {
        if (bleCheckStarted && !uiState.isInBLERange && uiState.currentAnswers.isNotEmpty()) {
            // Give some grace period
            delay(15000)
            if (!uiState.isInBLERange && !uiState.quizSubmitted) {
                onSubmitQuiz(true)
            }
        }
    }

    // Result screen after submission
    if (uiState.quizSubmitted && uiState.submissionResult != null) {
        QuizSubmittedScreen(
            result = uiState.submissionResult,
            onDone = onNavigateBack
        )
        return
    }

    // BLE verification gate
    if (!uiState.isInBLERange && !uiState.quizSubmitted) {
        BLEVerificationScreen(
            isScanning = uiState.isBLEScanning,
            onCancel = onNavigateBack
        )
        return
    }

    val currentQuestion = quiz.questions.getOrNull(uiState.currentQuestionIndex)
    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60
    val timeColor = when {
        timeRemainingSeconds <= 60 -> ErrorRed
        timeRemainingSeconds <= 300 -> WarningOrange
        else -> TextPrimary
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            quiz.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Question ${uiState.currentQuestionIndex + 1} of ${quiz.questions.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    // Timer
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = timeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = timeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        bottomBar = {
            // Navigation buttons
            Surface(
                color = DarkSurface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onPreviousQuestion,
                        enabled = uiState.currentQuestionIndex > 0,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Previous")
                    }

                    if (uiState.currentQuestionIndex == quiz.questions.size - 1) {
                        Button(
                            onClick = { showSubmitDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Submit", fontWeight = FontWeight.Bold, color = DarkBackground)
                        }
                    } else {
                        Button(
                            onClick = onNextQuestion,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Purple40)
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // BLE Status indicator
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (uiState.isInBLERange) SuccessGreen.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        if (uiState.isInBLERange) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                        contentDescription = null,
                        tint = if (uiState.isInBLERange) SuccessGreen else ErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (uiState.isInBLERange) "In classroom range" else "⚠ Signal weak — stay near teacher",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.isInBLERange) SuccessGreen else ErrorRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Question navigation dots
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(quiz.questions) { index, _ ->
                    val isAnswered = uiState.currentAnswers.containsKey(index.toString())
                    val isCurrent = index == uiState.currentQuestionIndex

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCurrent -> Purple40
                                    isAnswered -> SuccessGreen.copy(alpha = 0.3f)
                                    else -> DarkSurfaceVariant
                                }
                            )
                            .then(
                                if (isCurrent) Modifier.border(2.dp, Purple60, CircleShape)
                                else Modifier
                            )
                            .clickable { onGoToQuestion(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrent) TextPrimary else TextSecondary,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Question Card
            if (currentQuestion != null) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Question ${uiState.currentQuestionIndex + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Purple60,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            currentQuestion.text,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Options
                val selectedOption = uiState.currentAnswers[uiState.currentQuestionIndex.toString()]

                currentQuestion.options.forEachIndexed { index, option ->
                    val isSelected = selectedOption == index

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                onSelectAnswer(uiState.currentQuestionIndex, index)
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Purple40.copy(alpha = 0.15f) else DarkSurfaceVariant
                        ),
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(2.dp, Purple40)
                        } else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Purple40 else DarkCard
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${'A' + index}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                option,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )

                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Purple40,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Answered count
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Answered: ${uiState.currentAnswers.size} / ${quiz.questions.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Submit confirmation dialog
    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            containerColor = DarkSurfaceVariant,
            title = {
                Text("Submit Quiz?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "You have answered ${uiState.currentAnswers.size} out of ${quiz.questions.size} questions.",
                        color = TextSecondary
                    )
                    if (uiState.currentAnswers.size < quiz.questions.size) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "⚠ ${quiz.questions.size - uiState.currentAnswers.size} questions are unanswered!",
                            color = WarningOrange
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        onSubmitQuiz(false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Submit", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("Continue Quiz", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun BLEVerificationScreen(
    isScanning: Boolean,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = Purple40,
                        strokeWidth = 4.dp
                    )
                } else {
                    Icon(
                        Icons.Default.BluetoothSearching,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Purple60
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Verifying Location",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Scanning for teacher's Bluetooth beacon...\nMake sure you are in the classroom.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "📡 Bluetooth must be enabled",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarningOrange
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun QuizSubmittedScreen(
    result: QuizResult,
    onDone: () -> Unit
) {
    val percentage = if (result.totalQuestions > 0)
        (result.score.toFloat() / result.totalQuestions * 100).toInt() else 0

    val scoreColor = when {
        percentage >= 80 -> SuccessGreen
        percentage >= 50 -> WarningOrange
        else -> ErrorRed
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (result.wasAutoSubmitted) "⏰" else "🎉",
                    fontSize = 64.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    if (result.wasAutoSubmitted) "Quiz Auto-Submitted"
                    else "Quiz Submitted!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                if (result.wasAutoSubmitted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "You left the classroom or time ran out",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningOrange
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Score circle
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(scoreColor.copy(alpha = 0.1f))
                        .border(4.dp, scoreColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$percentage%",
                            style = MaterialTheme.typography.displaySmall,
                            color = scoreColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${result.score}/${result.totalQuestions}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple40)
                ) {
                    Text("Back to Dashboard", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
