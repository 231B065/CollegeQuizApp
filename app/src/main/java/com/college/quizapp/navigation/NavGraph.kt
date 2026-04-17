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
    const val QUIZ_RESULTS = "quiz_results"
    const val TEACHER_ATTENDANCE = "teacher_attendance"
    const val STUDENT_DASHBOARD = "student_dashboard"
    const val TAKE_QUIZ = "take_quiz"
    const val QUIZ_HISTORY = "quiz_history"
    const val QUIZ_RESULT_DETAIL = "quiz_result_detail"
    const val STUDENT_ATTENDANCE = "student_attendance"
    const val AI_QUIZ_INPUT = "ai_quiz_input"
    const val AI_QUIZ_REVIEW = "ai_quiz_review"
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    teacherViewModel: TeacherViewModel = viewModel(),
    studentViewModel: StudentViewModel = viewModel(),
    aiQuizViewModel: AiQuizViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    var selectedQuizForExam by remember { mutableStateOf<Quiz?>(null) }

    val startDestination = if (authState.isLoggedIn) {
        when (authState.user?.role) {
            UserRole.TEACHER -> Routes.TEACHER_DASHBOARD
            UserRole.STUDENT -> Routes.STUDENT_DASHBOARD
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
                onNavigateToAttendance = {
                    navController.navigate(Routes.TEACHER_ATTENDANCE)
                },
                onNavigateToAiQuiz = {
                    aiQuizViewModel.resetState()
                    navController.navigate(Routes.AI_QUIZ_INPUT)
                },
                onNavigateToQuizDetail = {
                    teacherViewModel.selectQuiz(it)
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
            val state by teacherViewModel.uiState.collectAsState()
            val aiState by aiQuizViewModel.uiState.collectAsState()

            // Get AI-generated questions if coming from AI flow
            val aiQuestions = remember(aiState.generationComplete) {
                if (aiState.generationComplete) {
                    aiQuizViewModel.getSelectedQuestions()
                } else {
                    emptyList()
                }
            }

            CreateQuizScreen(
                user = user,
                batches = state.batches,
                isLoading = state.isLoading,
                onCreateQuiz = { t, d, b, q, dur, s, e ->
                    teacherViewModel.createQuiz(
                        t, d, user.id, user.name, b, q, dur, s, e
                    )
                },
                onNavigateBack = { navController.popBackStack() },
                initialQuestions = aiQuestions
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

        composable(Routes.QUIZ_RESULTS) {
            val state by teacherViewModel.uiState.collectAsState()
            val quiz = state.selectedQuiz ?: return@composable

            QuizResultsScreen(
                quizTitle = quiz.title,
                results = state.quizResults,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TEACHER_ATTENDANCE) {
            val user = authState.user ?: return@composable
            val state by teacherViewModel.uiState.collectAsState()

            TeacherAttendanceScreen(
                user = user,
                batches = state.batches,
                activeSession = state.activeAttendanceSession,
                isAdvertising = state.isAttendanceAdvertising,
                onStartAttendance = { batchId ->
                    teacherViewModel.startAttendanceSession(batchId, user.id, user.name)
                },
                onEndAttendance = {
                    teacherViewModel.endAttendanceSession()
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ================= AI QUIZ GENERATOR =================

        composable(Routes.AI_QUIZ_INPUT) {
            val aiState by aiQuizViewModel.uiState.collectAsState()

            AiQuizInputScreen(
                uiState = aiState,
                onTopicChange = { aiQuizViewModel.updateTopic(it) },
                onDifficultyChange = { aiQuizViewModel.updateDifficulty(it) },
                onQuestionCountChange = { aiQuizViewModel.updateQuestionCount(it) },
                onGenerate = { aiQuizViewModel.generateQuestions() },
                onClearError = { aiQuizViewModel.clearError() },
                onNavigateBack = { navController.popBackStack() }
            )

            // Navigate to review screen when generation completes
            LaunchedEffect(aiState.generationComplete) {
                if (aiState.generationComplete && aiState.generatedQuestions.isNotEmpty()) {
                    navController.navigate(Routes.AI_QUIZ_REVIEW)
                }
            }
        }

        composable(Routes.AI_QUIZ_REVIEW) {
            val aiState by aiQuizViewModel.uiState.collectAsState()

            AiQuizReviewScreen(
                uiState = aiState,
                onToggleQuestion = { aiQuizViewModel.toggleQuestion(it) },
                onSelectAll = { aiQuizViewModel.selectAll() },
                onDeselectAll = { aiQuizViewModel.deselectAll() },
                onRegenerate = {
                    aiQuizViewModel.retry()
                    navController.popBackStack()
                },
                onCreateQuiz = {
                    // Navigate to CreateQuizScreen with selected questions pre-filled
                    navController.navigate(Routes.CREATE_QUIZ) {
                        // Pop back to dashboard so the back stack is clean
                        popUpTo(Routes.TEACHER_DASHBOARD) { inclusive = false }
                    }
                },
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
                onNavigateToAttendance = {
                    navController.navigate(Routes.STUDENT_ATTENDANCE)
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

        composable(Routes.STUDENT_ATTENDANCE) {
            val user = authState.user ?: return@composable
            val state by studentViewModel.uiState.collectAsState()

            StudentAttendanceScreen(
                isDiscovering = state.isAttendanceDiscovering,
                connectionStatus = state.attendanceConnectionStatus,
                markedPresent = state.attendanceMarkedPresent,
                onStartDiscovery = {
                    studentViewModel.startAttendanceDiscovery(user.id, user.name)
                },
                onStopDiscovery = {
                    studentViewModel.stopAttendanceDiscovery()
                },
                onNavigateBack = {
                    studentViewModel.stopAttendanceDiscovery()
                    navController.popBackStack()
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
    }
}