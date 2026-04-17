package com.college.quizapp.data.repository

import com.college.quizapp.data.model.Batch
import com.college.quizapp.data.model.Question
import com.college.quizapp.data.model.Quiz
import com.college.quizapp.data.model.QuizResult
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.college.quizapp.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel

/**
 * Handles all quiz, batch, and result operations with Firestore.
 */
class QuizRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val quizzesCollection = firestore.collection("quizzes")
    private val batchesCollection = firestore.collection("batches")
    private val resultsCollection = firestore.collection("quizResults")

    // ==================== BATCH OPERATIONS ====================

    /**
     * Create a new batch.
     */
    suspend fun createBatch(name: String, teacherId: String): Result<Batch> {
        return try {
            val docRef = batchesCollection.document()
            val batch = Batch(
                id = docRef.id,
                name = name,
                createdBy = teacherId,
                studentCount = 0
            )
            docRef.set(batch).await()
            Result.success(batch)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all batches created by a teacher.
     */
    suspend fun getTeacherBatches(teacherId: String): Result<List<Batch>> {
        return try {
            val snapshot = batchesCollection
                .whereEqualTo("createdBy", teacherId)
                .get()
                .await()
            val batches = snapshot.documents.map { doc ->
                Batch(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    createdBy = doc.getString("createdBy") ?: "",
                    studentCount = (doc.getLong("studentCount") ?: 0).toInt()
                )
            }
            Result.success(batches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all batches (for student registration).
     */
    suspend fun getAllBatches(): Result<List<Batch>> {
        return try {
            val snapshot = batchesCollection.get().await()
            val batches = snapshot.documents.map { doc ->
                Batch(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    createdBy = doc.getString("createdBy") ?: "",
                    studentCount = (doc.getLong("studentCount") ?: 0).toInt()
                )
            }
            Result.success(batches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== QUIZ OPERATIONS ====================

    /**
     * Create a new quiz.
     */
    suspend fun createQuiz(
        title: String,
        description: String,
        teacherId: String,
        teacherName: String,
        batchIds: List<String>,
        questions: List<Question>,
        durationMinutes: Int,
        startTime: Timestamp,
        endTime: Timestamp
    ): Result<Quiz> {
        return try {
            val docRef = quizzesCollection.document()
            val bleSessionId = UUID.randomUUID().toString()

            val quizData = hashMapOf(
                "id" to docRef.id,
                "title" to title,
                "description" to description,
                "createdBy" to teacherId,
                "teacherName" to teacherName,
                "batchIds" to batchIds,
                "questions" to questions.map { q ->
                    hashMapOf(
                        "type" to q.type,
                        "text" to q.text,
                        "options" to q.options,
                        "correctOptionIndex" to q.correctOptionIndex
                    )
                },
                "durationMinutes" to durationMinutes,
                "startTime" to startTime,
                "endTime" to endTime,
                "bleSessionId" to bleSessionId,
                "isActive" to false
            )

            docRef.set(quizData).await()

            val quiz = Quiz(
                id = docRef.id,
                title = title,
                description = description,
                createdBy = teacherId,
                teacherName = teacherName,
                batchIds = batchIds,
                questions = questions,
                durationMinutes = durationMinutes,
                startTime = startTime,
                endTime = endTime,
                bleSessionId = bleSessionId,
                isActive = false
            )

            Result.success(quiz)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all quizzes created by a teacher (real-time).
     */
    fun getTeacherQuizzesFlow(teacherId: String): Flow<List<Quiz>> = callbackFlow {
        val listener = quizzesCollection
            .whereEqualTo("createdBy", teacherId)
            .orderBy("startTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val quizzes = snapshot?.documents?.map { parseQuiz(it) } ?: emptyList()
                trySend(quizzes)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Get quizzes available for a student's batch (real-time).
     */
    fun getStudentQuizzesFlow(batchId: String): Flow<List<Quiz>> = callbackFlow {
        val listener = quizzesCollection
            .whereArrayContains("batchIds", batchId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val quizzes = snapshot?.documents?.map { parseQuiz(it) } ?: emptyList()
                trySend(quizzes)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Toggle quiz active status.
     */
    suspend fun setQuizActive(quizId: String, isActive: Boolean): Result<Unit> {
        return try {
            quizzesCollection.document(quizId).update("isActive", isActive).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a single quiz by ID.
     */
    suspend fun getQuiz(quizId: String): Result<Quiz> {
        return try {
            val doc = quizzesCollection.document(quizId).get().await()
            Result.success(parseQuiz(doc))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== RESULT OPERATIONS ====================

    /**
     * Submit a quiz result.
     */
    suspend fun submitQuiz(
        quizId: String,
        quizTitle: String,
        studentId: String,
        studentName: String,
        batchId: String,
        answers: Map<String, String>,
        questions: List<Question>,
        wasAutoSubmitted: Boolean = false
    ): Result<QuizResult> {
        return try {
            // Check if already submitted
            val existing = resultsCollection
                .whereEqualTo("quizId", quizId)
                .whereEqualTo("studentId", studentId)
                .get()
                .await()

            if (!existing.isEmpty) {
                return Result.failure(Exception("Quiz already submitted"))
            }

            // Calculate score
            var score = 0
            for ((index, answer) in answers) {
                val qIndex = index.toIntOrNull() ?: continue
                if (qIndex < questions.size && questions[qIndex].type == "MCQ") {
                    val selectedOption = answer.toIntOrNull()
                    if (selectedOption != null && questions[qIndex].correctOptionIndex == selectedOption) {
                        score++
                    }
                }
            }

            // AI Check for subjective questions
            val aiFeedback = mutableMapOf<String, String>()
            if (BuildConfig.GEMINI_API_KEY.isNotEmpty()) {
                try {
                    val generativeModel = GenerativeModel(
                        modelName = "gemini-1.5-flash",
                        apiKey = BuildConfig.GEMINI_API_KEY
                    )

                    for ((index, answer) in answers) {
                        val qIndex = index.toIntOrNull() ?: continue
                        if (qIndex < questions.size && questions[qIndex].type == "SUBJECTIVE") {
                            if (answer.isNotBlank()) {
                                try {
                                    val prompt = "Evaluate the following student answer for a college-level quiz.\n" +
                                            "Question: ${questions[qIndex].text}\n" +
                                            "Student's Answer: $answer\n" +
                                            "If the answer is essentially correct and complete, reply only with 'Correct'.\n" +
                                            "If the answer is incorrect or incomplete, provide brief, constructive feedback explaining what is wrong or missing. Keep it under 2 sentences."
                                    val response = generativeModel.generateContent(prompt)
                                    aiFeedback[index] = response.text ?: "Could not generate feedback."
                                } catch (e: Exception) {
                                    aiFeedback[index] = "Error evaluating answer."
                                }
                            } else {
                                aiFeedback[index] = "No answer provided."
                            }
                        }
                    }
                } catch (e: Exception) {
                    // API key might be missing or invalid
                }
            }

            val docRef = resultsCollection.document()
            val result = QuizResult(
                id = docRef.id,
                quizId = quizId,
                quizTitle = quizTitle,
                studentId = studentId,
                studentName = studentName,
                batchId = batchId,
                answers = answers,
                score = score,
                totalQuestions = questions.size,
                submittedAt = Timestamp.now(),
                wasAutoSubmitted = wasAutoSubmitted,
                aiFeedback = aiFeedback
            )

            docRef.set(
                hashMapOf(
                    "id" to result.id,
                    "quizId" to result.quizId,
                    "quizTitle" to result.quizTitle,
                    "studentId" to result.studentId,
                    "studentName" to result.studentName,
                    "batchId" to result.batchId,
                    "answers" to result.answers,
                    "score" to result.score,
                    "totalQuestions" to result.totalQuestions,
                    "submittedAt" to result.submittedAt,
                    "wasAutoSubmitted" to result.wasAutoSubmitted,
                    "aiFeedback" to result.aiFeedback
                )
            ).await()

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get results for a specific quiz (teacher view).
     */
    fun getQuizResultsFlow(quizId: String): Flow<List<QuizResult>> = callbackFlow {
        val listener = resultsCollection
            .whereEqualTo("quizId", quizId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val results = snapshot?.documents?.map { doc ->
                    QuizResult(
                        id = doc.id,
                        quizId = doc.getString("quizId") ?: "",
                        quizTitle = doc.getString("quizTitle") ?: "",
                        studentId = doc.getString("studentId") ?: "",
                        studentName = doc.getString("studentName") ?: "",
                        batchId = doc.getString("batchId") ?: "",
                        answers = (doc.get("answers") as? Map<String, Any>)?.mapValues { it.value.toString() } ?: emptyMap(),
                        score = (doc.getLong("score") ?: 0).toInt(),
                        totalQuestions = (doc.getLong("totalQuestions") ?: 0).toInt(),
                        submittedAt = doc.getTimestamp("submittedAt"),
                        wasAutoSubmitted = doc.getBoolean("wasAutoSubmitted") ?: false,
                        aiFeedback = (doc.get("aiFeedback") as? Map<String, Any>)?.mapValues { it.value.toString() } ?: emptyMap()
                    )
                } ?: emptyList()
                trySend(results)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Get all results for a student.
     */
    fun getStudentResultsFlow(studentId: String): Flow<List<QuizResult>> = callbackFlow {
        val listener = resultsCollection
            .whereEqualTo("studentId", studentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val results = snapshot?.documents?.map { doc ->
                    QuizResult(
                        id = doc.id,
                        quizId = doc.getString("quizId") ?: "",
                        quizTitle = doc.getString("quizTitle") ?: "",
                        studentId = doc.getString("studentId") ?: "",
                        studentName = doc.getString("studentName") ?: "",
                        batchId = doc.getString("batchId") ?: "",
                        answers = (doc.get("answers") as? Map<String, Any>)?.mapValues { it.value.toString() } ?: emptyMap(),
                        score = (doc.getLong("score") ?: 0).toInt(),
                        totalQuestions = (doc.getLong("totalQuestions") ?: 0).toInt(),
                        submittedAt = doc.getTimestamp("submittedAt"),
                        wasAutoSubmitted = doc.getBoolean("wasAutoSubmitted") ?: false,
                        aiFeedback = (doc.get("aiFeedback") as? Map<String, Any>)?.mapValues { it.value.toString() } ?: emptyMap()
                    )
                } ?: emptyList()
                trySend(results)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Check if student already submitted a quiz.
     */
    suspend fun hasStudentSubmitted(quizId: String, studentId: String): Boolean {
        return try {
            val snapshot = resultsCollection
                .whereEqualTo("quizId", quizId)
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    // ==================== HELPERS ====================

    @Suppress("UNCHECKED_CAST")
    private fun parseQuiz(doc: com.google.firebase.firestore.DocumentSnapshot): Quiz {
        val questionsRaw = doc.get("questions") as? List<Map<String, Any>> ?: emptyList()
        val questions = questionsRaw.map { q ->
            Question(
                type = q["type"] as? String ?: "MCQ",
                text = q["text"] as? String ?: "",
                options = q["options"] as? List<String> ?: emptyList(),
                correctOptionIndex = (q["correctOptionIndex"] as? Long ?: 0).toInt()
            )
        }

        return Quiz(
            id = doc.id,
            title = doc.getString("title") ?: "",
            description = doc.getString("description") ?: "",
            createdBy = doc.getString("createdBy") ?: "",
            teacherName = doc.getString("teacherName") ?: "",
            batchIds = doc.get("batchIds") as? List<String> ?: emptyList(),
            questions = questions,
            durationMinutes = (doc.getLong("durationMinutes") ?: 30).toInt(),
            startTime = doc.getTimestamp("startTime"),
            endTime = doc.getTimestamp("endTime"),
            bleSessionId = doc.getString("bleSessionId") ?: "",
            isActive = doc.getBoolean("isActive") ?: false
        )
    }
}
