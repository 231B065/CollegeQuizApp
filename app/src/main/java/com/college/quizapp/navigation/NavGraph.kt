package com.college.quizapp.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.college.quizapp.data.model.Quiz
import com.college.quizapp.data.model.UserRole
import com.college.quizapp.ui.auth.*
import com.college.quizapp.ui.student.*
import com.college.quizapp.ui.teacher.*
import com.college.quizapp.viewmodel.*

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val TEACHER_DASHBOARD = "teacher_dashboard"
    const val CREATE_QUIZ = "create_quiz"
    const val MANAGE_BATCHES = "manage_batches"
    const val QUIZ_DETAIL = "quiz_detail"
    const val STUDENT_REQUESTS = "student_requests"
    const val QUIZ_RESULTS = "quiz_results"
    const val STUDENT_DASHBOARD = "student_dashboard"
    const val TAKE_QUIZ = "take_quiz"
    const val QUIZ_HISTORY = "quiz_history"
    const val QUIZ_RESULT_DETAIL = "quiz_result_detail"
    const val PENDING_APPROVAL = "pending_approval"
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

    val startDestination = if (authState.isLoggedIn) {
        when (authState.user?.role) {
            UserRole.TEACHER -> Routes.TEACHER_DASHBOARD
            UserRole.STUDENT ->
                if (authState.user?.isApproved == true)
                    Routes.STUDENT_DASHBOARD
                else Routes.PENDING_APPROVAL
            else -> Routes.LOGIN
        }
    } else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {

        // ================= AUTH =================

        composable(Routes.LOGIN) {
            LoginScreen(
                uiState = authState,
                onSignIn = { e, p -> authViewModel.signIn(e, p) },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onClearError = { authViewModel.clearError() }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                uiState = authState,
                onSignUp = { e, p, n, r, b ->
                    authViewModel.signUp(e, p, n, r, b)
                },
                onNavigateToLogin = { navController.popBackStack() },
                onLoadBatches = { authViewModel.loadBatches() },
                onClearError = { authViewModel.clearError() }
            )
        }

        // ================= TEACHER =================

        composable(Routes.TEACHER_DASHBOARD) {
            val user = authState.user ?: return@composable
            val state by teacherViewModel.uiState.collectAsState()

            LaunchedEffect(user.id) {
                teacherViewModel.loadQuizzes(user.id)
                teacherViewModel.loadBatches(user.id)
                teacherViewModel.loadPendingStudents()
            }

            TeacherDashboardScreen(
                user = user,
                uiState = state,
                onNavigateToCreateQuiz = {
                    navController.navigate(Routes.CREATE_QUIZ)
                },
                onNavigateToManageBatches = {
                    navController.navigate(Routes.MANAGE_BATCHES)
                },
                onNavigateToQuizDetail = {
                    teacherViewModel.selectQuiz(it)
                    navController.navigate(Routes.QUIZ_DETAIL)
                },
                onNavigateToRequests = {
                    navController.navigate(Routes.STUDENT_REQUESTS)
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
            val state by teacherViewModel.uiState.collectAsState()

            CreateQuizScreen(
                user = user,
                batches = state.batches,
                isLoading = state.isLoading,
                onCreateQuiz = { t, d, b, q, dur, s, e ->
                    teacherViewModel.createQuiz(
                        t, d, user.id, user.name, b, q, dur, s, e
                    )
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MANAGE_BATCHES) {
            val user = authState.user ?: return@composable
            val state by teacherViewModel.uiState.collectAsState()

            ManageBatchScreen(
                batches = state.batches,
                isLoading = state.isLoading,
                onCreateBatch = { teacherViewModel.createBatch(it, user.id) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.QUIZ_DETAIL) {
            val state by teacherViewModel.uiState.collectAsState()
            val quiz = state.selectedQuiz ?: return@composable

            QuizDetailScreen(
                quiz = quiz,
                results = state.quizResults,
                isAdvertising = state.isAdvertising,
                onToggleActive = { teacherViewModel.toggleQuizActive(quiz) },
                onViewResults = {
                    navController.navigate(Routes.QUIZ_RESULTS)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.STUDENT_REQUESTS) {
            val state by teacherViewModel.uiState.collectAsState()

            StudentRequestsScreen(
                pendingStudents = state.pendingStudents,
                isLoading = state.isLoading,
                successMessage = state.successMessage,
                onLoadRequests = { teacherViewModel.loadPendingStudents() },
                onApproveStudent = { id, batch ->
                    teacherViewModel.approveStudent(id, batch)
                },
                onNavigateBack = { navController.popBackStack() },
                onClearSuccess = { teacherViewModel.clearSuccess() }
            )
        }

        composable(Routes.QUIZ_RESULTS) {
            val state by teacherViewModel.uiState.collectAsState()
            val quiz = state.selectedQuiz ?: return@composable

            QuizResultsScreen(
                quizTitle = quiz.title,
                results = state.quizResults,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ================= STUDENT =================

        composable(Routes.STUDENT_DASHBOARD) {
            val user = authState.user ?: return@composable
            val state by studentViewModel.uiState.collectAsState()

            LaunchedEffect(user.batchId) {
                studentViewModel.loadQuizzes(user.batchId)
                studentViewModel.loadResults(user.id)
            }

            StudentDashboardScreen(
                user = user,
                uiState = state,
                onNavigateToQuiz = {
                    selectedQuizForExam = it
                    studentViewModel.startQuiz(it)
                    navController.navigate(Routes.TAKE_QUIZ)
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.QUIZ_HISTORY)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.TAKE_QUIZ) {
            val user = authState.user ?: return@composable
            val state by studentViewModel.uiState.collectAsState()
            val quiz = selectedQuizForExam ?: return@composable

            TakeQuizScreen(
                user = user,
                quiz = quiz,
                uiState = state,
                onSelectAnswer = { q, o ->
                    studentViewModel.selectAnswer(q, o)
                },
                onNextQuestion = { studentViewModel.nextQuestion() },
                onPreviousQuestion = { studentViewModel.previousQuestion() },
                onGoToQuestion = { studentViewModel.goToQuestion(it) },
                onSubmitQuiz = {
                    studentViewModel.submitQuiz(
                        user.id, user.name, user.batchId, it
                    )
                },
                onStartBLEVerification = { sessionId -> // ✅ FIX
                    studentViewModel.startBLEVerification(sessionId)
                },
                onNavigateBack = {
                    studentViewModel.resetQuizState()
                    navController.popBackStack()
                },
                onUpdateTime = { studentViewModel.updateTimeRemaining(it) },
                onNavigateToDetails = {
                    navController.navigate(Routes.QUIZ_RESULT_DETAIL)
                }
            )
        }

        composable(Routes.QUIZ_HISTORY) {
            val state by studentViewModel.uiState.collectAsState()

            QuizHistoryScreen(
                results = state.results,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = {
                    studentViewModel.selectResultForDetails(it)
                    navController.navigate(Routes.QUIZ_RESULT_DETAIL)
                }
            )
        }

        composable(Routes.QUIZ_RESULT_DETAIL) {
            val state by studentViewModel.uiState.collectAsState()
            val result = state.selectedResult ?: return@composable

            QuizResultDetailScreen(
                result = result,
                quiz = state.currentQuiz,
                isLoading = state.isLoading,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PENDING_APPROVAL) {
            val user = authState.user ?: return@composable

            PendingApprovalScreen(
                user = user,
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onRefresh = { authViewModel.refreshUser() }
            )
        }
    }
}