package com.college.quizapp.ui.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.college.quizapp.data.model.AttendanceSession
import com.college.quizapp.data.model.Batch
import com.college.quizapp.data.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAttendanceScreen(
    user: User,
    batches: List<Batch>,
    activeSession: AttendanceSession?,
    isAdvertising: Boolean,
    onStartAttendance: (batchId: String) -> Unit,
    onEndAttendance: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedBatchId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Attendance") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (activeSession == null) {
                // No active session -> Select batch to start
                Text(
                    text = "Select a batch to start an attendance session.",
                    style = MaterialTheme.typography.titleMedium
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(batches) { batch ->
                        Card(
                            onClick = { selectedBatchId = batch.id },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedBatchId == batch.id) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            PaddingValues(16.dp).let {
                                Text(
                                    text = batch.name,
                                    modifier = Modifier.padding(16.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        selectedBatchId?.let {
                            onStartAttendance(it)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedBatchId != null
                ) {
                    Text("Start Attendance Beacon")
                }
            } else {
                // Active session running
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isAdvertising) "Advertising Attendance Beacon..." else "Session Active",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (isAdvertising) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Text(
                    text = "Present Students: ${activeSession.presentStudents.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeSession.presentStudents.entries.toList()) { (studentId, studentName) ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = studentName, style = MaterialTheme.typography.bodyLarge)
                                Text(text = "PRESENT", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Button(
                    onClick = onEndAttendance,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("End Attendance Session")
                }
                
                var showWarning by remember { mutableStateOf(true) }
                if(showWarning) {
                    AlertDialog(
                        onDismissRequest = { showWarning = false },
                        confirmButton = {
                            TextButton(onClick = { showWarning = false }) { Text("Got it") }
                        },
                        title = { Text("Keep Screen On") },
                        text = { Text("To ensure reliable local network connections, please keep this screen active and do not turn off the device screen until you end the attendance.") },
                        icon = { Icon(Icons.Filled.Warning, contentDescription = null) }
                    )
                }
            }
        }
    }
}
