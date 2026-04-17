package com.college.quizapp.data.repository

import com.college.quizapp.data.model.AttendanceSession
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Handles operations related to smart attendance sessions.
 */
class AttendanceRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val attendanceCollection = firestore.collection("attendance")

    /**
     * Create a new attendance session for a given batch.
     */
    suspend fun createSession(batchId: String, teacherId: String): Result<AttendanceSession> {
        return try {
            val docRef = attendanceCollection.document()
            val session = AttendanceSession(
                id = docRef.id,
                batchId = batchId,
                teacherId = teacherId,
                active = true,
                presentStudents = emptyMap()
            )
            docRef.set(session).await()
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark a student as present in the given session.
     */
    suspend fun markStudentPresent(sessionId: String, studentId: String, studentName: String): Result<Unit> {
        return try {
            val updateData = mapOf(
                "presentStudents.$studentId" to studentName
            )
            attendanceCollection.document(sessionId).update(updateData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * End an active attendance session.
     */
    suspend fun endSession(sessionId: String): Result<Unit> {
        return try {
            attendanceCollection.document(sessionId).update("active", false).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get real-time updates for a specific attendance session (Teacher live list).
     */
    fun getSessionFlow(sessionId: String): Flow<AttendanceSession?> = callbackFlow {
        val listener = attendanceCollection.document(sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val session = snapshot?.toObject(AttendanceSession::class.java)
                trySend(session)
            }
        awaitClose { listener.remove() }
    }
}
