# 📚 College Quiz App

A comprehensive Android application for college classrooms that enables teachers to create and manage quizzes, take smart attendance, and leverage AI for automatic question generation — all in one platform.

Built with **Kotlin**, **Jetpack Compose**, and **Firebase**.

---

## ✨ Features

### 👨‍🏫 For Teachers

| Feature | Description |
|---------|-------------|
| **Quiz Management** | Create quizzes with MCQ and subjective questions, set schedules, assign to batches |
| **AI Quiz Generator** | Automatically generate MCQs using AI — enter a topic, difficulty, and count, then review and select questions |
| **Batch Management** | Create and manage student batches (e.g., CSE-2024-A) |
| **Smart Attendance** | Take real-time proximity-based attendance using Google Nearby Connections |
| **BLE Exam Proctoring** | Broadcast BLE beacons to verify student physical presence during exams |
| **Quiz Results** | View detailed per-student quiz results with scores and submissions |
| **AI Answer Evaluation** | Automatic AI-powered evaluation of subjective answers with feedback |

### 👩‍🎓 For Students

| Feature | Description |
|---------|-------------|
| **Take Quizzes** | Attempt quizzes with timed sessions, question navigation, and auto-submit |
| **BLE Verification** | Verify physical presence in classroom via BLE beacon detection |
| **Smart Attendance** | Mark attendance automatically via proximity to teacher's device |
| **Quiz History** | View past quiz attempts with scores and detailed results |
| **AI Feedback** | Receive AI-generated feedback on subjective answers |

---

## 🏗️ Architecture

```
com.college.quizapp/
├── data/
│   ├── model/              # Data classes (Quiz, Question, User, Batch, etc.)
│   └── repository/         # Firebase & AI API interactions
│       ├── QuizRepository.kt
│       ├── AuthRepository.kt
│       ├── AttendanceRepository.kt
│       └── AiQuizRepository.kt
├── viewmodel/              # ViewModels with StateFlow
│   ├── AuthViewModel.kt
│   ├── TeacherViewModel.kt
│   ├── StudentViewModel.kt
│   └── AiQuizViewModel.kt
├── ui/
│   ├── auth/               # Login & Register screens
│   ├── teacher/            # Teacher dashboard, quiz creation, AI generator, attendance
│   ├── student/            # Student dashboard, quiz taking, history, attendance
│   └── theme/              # Color palette, typography, Material 3 theme
├── navigation/             # Jetpack Navigation with type-safe routes
├── ble/                    # BLE Beacon advertiser & scanner
├── nearby/                 # Google Nearby Connections for attendance
├── MainActivity.kt
└── QuizApplication.kt
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin |
| **UI** | Jetpack Compose + Material 3 |
| **Navigation** | Jetpack Navigation Compose |
| **Backend** | Firebase (Auth + Firestore) |
| **AI** | Groq API (LLaMA 3.3 70B) for quiz generation, Gemini for answer evaluation |
| **Proximity** | Google Nearby Connections API |
| **BLE** | Android BLE Advertiser & Scanner |
| **Async** | Kotlin Coroutines + StateFlow |
| **Architecture** | MVVM (Model-View-ViewModel) |

---

## 📱 Screens

### Authentication
- **Login** — Email/password sign-in
- **Register** — Sign up as Teacher or Student, with batch selection for students

### Teacher Flow
- **Dashboard** — Overview stats, quiz list, quick actions
- **Create Quiz** — Manual quiz creation with MCQ & subjective questions
- **AI Quiz Generator** — Enter topic → generate with AI → review & select → create quiz
- **Manage Batches** — Create and view student batches
- **Quiz Detail** — View quiz info, toggle active/inactive, BLE broadcasting
- **Quiz Results** — Per-student scores and submissions
- **Smart Attendance** — Start proximity-based attendance sessions

### Student Flow
- **Dashboard** — Available quizzes, quick actions
- **Take Quiz** — Timed exams with question navigation and BLE verification
- **Quiz History** — Past attempts with scores
- **Quiz Result Detail** — Detailed review with AI feedback
- **Smart Attendance** — Discover teacher and mark presence

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug (2024.2.1) or later
- **JDK 17**
- **Android SDK 35**
- **Firebase project** with Auth and Firestore enabled
- **Groq API key** (free) — for AI quiz generation

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/CollegeQuizApp.git
   cd CollegeQuizApp
   ```

2. **Firebase Setup**
   - Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
   - Enable **Email/Password Authentication**
   - Enable **Cloud Firestore**
   - Download `google-services.json` and place it in `app/`

3. **API Keys**
   
   Add your API keys to `local.properties`:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key_here
   GROQ_API_KEY=your_groq_api_key_here
   ```
   
   - **Gemini API key**: Get from [aistudio.google.com/apikey](https://aistudio.google.com/apikey) (used for subjective answer evaluation)
   - **Groq API key**: Get from [console.groq.com](https://console.groq.com) (used for AI quiz generation — free tier: 30 RPM, 14,400 RPD)

4. **Build & Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or open in Android Studio and click **Run**.

---

## 🤖 AI Quiz Generator

The AI quiz generator allows teachers to automatically create MCQs:

1. **Input** — Enter topic (e.g., "Operating Systems"), select difficulty (Easy/Medium/Hard), choose question count (5–20)
2. **Generate** — AI generates high-quality MCQs via Groq's LLaMA 3.3 70B model
3. **Review** — Browse generated questions, toggle correct answer visibility
4. **Select** — Check/uncheck individual questions, select all / deselect all
5. **Create** — Create quiz with selected questions pre-filled into the standard quiz creation form

---

## 📡 Smart Attendance System

Proximity-based attendance using **Google Nearby Connections**:

- **Teacher** starts an attendance session → device begins advertising
- **Student** opens attendance screen → device discovers nearby teacher
- Automatic P2P connection → student identity sent → teacher marks present
- Real-time updates via Firestore

---

## 🔒 BLE Exam Proctoring

Anti-cheating mechanism using **Bluetooth Low Energy**:

- Teacher's device broadcasts a BLE beacon with a unique session UUID
- Students must detect the beacon to start taking the quiz
- Ensures students are physically present in the classroom

---

## 📂 Firestore Data Model

```
├── users/
│   └── {userId}
│       ├── email, name, role (TEACHER/STUDENT), batchId
│
├── batches/
│   └── {batchId}
│       ├── name, createdBy, studentCount
│
├── quizzes/
│   └── {quizId}
│       ├── title, description, createdBy, teacherName
│       ├── batchIds[], questions[]
│       ├── durationMinutes, startTime, endTime
│       ├── bleSessionId, isActive
│
├── quizResults/
│   └── {resultId}
│       ├── quizId, studentId, studentName, batchId
│       ├── answers{}, score, totalQuestions
│       ├── submittedAt, wasAutoSubmitted
│       └── aiFeedback{}
│
└── attendanceSessions/
    └── {sessionId}
        ├── batchId, teacherId, date, active
        └── presentStudents{}
```

---

## 🔑 Permissions

| Permission | Purpose |
|-----------|---------|
| `INTERNET` | Firebase & AI API communication |
| `BLUETOOTH_*` | BLE beacon for exam proctoring |
| `ACCESS_FINE_LOCATION` | Required for BLE scanning |
| `NEARBY_WIFI_DEVICES` | Google Nearby Connections for attendance |

---

## 📄 License

This project is for educational purposes.

---

## 👨‍💻 Author

**Arpan Pandey**

Built as a college project to modernize classroom quiz management and attendance tracking with AI and Bluetooth technology.
