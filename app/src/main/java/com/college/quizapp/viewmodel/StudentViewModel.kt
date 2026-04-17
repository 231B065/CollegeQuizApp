package com.college.quizapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.college.quizapp.ble.BLEBeaconScanner
import com.college.quizapp.data.model.Quiz
import com.college.quizapp.data.model.QuizResult
import com.college.quizapp.data.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class StudentUiState(
    val isLoading: Boolean = false,
    val quizzes: List<Quiz> = emptyList(),
    val results: List<QuizResult> = emptyList(),
    val currentQuiz: Quiz? = null,
    val currentAnswers: MutableMap<String, String> = mutableMapOf(),
    val currentQuestionIndex: Int = 0,
    val isInBLERange: Boolean = false,
    val isBLEScanning: Boolean = false,
    val quizSubmitted: Boolean = false,
    val submissionResult: QuizResult? = null,
    val selectedResult: QuizResult? = null,
    val error: String? = null,
    val timeRemainingSeconds: Long = 0L
)

class StudentViewModel : ViewModel() {

    private val quizRepository = QuizRepository()
    private var bleScanner: BLEBeaconScanner? = null

    private val _uiState = MutableStateFlow(StudentUiState())
    val uiState: StateFlow<StudentUiState> = _uiState

    fun initBLE(context: Context) {
        if (bleScanner == null) {
            bleScanner = BLEBeaconScanner(context.applicationContext)
        }
    }

    fun loadQuizzes(batchId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            quizRepository.getStudentQuizzesFlow(batchId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
                .collect { quizzes ->
                    _uiState.value = _uiState.value.copy(quizzes = quizzes, isLoading = false)
                }
        }
    }

    fun loadQuizDetails(quizId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = quizRepository.getQuiz(quizId)
            result.fold(
                onSuccess = { quiz ->
                    _uiState.value = _uiState.value.copy(currentQuiz = quiz, isLoading = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
            )
        }
    }

    fun loadResults(studentId: String) {
        viewModelScope.launch {
            quizRepository.getStudentResultsFlow(studentId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
                .collect { results ->
                    _uiState.value = _uiState.value.copy(results = results)
                }
        }
    }

    fun startBLEVerification(bleSessionId: String) {
        val scanner = bleScanner ?: return
        scanner.startScanning(bleSessionId)

        viewModelScope.launch {
            scanner.isInRange.collect { inRange ->
                _uiState.value = _uiState.value.copy(isInBLERange = inRange)
            }
        }

        viewModelScope.launch {
            scanner.isScanning.collect { scanning ->
                _uiState.value = _uiState.value.copy(isBLEScanning = scanning)
            }
        }
    }

    fun stopBLEVerification() {
        bleScanner?.stopScanning()
        _uiState.value = _uiState.value.copy(
            isInBLERange = false,
            isBLEScanning = false
        )
    }

    fun startQuiz(quiz: Quiz) {
        _uiState.value = _uiState.value.copy(
            currentQuiz = quiz,
            currentAnswers = mutableMapOf(),
            currentQuestionIndex = 0,
            quizSubmitted = false,
            submissionResult = null,
            timeRemainingSeconds = quiz.durationMinutes.toLong() * 60
        )
    }

    fun selectAnswer(questionIndex: Int, answer: String) {
        val answers = _uiState.value.currentAnswers.toMutableMap()
        answers[questionIndex.toString()] = answer
        _uiState.value = _uiState.value.copy(currentAnswers = answers)
    }

    fun nextQuestion() {
        val quiz = _uiState.value.currentQuiz ?: return
        val current = _uiState.value.currentQuestionIndex
        if (current < quiz.questions.size - 1) {
            _uiState.value = _uiState.value.copy(currentQuestionIndex = current + 1)
        }
    }

    fun previousQuestion() {
        val current = _uiState.value.currentQuestionIndex
        if (current > 0) {
            _uiState.value = _uiState.value.copy(currentQuestionIndex = current - 1)
        }
    }

    fun goToQuestion(index: Int) {
        _uiState.value = _uiState.value.copy(currentQuestionIndex = index)
    }

    fun updateTimeRemaining(seconds: Long) {
        _uiState.value = _uiState.value.copy(timeRemainingSeconds = seconds)
    }

    fun submitQuiz(
        studentId: String,
        studentName: String,
        batchId: String,
        wasAutoSubmitted: Boolean = false
    ) {
        val quiz = _uiState.value.currentQuiz ?: return
        if (_uiState.value.quizSubmitted) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = quizRepository.submitQuiz(
                quizId = quiz.id,
                quizTitle = quiz.title,
                studentId = studentId,
                studentName = studentName,
                batchId = batchId,
                answers = _uiState.value.currentAnswers,
                questions = quiz.questions,
                wasAutoSubmitted = wasAutoSubmitted
            )

            result.fold(
                onSuccess = { quizResult ->
                    stopBLEVerification()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        quizSubmitted = true,
                        submissionResult = quizResult
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    fun selectResultForDetails(result: QuizResult) {
        _uiState.value = _uiState.value.copy(selectedResult = result)
        loadQuizDetails(result.quizId)
    }

    suspend fun hasAlreadySubmitted(quizId: String, studentId: String): Boolean {
        return quizRepository.hasStudentSubmitted(quizId, studentId)
    }

    fun resetQuizState() {
        _uiState.value = _uiState.value.copy(
            currentQuiz = null,
            currentAnswers = mutableMapOf(),
            currentQuestionIndex = 0,
            quizSubmitted = false,
            submissionResult = null,
            timeRemainingSeconds = 0L,
            isInBLERange = false
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        bleScanner?.stopScanning()
    }
}