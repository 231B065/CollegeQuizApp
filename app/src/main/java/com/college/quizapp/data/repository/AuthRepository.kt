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

            val isApproved = role == UserRole.TEACHER

            val user = User(
                id = uid,
                email = email,
                name = name,
                role = role,
                batchId = batchId,
                isApproved = isApproved
            )

            usersCollection.document(uid).set(
                mapOf(
                    "id" to user.id,
                    "email" to user.email,
                    "name" to user.name,
                    "role" to user.role.name,
                    "batchId" to user.batchId,
                    "isApproved" to user.isApproved
                )
            ).await()

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
            batchId = doc.getString("batchId") ?: "",
            isApproved = doc.getBoolean("isApproved") ?: true // Default to true for existing users
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

    /**
     * Get pending students for a list of batch IDs.
     */
    suspend fun getPendingStudents(batchIds: List<String>): Result<List<User>> {
        return try {
            if (batchIds.isEmpty()) return Result.success(emptyList())

            // Firestore 'in' queries are limited to 10 items. For simplicity, assuming teacher has <= 10 batches.
            // If they have more, we'd need to batch the queries.
            val snapshot = usersCollection
                .whereEqualTo("role", UserRole.STUDENT.name)
                .whereEqualTo("isApproved", false)
                .whereIn("batchId", batchIds)
                .get()
                .await()

            val users = snapshot.documents.map { doc ->
                User(
                    id = doc.id,
                    email = doc.getString("email") ?: "",
                    name = doc.getString("name") ?: "",
                    role = UserRole.STUDENT,
                    batchId = doc.getString("batchId") ?: "",
                    isApproved = false
                )
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Approve a student to join a batch.
     */
    suspend fun approveStudent(studentId: String, batchId: String): Result<Unit> {
        return try {
            val userRef = usersCollection.document(studentId)
            val batchRef = firestore.collection("batches").document(batchId)

            firestore.runTransaction { transaction ->
                val userSnapshot = transaction.get(userRef)
                if (userSnapshot.exists() && userSnapshot.getBoolean("isApproved") != true) {
                    transaction.update(userRef, "isApproved", true)
                    
                    // Increment batch count
                    val batchSnapshot = transaction.get(batchRef)
                    if (batchSnapshot.exists()) {
                        val currentCount = batchSnapshot.getLong("studentCount") ?: 0
                        transaction.update(batchRef, "studentCount", currentCount + 1)
                    }
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
