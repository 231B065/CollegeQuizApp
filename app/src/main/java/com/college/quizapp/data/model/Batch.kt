package com.college.quizapp.data.model

/**
 * Represents a student batch (e.g., CSE-2024-A).
 */
data class Batch(
    val id: String = "",
    val name: String = "",
    val createdBy: String = "",
    val studentCount: Int = 0
) {
    constructor() : this("", "", "", 0)
}
