package com.college.quizapp.data.repository

import com.college.quizapp.data.model.User
import com.college.quizapp.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Handles authentication and user profile management via Firebase.
 */
class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    val currentFirebaseUser get() = auth.currentUser

    /**
     * Sign in with email and password.
     */
    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Sign in failed")
            val user = getUser(uid)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Register a new user with role and optional batch assignment.
     */
    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        role: UserRole,
        batchId: String = ""
    ): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Registration failed")

            val user = User(
                id = uid,
                email = email,
                name = name,
                role = role,
                batchId = batchId
            )

            usersCollection.document(uid).set(
                mapOf(
                    "id" to user.id,
                    "email" to user.email,
                    "name" to user.name,
                    "role" to user.role.name,
                    "batchId" to user.batchId
                )
            ).await()

            // Increment student count in batch if student
            if (role == UserRole.STUDENT && batchId.isNotEmpty()) {
                val batchRef = firestore.collection("batches").document(batchId)
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(batchRef)
                    val currentCount = snapshot.getLong("studentCount") ?: 0
                    transaction.update(batchRef, "studentCount", currentCount + 1)
                }.await()
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user profile from Firestore.
     */
    suspend fun getUser(uid: String): User {
        val doc = usersCollection.document(uid).get().await()
        return User(
            id = doc.id,
            email = doc.getString("email") ?: "",
            name = doc.getString("name") ?: "",
            role = try {
                UserRole.valueOf(doc.getString("role") ?: "STUDENT")
            } catch (e: Exception) {
                UserRole.STUDENT
            },
            batchId = doc.getString("batchId") ?: ""
        )
    }

    /**
     * Get current logged-in user's profile.
     */
    suspend fun getCurrentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            getUser(uid)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Sign out the current user.
     */
    fun signOut() {
        auth.signOut()
    }
}
