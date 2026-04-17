package com.college.quizapp.ui.teacher

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.college.quizapp.data.model.Batch
import com.college.quizapp.data.model.Question
import com.college.quizapp.data.model.User
import com.college.quizapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuizScreen(
    user: User,
    batches: List<Batch>,
    isLoading: Boolean,
    onCreateQuiz: (
        title: String,
        description: String,
        batchIds: List<String>,
        questions: List<Question>,
        durationMinutes: Int,
        startTime: Date,
        endTime: Date
    ) -> Unit,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf("30") }
    var selectedBatchIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var questions by remember {
        mutableStateOf(
            listOf(
                QuestionDraft("MCQ", "", mutableListOf("", "", "", ""), 0)
            )
        )
    }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }

    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create Quiz",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Basic Info Section
            item {
                SectionHeader("Quiz Details")

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Quiz Title") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Purple40,
                                unfocusedBorderColor = TextMuted
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Purple40,
                                unfocusedBorderColor = TextMuted
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = durationMinutes,
                            onValueChange = { durationMinutes = it.filter { c -> c.isDigit() } },
                            label = { Text("Duration (minutes)") },
                            leadingIcon = {
                                Icon(Icons.Default.Timer, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Purple40,
                                unfocusedBorderColor = TextMuted
                            )
                        )
                    }
                }
            }

            // Schedule Section
            item {
                SectionHeader("Schedule")

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Start Time
                        DateTimePickerButton(
                            label = "Start Time",
                            selectedDate = startDate,
                            dateFormat = dateFormat,
                            onPick = { date -> startDate = date },
                            context = context
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // End Time
                        DateTimePickerButton(
                            label = "End Time",
                            selectedDate = endDate,
                            dateFormat = dateFormat,
                            onPick = { date -> endDate = date },
                            context = context
                        )
                    }
                }
            }

            // Batch Selection
            item {
                SectionHeader("Select Batches")

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (batches.isEmpty()) {
                            Text(
                                "No batches created yet. Create batches first!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        } else {
                            batches.forEach { batch ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedBatchIds.contains(batch.id),
                                        onCheckedChange = { checked ->
                                            selectedBatchIds = if (checked) {
                                                selectedBatchIds + batch.id
                                            } else {
                                                selectedBatchIds - batch.id
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Purple40
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            batch.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextPrimary
                                        )
                                        Text(
                                            "${batch.studentCount} students",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Questions Section
            item {
                SectionHeader("Questions (${questions.size})")
            }

            itemsIndexed(questions) { index, question ->
                QuestionEditor(
                    questionNumber = index + 1,
                    question = question,
                    onUpdate = { updated ->
                        questions = questions.toMutableList().also { it[index] = updated }
                    },
                    onDelete = {
                        if (questions.size > 1) {
                            questions = questions.toMutableList().also { it.removeAt(index) }
                        }
                    },
                    canDelete = questions.size > 1
                )
            }

            // Add Question Button
            item {
                OutlinedButton(
                    onClick = {
                        questions = questions + QuestionDraft("MCQ", "", mutableListOf("", "", "", ""), 0)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple60)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Question")
                }
            }

            // Create Button
            item {
                val isValid = title.isNotBlank() && selectedBatchIds.isNotEmpty() &&
                        questions.all { it.text.isNotBlank() && (it.type == "SUBJECTIVE" || it.options.all { o -> o.isNotBlank() }) } &&
                        startDate != null && endDate != null &&
                        (durationMinutes.toIntOrNull() ?: 0) > 0

                Button(
                    onClick = {
                        onCreateQuiz(
                            title,
                            description,
                            selectedBatchIds.toList(),
                            questions.map { Question(it.type, it.text, it.options, it.correctOptionIndex) },
                            durationMinutes.toIntOrNull() ?: 30,
                            startDate!!,
                            endDate!!
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = isValid && !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Purple40)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Quiz", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Purple60,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

data class QuestionDraft(
    val type: String = "MCQ",
    val text: String,
    val options: List<String>,
    val correctOptionIndex: Int
)

@Composable
fun QuestionEditor(
    questionNumber: Int,
    question: QuestionDraft,
    onUpdate: (QuestionDraft) -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Purple40),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$questionNumber",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Question $questionNumber",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = question.text,
                onValueChange = { onUpdate(question.copy(text = it)) },
                label = { Text("Question text") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple40,
                    unfocusedBorderColor = TextMuted
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = question.type == "MCQ",
                    onClick = { onUpdate(question.copy(type = "MCQ")) },
                    colors = RadioButtonDefaults.colors(selectedColor = Purple40)
                )
                Text("Multiple Choice", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                
                Spacer(modifier = Modifier.width(16.dp))
                
                RadioButton(
                    selected = question.type == "SUBJECTIVE",
                    onClick = { onUpdate(question.copy(type = "SUBJECTIVE")) },
                    colors = RadioButtonDefaults.colors(selectedColor = Purple40)
                )
                Text("Subjective", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (question.type == "MCQ") {
                Text(
                    "Options (tap radio to mark correct answer)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                question.options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = question.correctOptionIndex == index,
                            onClick = { onUpdate(question.copy(correctOptionIndex = index)) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = SuccessGreen
                            )
                        )

                        OutlinedTextField(
                            value = option,
                            onValueChange = { newValue ->
                                val newOptions = question.options.toMutableList()
                                newOptions[index] = newValue
                                onUpdate(question.copy(options = newOptions))
                            },
                            label = { Text("Option ${('A' + index)}") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (question.correctOptionIndex == index) SuccessGreen else Purple40,
                                unfocusedBorderColor = TextMuted
                            )
                        )
                    }
                }
            } else {
                Text(
                    "Student will provide a text answer.",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun DateTimePickerButton(
    label: String,
    selectedDate: Date?,
    dateFormat: SimpleDateFormat,
    onPick: (Date) -> Unit,
    context: android.content.Context
) {
    val calendar = remember { Calendar.getInstance() }

    OutlinedButton(
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                            calendar.set(Calendar.MINUTE, minute)
                            onPick(calendar.time)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        false
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
    ) {
        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (selectedDate != null) "$label: ${dateFormat.format(selectedDate)}"
            else "Pick $label",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
