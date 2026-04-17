package com.college.quizapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.college.quizapp.data.model.Question
import com.college.quizapp.data.repository.AiQuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the AI quiz generation flow.
 */
data class AiQuizUiState(
    val topic: String = "",
    val difficulty: String = "Medium",
    val questionCount: Int = 10,
    val isGenerating: Boolean = false,
    val generatedQuestions: List<Question> = emptyList(),
    val selectedIndices: Set<Int> = emptySet(),
    val error: String? = null,
    val generationComplete: Boolean = false
)

/**
 * ViewModel for the AI-powered quiz generation feature.
 * Manages the input → generation → review → selection flow.
 */
class AiQuizViewModel : ViewModel() {

    private val repository = AiQuizRepository()

    private val _uiState = MutableStateFlow(AiQuizUiState())
    val uiState: StateFlow<AiQuizUiState> = _uiState

    // ==================== INPUT ACTIONS ====================

    fun updateTopic(topic: String) {
        _uiState.value = _uiState.value.copy(topic = topic)
    }

    fun updateDifficulty(difficulty: String) {
        _uiState.value = _uiState.value.copy(difficulty = difficulty)
    }

    fun updateQuestionCount(count: Int) {
        _uiState.value = _uiState.value.copy(questionCount = count.coerceIn(5, 20))
    }

    // ==================== GENERATION ====================

    /**
     * Generate questions using AI. Auto-selects all generated questions.
     */
    fun generateQuestions() {
        val state = _uiState.value
        if (state.topic.isBlank()) {
            _uiState.value = state.copy(error = "Please enter a topic")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                error = null,
                generationComplete = false,
                generatedQuestions = emptyList(),
                selectedIndices = emptySet()
            )

            val result = repository.generateQuestions(
                topic = state.topic,
                difficulty = state.difficulty,
                count = state.questionCount
            )

            result.fold(
                onSuccess = { questions ->
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        generatedQuestions = questions,
                        selectedIndices = questions.indices.toSet(), // Select all by default
                        generationComplete = true
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = e.message ?: "Generation failed"
                    )
                }
            )
        }
    }

    /**
     * Retry generation with the same parameters.
     */
    fun retry() {
        generateQuestions()
    }

    // ==================== SELECTION ====================

    /**
     * Toggle a single question's selection state.
     */
    fun toggleQuestion(index: Int) {
        val current = _uiState.value.selectedIndices
        _uiState.value = _uiState.value.copy(
            selectedIndices = if (current.contains(index)) {
                current - index
            } else {
                current + index
            }
        )
    }

    /**
     * Select all generated questions.
     */
    fun selectAll() {
        _uiState.value = _uiState.value.copy(
            selectedIndices = _uiState.value.generatedQuestions.indices.toSet()
        )
    }

    /**
     * Deselect all questions.
     */
    fun deselectAll() {
        _uiState.value = _uiState.value.copy(selectedIndices = emptySet())
    }

    /**
     * Get the list of currently selected questions.
     */
    fun getSelectedQuestions(): List<Question> {
        val state = _uiState.value
        return state.selectedIndices.sorted().mapNotNull { index ->
            state.generatedQuestions.getOrNull(index)
        }
    }

    /**
     * Get count of selected questions.
     */
    fun getSelectedCount(): Int = _uiState.value.selectedIndices.size

    // ==================== CLEANUP ====================

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Reset all state for a fresh generation.
     */
    fun resetState() {
        _uiState.value = AiQuizUiState()
    }
}
