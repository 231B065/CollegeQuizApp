package com.college.quizapp.data.model

/**
 * Represents a user in the system — either a Teacher or a Student.
 */
data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: UserRole = UserRole.STUDENT,
    val batchId: String = "",
    val isApproved: Boolean = false
) {
    // No-arg constructor for Firestore deserialization
    constructor() : this("", "", "", UserRole.STUDENT, "", true) // Default true for existing users parsed by reflection
}

enum class UserRole {
    TEACHER,
    STUDENT
}
