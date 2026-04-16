package com.college.quizapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.college.quizapp.navigation.NavGraph
import com.college.quizapp.ui.theme.CollegeQuizAppTheme
import com.college.quizapp.ui.theme.DarkBackground
import com.college.quizapp.viewmodel.StudentViewModel
import com.college.quizapp.viewmodel.TeacherViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CollegeQuizAppTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val teacherViewModel: TeacherViewModel = viewModel()
                    val studentViewModel: StudentViewModel = viewModel()

                    // Initialize BLE with context
                    teacherViewModel.initBLE(this@MainActivity)
                    studentViewModel.initBLE(this@MainActivity)

                    NavGraph(
                        teacherViewModel = teacherViewModel,
                        studentViewModel = studentViewModel
                    )
                }
            }
        }
    }
}
