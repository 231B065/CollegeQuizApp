package com.college.quizapp.data.model

/**
 * Represents a user in the system — either a Teacher or a Student.
 */
data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: UserRole = UserRole.STUDENT,
    val batchId: String = ""
) {
    // No-arg constructor for Firestore deserialization
    constructor() : this("", "", "", UserRole.STUDENT, "")
}

enum class UserRole {
    TEACHER,
    STUDENT
}
