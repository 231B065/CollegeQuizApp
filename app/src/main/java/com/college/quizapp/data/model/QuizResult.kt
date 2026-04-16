package com.college.quizapp.data.model

import com.google.firebase.Timestamp

/**
 * Represents a student's quiz submission and result.
 */
data class QuizResult(
    val id: String = "",
    val quizId: String = "",
    val quizTitle: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val batchId: String = "",
    val answers: Map<String, Int> = emptyMap(), // questionIndex (as string) -> selectedOption
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val submittedAt: Timestamp? = null,
    val wasAutoSubmitted: Boolean = false
) {
    constructor() : this("", "", "", "", "", "", emptyMap(), 0, 0, null, false)
}
