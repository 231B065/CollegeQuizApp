package com.college.quizapp.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.college.quizapp.data.model.Quiz
import com.college.quizapp.data.model.UserRole
import com.college.quizapp.ui.auth.LoginScreen
import com.college.quizapp.ui.auth.RegisterScreen
import com.college.quizapp.ui.student.QuizHistoryScreen
import com.college.quizapp.ui.student.StudentDashboardScreen
import com.college.quizapp.ui.student.TakeQuizScreen
import com.college.quizapp.ui.teacher.*
import com.college.quizapp.viewmodel.AuthViewModel
import com.college.quizapp.viewmodel.StudentViewModel
import com.college.quizapp.viewmodel.TeacherViewModel

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val TEACHER_DASHBOARD = "teacher_dashboard"
    const val CREATE_QUIZ = "create_quiz"
    const val MANAGE_BATCHES = "manage_batches"
    const val QUIZ_DETAIL = "quiz_detail"
    const val QUIZ_RESULTS = "quiz_results"
    const val STUDENT_DASHBOARD = "student_dashboard"
    const val TAKE_QUIZ = "take_quiz"
    const val QUIZ_HISTORY = "quiz_history"
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    teacherViewModel: TeacherViewModel = viewModel(),
    studentViewModel: StudentViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    var selectedQuizForExam by remember { mutableStateOf<Quiz?>(null) }

    // Determine start destination
    val startDestination = if (authState.isLoggedIn) {
        when (authState.user?.role) {
            UserRole.TEACHER -> Routes.TEACHER_DASHBOARD
            UserRole.STUDENT -> Routes.STUDENT_DASHBOARD
            else -> Routes.LOGIN
        }
    } else {
        Routes.LOGIN
    }

    // Auto-navigate when login state changes
    LaunchedEffect(authState.isLoggedIn, authState.user?.role) {
        if (authState.isLoggedIn) {
            when (authState.user?.role) {
                UserRole.TEACHER -> {
                    navController.navigate(Routes.TEACHER_DASHBOARD) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                UserRole.STUDENT -> {
                    navController.navigate(Routes.STUDENT_DASHBOARD) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                else -> {}
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ==================== AUTH ====================

        composable(Routes.LOGIN) {
            LoginScreen(
                uiState = authState,
                onSignIn = { email, password ->
                    authViewModel.signIn(email, password)
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                },
                onClearError = { authViewModel.clearError() }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                uiState = authState,
                onSignUp = { email, password, name, role, batchId ->
                    authViewModel.signUp(email, password, name, role, batchId)
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onLoadBatches = { authViewModel.loadBatches() },
                onClearError = { authViewModel.clearError() }
            )
        }

        // ==================== TEACHER ====================

        composable(Routes.TEACHER_DASHBOARD) {
            val user = authState.user ?: return@composable
            val teacherState by teacherViewModel.uiState.collectAsState()

            LaunchedEffect(user.id) {
                teacherViewModel.loadQuizzes(user.id)
                teacherViewModel.loadBatches(user.id)
            }

            TeacherDashboardScreen(
                user = user,
                uiState = teacherState,
                onNavigateToCreateQuiz = {
                    navController.navigate(Routes.CREATE_QUIZ)
                },
                onNavigateToManageBatches = {
                    navController.navigate(Routes.MANAGE_BATCHES)
                },
                onNavigateToQuizDetail = { quiz ->
                    teacherViewModel.selectQuiz(quiz)
                    navController.navigate(Routes.QUIZ_DETAIL)
                },
                onSignOut = {
                    teacherViewModel.stopAdvertising()
                    authViewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CREATE_QUIZ) {
            val user = authState.user ?: return@composable
            val teacherState by teacherViewModel.uiState.collectAsState()

            LaunchedEffect(teacherState.successMessage) {
                if (teacherState.successMessage != null) {
                    teacherViewModel.clearSuccess()
                    navController.popBackStack()
                }
            }

            CreateQuizScreen(
                user = user,
                batches = teacherState.batches,
                isLoading = teacherState.isLoading,
                onCreateQuiz = { title, desc, batchIds, questions, duration, startTime, endTime ->
                    teacherViewModel.createQuiz(
                        title = title,
                        description = desc,
                        teacherId = user.id,
                        teacherName = user.name,
                        batchIds = batchIds,
                        questions = questions,
                        durationMinutes = duration,
                        startTime = startTime,
                        endTime = endTime
                    )
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MANAGE_BATCHES) {
            val user = authState.user ?: return@composable
            val teacherState by teacherViewModel.uiState.collectAsState()

            ManageBatchScreen(
                batches = teacherState.batches,
                isLoading = teacherState.isLoading,
                onCreateBatch = { name ->
                    teacherViewModel.createBatch(name, user.id)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.QUIZ_DETAIL) {
            val teacherState by teacherViewModel.uiState.collectAsState()
            val quiz = teacherState.selectedQuiz ?: return@composable

            QuizDetailScreen(
                quiz = quiz,
                results = teacherState.quizResults,
                isAdvertising = teacherState.isAdvertising,
                onToggleActive = { teacherViewModel.toggleQuizActive(quiz) },
                onViewResults = {
                    navController.navigate(Routes.QUIZ_RESULTS)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.QUIZ_RESULTS) {
            val teacherState by teacherViewModel.uiState.collectAsState()
            val quiz = teacherState.selectedQuiz ?: return@composable

            QuizResultsScreen(
                quizTitle = quiz.title,
                results = teacherState.quizResults,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ==================== STUDENT ====================

        composable(Routes.STUDENT_DASHBOARD) {
            val user = authState.user ?: return@composable
            val studentState by studentViewModel.uiState.collectAsState()

            LaunchedEffect(user.batchId) {
                studentViewModel.loadQuizzes(user.batchId)
                studentViewModel.loadResults(user.id)
            }

            StudentDashboardScreen(
                user = user,
                uiState = studentState,
                onNavigateToQuiz = { quiz ->
                    selectedQuizForExam = quiz
                    studentViewModel.startQuiz(quiz)
                    navController.navigate(Routes.TAKE_QUIZ)
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.QUIZ_HISTORY)
                },
                onSignOut = {
                    studentViewModel.stopBLEVerification()
                    authViewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.TAKE_QUIZ) {
            val user = authState.user ?: return@composable
            val studentState by studentViewModel.uiState.collectAsState()
            val quiz = selectedQuizForExam ?: return@composable

            TakeQuizScreen(
                user = user,
                quiz = quiz,
                uiState = studentState,
                onSelectAnswer = { qIndex, optIndex ->
                    studentViewModel.selectAnswer(qIndex, optIndex)
                },
                onNextQuestion = { studentViewModel.nextQuestion() },
                onPreviousQuestion = { studentViewModel.previousQuestion() },
                onGoToQuestion = { studentViewModel.goToQuestion(it) },
                onSubmitQuiz = { wasAutoSubmitted ->
                    studentViewModel.submitQuiz(
                        studentId = user.id,
                        studentName = user.name,
                        batchId = user.batchId,
                        wasAutoSubmitted = wasAutoSubmitted
                    )
                },
                onStartBLEVerification = { sessionId ->
                    studentViewModel.startBLEVerification(sessionId)
                },
                onNavigateBack = {
                    studentViewModel.stopBLEVerification()
                    studentViewModel.resetQuizState()
                    navController.popBackStack()
                },
                onUpdateTime = { studentViewModel.updateTimeRemaining(it) }
            )
        }

        composable(Routes.QUIZ_HISTORY) {
            val studentState by studentViewModel.uiState.collectAsState()

            QuizHistoryScreen(
                results = studentState.results,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
