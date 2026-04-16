package com.college.quizapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.college.quizapp.data.model.Batch
import com.college.quizapp.data.model.User
import com.college.quizapp.data.model.UserRole
import com.college.quizapp.data.repository.AuthRepository
import com.college.quizapp.data.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val batches: List<Batch> = emptyList()
)

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val quizRepository = QuizRepository()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        checkCurrentUser()
    }

    /**
     * Check if user is already logged in on app start.
     */
    private fun checkCurrentUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                _uiState.value = _uiState.value.copy(
                    user = user,
                    isLoggedIn = true
                )
            }
        }
    }

    /**
     * Sign in with email and password.
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = authRepository.signIn(email, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user,
                        isLoggedIn = true,
                        error = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Sign in failed"
                    )
                }
            )
        }
    }

    /**
     * Register a new user.
     */
    fun signUp(
        email: String,
        password: String,
        name: String,
        role: UserRole,
        batchId: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = authRepository.signUp(email, password, name, role, batchId)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user,
                        isLoggedIn = true,
                        error = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Registration failed"
                    )
                }
            )
        }
    }

    /**
     * Load available batches (for student registration form).
     */
    fun loadBatches() {
        viewModelScope.launch {
            val result = quizRepository.getAllBatches()
            result.onSuccess { batches ->
                _uiState.value = _uiState.value.copy(batches = batches)
            }
        }
    }

    /**
     * Sign out the current user.
     */
    fun signOut() {
        authRepository.signOut()
        _uiState.value = AuthUiState()
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
