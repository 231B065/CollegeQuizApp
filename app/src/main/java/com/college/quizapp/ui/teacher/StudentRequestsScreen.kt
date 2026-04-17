package com.college.quizapp.ui.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.college.quizapp.data.model.User
import com.college.quizapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentRequestsScreen(
    pendingStudents: List<User>,
    isLoading: Boolean,
    successMessage: String?,
    onLoadRequests: () -> Unit,
    onApproveStudent: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    onClearSuccess: () -> Unit
) {
    LaunchedEffect(Unit) {
        onLoadRequests()
    }

    if (successMessage != null) {
        LaunchedEffect(successMessage) {
            kotlinx.coroutines.delay(2000)
            onClearSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Requests", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        snackbarHost = {
            if (successMessage != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = SuccessGreen,
                    contentColor = DarkBackground
                ) {
                    Text(successMessage, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        if (isLoading && pendingStudents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Purple40)
            }
        } else if (pendingStudents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "All caught up",
                        modifier = Modifier.size(64.dp),
                        tint = SuccessGreen.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No pending requests.",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Text(
                        "All students have been approved.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                items(pendingStudents) { student ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Purple40.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = Purple60
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    student.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    student.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Batch ID: ${student.batchId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Purple40
                                )
                            }
                            
                            Button(
                                onClick = { onApproveStudent(student.id, student.batchId) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                            ) {
                                Text("Approve", color = DarkBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
