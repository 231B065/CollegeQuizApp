package com.college.quizapp

import android.app.Application

class QuizApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase initializes automatically via google-services plugin
    }
}
