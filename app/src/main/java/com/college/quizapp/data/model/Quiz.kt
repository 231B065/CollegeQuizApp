package com.college.quizapp.data.model

import com.google.firebase.Timestamp

/**
 * Represents a quiz created by a teacher.
 */
data class Quiz(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val createdBy: String = "",
    val teacherName: String = "",
    val batchIds: List<String> = emptyList(),
    val questions: List<Question> = emptyList(),
    val durationMinutes: Int = 30,
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val bleSessionId: String = "",
    val isActive: Boolean = false
) {
    constructor() : this("", "", "", "", "", emptyList(), emptyList(), 30, null, null, "", false)
}

/**
 * Represents a single MCQ question with 4 options.
 */
data class Question(
    val text: String = "",
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = 0
) {
    constructor() : this("", emptyList(), 0)
}
