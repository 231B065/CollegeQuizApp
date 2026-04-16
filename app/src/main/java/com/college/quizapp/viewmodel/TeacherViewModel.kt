package com.college.quizapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.college.quizapp.ble.BLEBeaconAdvertiser
import com.college.quizapp.data.model.Batch
import com.college.quizapp.data.model.Question
import com.college.quizapp.data.model.Quiz
import com.college.quizapp.data.model.QuizResult
import com.college.quizapp.data.repository.QuizRepository
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
    val isAdvertising: Boolean = false
)

class TeacherViewModel : ViewModel() {

    private val quizRepository = QuizRepository()
    private var bleAdvertiser: BLEBeaconAdvertiser? = null

    private val _uiState = MutableStateFlow(TeacherUiState())
    val uiState: StateFlow<TeacherUiState> = _uiState

    /**
     * Initialize BLE advertiser with context.
     */
    fun initBLE(context: Context) {
        if (bleAdvertiser == null) {
            bleAdvertiser = BLEBeaconAdvertiser(context.applicationContext)
        }
    }

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
        _uiState.value = _uiState.value.copy(isAdvertising = false)
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
    }
}
