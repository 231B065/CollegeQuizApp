package com.college.quizapp.data.model

import com.google.firebase.Timestamp

data class AttendanceSession(
    val id: String = "",
    val batchId: String = "",
    val teacherId: String = "",
    val date: Timestamp = Timestamp.now(),
    val active: Boolean = true,
    // Maps student ID to student Name for easy display
    val presentStudents: Map<String, String> = emptyMap()
)
