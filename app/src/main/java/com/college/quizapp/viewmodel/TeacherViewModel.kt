package com.college.quizapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.college.quizapp.ble.BLEBeaconAdvertiser
import com.college.quizapp.data.model.Batch
import com.college.quizapp.data.model.Question
import com.college.quizapp.data.model.User
import com.college.quizapp.data.model.Quiz
import com.college.quizapp.data.model.QuizResult
import com.college.quizapp.data.model.AttendanceSession
import com.college.quizapp.data.repository.QuizRepository
import com.college.quizapp.data.repository.AuthRepository
import com.college.quizapp.data.repository.AttendanceRepository
import com.college.quizapp.nearby.NearbyAttendanceManager
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Date

data class TeacherUiState(
    val isLoading: Boolean = false,
    val quizzes: List<Quiz> = emptyList(),
    val batches: List<Batch> = emptyList(),
    val selectedQuiz: Quiz? = null,
    val quizResults: List<QuizResult> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null,
    val isAdvertising: Boolean = false,
    val pendingStudents: List<User> = emptyList(),
    // Attendance specific
    val activeAttendanceSession: AttendanceSession? = null,
    val isAttendanceAdvertising: Boolean = false
)

class TeacherViewModel : ViewModel() {

    private val quizRepository = QuizRepository()
    private val authRepository = AuthRepository()
    private val attendanceRepository = AttendanceRepository()

    private var bleAdvertiser: BLEBeaconAdvertiser? = null
    var nearbyAttendanceManager: NearbyAttendanceManager? = null
        private set

    private val _uiState = MutableStateFlow(TeacherUiState())
    val uiState: StateFlow<TeacherUiState> = _uiState

    /**
     * Initialize BLE advertiser and Nearby manager with context.
     */
    fun initBLE(context: Context) {
        if (bleAdvertiser == null) {
            bleAdvertiser = BLEBeaconAdvertiser(context.applicationContext)
        }
        if (nearbyAttendanceManager == null) {
            nearbyAttendanceManager = NearbyAttendanceManager(context.applicationContext).apply {
                onStudentConnectedAndDataReceived = { studentId, studentName ->
                    markStudentPresent(studentId, studentName)
                }
            }
        }
    }

    // ================== ATTENDANCE ==================

    fun startAttendanceSession(batchId: String, teacherId: String, teacherName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = attendanceRepository.createSession(batchId, teacherId)
            result.onSuccess { session ->
                _uiState.value = _uiState.value.copy(
                    activeAttendanceSession = session,
                    isLoading = false,
                    isAttendanceAdvertising = true
                )
                // Begin Advertising via Nearby
                nearbyAttendanceManager?.startAdvertising(teacherName, session.id)

                // Start observing the session for live updates
                observeAttendanceSession(session.id)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    private fun observeAttendanceSession(sessionId: String) {
        viewModelScope.launch {
            attendanceRepository.getSessionFlow(sessionId).collect { session ->
                if (session != null) {
                    _uiState.value = _uiState.value.copy(activeAttendanceSession = session)
                }
            }
        }
    }

    private fun markStudentPresent(studentId: String, studentName: String) {
        val sessionId = _uiState.value.activeAttendanceSession?.id ?: return
        viewModelScope.launch {
            attendanceRepository.markStudentPresent(sessionId, studentId, studentName)
        }
    }

    fun endAttendanceSession() {
        val sessionId = _uiState.value.activeAttendanceSession?.id
        if (sessionId != null) {
            viewModelScope.launch {
                attendanceRepository.endSession(sessionId)
            }
        }
        nearbyAttendanceManager?.stopAdvertising()
        _uiState.value = _uiState.value.copy(
            activeAttendanceSession = null,
            isAttendanceAdvertising = false
        )
    }

    // ================================================

    /**
     * Load all quizzes created by this teacher.
     */
    fun loadQuizzes(teacherId: String) {
        viewModelScope.launch {
            quizRepository.getTeacherQuizzesFlow(teacherId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
                .collect { quizzes ->
                    _uiState.value = _uiState.value.copy(quizzes = quizzes, isLoading = false)
                }
        }
    }

    /**
     * Load batches for this teacher.
     */
    fun loadBatches(teacherId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = quizRepository.getTeacherBatches(teacherId)
            result.fold(
                onSuccess = { batches ->
                    _uiState.value = _uiState.value.copy(batches = batches, isLoading = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            )
        }
    }

    /**
     * Load pending students for all of the teacher's batches.
     */
    fun loadPendingStudents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Extract batch IDs from loaded batches
            val batchIds = _uiState.value.batches.map { it.id }
            val result = authRepository.getPendingStudents(batchIds)
            result.fold(
                onSuccess = { students ->
                    _uiState.value = _uiState.value.copy(pendingStudents = students, isLoading = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
            )
        }
    }

    /**
     * Approve a student request.
     */
    fun approveStudent(studentId: String, batchId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = authRepository.approveStudent(studentId, batchId)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Student approved successfully",
                        isLoading = false
                    )
                    loadPendingStudents() // Refresh list
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
            )
        }
    }

    /**
     * Create a new batch.
     */
    fun createBatch(name: String, teacherId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = quizRepository.createBatch(name, teacherId)
            result.fold(
                onSuccess = {
                    loadBatches(teacherId)
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Batch '$name' created successfully",
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            )
        }
    }

    /**
     * Create a new quiz.
     */
    fun createQuiz(
        title: String,
        description: String,
        teacherId: String,
        teacherName: String,
        batchIds: List<String>,
        questions: List<Question>,
        durationMinutes: Int,
        startTime: Date,
        endTime: Date
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = quizRepository.createQuiz(
                title = title,
                description = description,
                teacherId = teacherId,
                teacherName = teacherName,
                batchIds = batchIds,
                questions = questions,
                durationMinutes = durationMinutes,
                startTime = Timestamp(startTime),
                endTime = Timestamp(endTime)
            )

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Quiz '$title' created successfully!",
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            )
        }
    }

    /**
     * Select a quiz and load its results.
     */
    fun selectQuiz(quiz: Quiz) {
        _uiState.value = _uiState.value.copy(selectedQuiz = quiz)
        loadQuizResults(quiz.id)
    }

    /**
     * Load results for a specific quiz.
     */
    private fun loadQuizResults(quizId: String) {
        viewModelScope.launch {
            quizRepository.getQuizResultsFlow(quizId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
                .collect { results ->
                    _uiState.value = _uiState.value.copy(quizResults = results)
                }
        }
    }

    /**
     * Toggle quiz active/inactive and start/stop BLE beacon.
     */
    fun toggleQuizActive(quiz: Quiz) {
        viewModelScope.launch {
            val newActive = !quiz.isActive
            val result = quizRepository.setQuizActive(quiz.id, newActive)

            result.onSuccess {
                if (newActive) {
                    bleAdvertiser?.startAdvertising(quiz.bleSessionId)
                } else {
                    bleAdvertiser?.stopAdvertising()
                }
                _uiState.value = _uiState.value.copy(
                    isAdvertising = newActive,
                    selectedQuiz = quiz.copy(isActive = newActive)
                )
            }

            result.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * Stop BLE advertising (cleanup).
     */
    fun stopAdvertising() {
        bleAdvertiser?.stopAdvertising()
        nearbyAttendanceManager?.stopAdvertising()
        _uiState.value = _uiState.value.copy(isAdvertising = false, isAttendanceAdvertising = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        bleAdvertiser?.stopAdvertising()
        nearbyAttendanceManager?.stopAdvertising()
    }
}

