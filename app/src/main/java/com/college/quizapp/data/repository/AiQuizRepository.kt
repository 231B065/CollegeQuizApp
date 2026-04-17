package com.college.quizapp.data.repository

import com.college.quizapp.BuildConfig
import com.college.quizapp.data.model.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Repository for AI-powered quiz question generation using Groq API.
 * Groq provides a generous free tier with fast inference.
 */
class AiQuizRepository {

    companion object {
        private const val GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"
        private const val MODEL = "llama-3.3-70b-versatile"
    }

    /**
     * Generate MCQ questions using Groq AI.
     *
     * @param topic The subject/topic for question generation
     * @param difficulty Easy, Medium, or Hard
     * @param count Number of questions to generate (5–20)
     * @return Result containing list of Question objects
     */
    suspend fun generateQuestions(
        topic: String,
        difficulty: String,
        count: Int
    ): Result<List<Question>> {
        val apiKey = BuildConfig.GROQ_API_KEY
        if (apiKey.isEmpty()) {
            return Result.failure(
                Exception("Groq API key is not configured. Add GROQ_API_KEY to local.properties.")
            )
        }

        return try {
            val responseText = callGroqApi(apiKey, topic, difficulty, count)
            val questions = parseQuestionsFromResponse(responseText)

            if (questions.isEmpty()) {
                Result.failure(Exception("Could not parse any questions from the AI response. Please try again."))
            } else {
                Result.success(questions)
            }
        } catch (e: Exception) {
            Result.failure(
                Exception("AI generation failed: ${e.message ?: "Unknown error"}. Please try again.")
            )
        }
    }

    /**
     * Call the Groq API to generate quiz questions.
     */
    private suspend fun callGroqApi(
        apiKey: String,
        topic: String,
        difficulty: String,
        count: Int
    ): String = withContext(Dispatchers.IO) {
        val prompt = buildPrompt(topic, difficulty, count)

        val requestBody = JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are an expert quiz generator for college-level education. You always respond with valid JSON arrays only, no markdown, no explanation.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.7)
            put("max_tokens", 4096)
        }

        val url = URL(GROQ_API_URL)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 60000

            // Send request
            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            // Read response
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorStream = connection.errorStream
                val errorBody = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream, "UTF-8")).use { it.readText() }
                } else {
                    "Unknown error"
                }
                throw Exception("API error ($responseCode): $errorBody")
            }

            val responseBody = BufferedReader(
                InputStreamReader(connection.inputStream, "UTF-8")
            ).use { it.readText() }

            // Extract the content from Groq's response
            val jsonResponse = JSONObject(responseBody)
            val choices = jsonResponse.getJSONArray("choices")
            if (choices.length() == 0) {
                throw Exception("AI returned no response")
            }

            choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } finally {
            connection.disconnect()
        }
    }

    private fun buildPrompt(topic: String, difficulty: String, count: Int): String {
        return """Generate exactly $count multiple-choice questions (MCQs) on the topic "$topic" at "$difficulty" difficulty level.

Rules:
1. Each question must have exactly 4 options.
2. Exactly one option must be correct.
3. Questions should be clear, unambiguous, and appropriate for college students.
4. For "Easy" difficulty: test basic concepts and definitions.
5. For "Medium" difficulty: test application and understanding.
6. For "Hard" difficulty: test analysis, edge cases, and deep understanding.
7. Options should be plausible — avoid obviously wrong answers.

IMPORTANT: Respond ONLY with a valid JSON array. No markdown, no explanation, no code fences.

Format:
[
  {
    "question": "What is ...?",
    "options": ["Option A", "Option B", "Option C", "Option D"],
    "correctOptionIndex": 0
  }
]

Where correctOptionIndex is the 0-based index of the correct option (0, 1, 2, or 3).

Generate exactly $count questions now."""
    }

    /**
     * Parse the AI response text into a list of Question objects.
     * Handles various response formats (raw JSON, markdown-wrapped, etc.)
     */
    private fun parseQuestionsFromResponse(responseText: String): List<Question> {
        // Clean up the response — strip markdown code fences if present
        val cleaned = responseText
            .replace("```json", "")
            .replace("```", "")
            .trim()

        // Try to find a JSON array in the response
        val jsonArrayString = extractJsonArray(cleaned)
            ?: return emptyList()

        return try {
            val jsonArray = JSONArray(jsonArrayString)
            val questions = mutableListOf<Question>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val question = parseQuestion(obj)
                if (question != null) {
                    questions.add(question)
                }
            }

            questions
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun extractJsonArray(text: String): String? {
        // Find the first '[' and the last ']'
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start == -1 || end == -1 || end <= start) return null
        return text.substring(start, end + 1)
    }

    private fun parseQuestion(obj: JSONObject): Question? {
        return try {
            val questionText = obj.getString("question")

            val optionsArray = obj.getJSONArray("options")
            val options = mutableListOf<String>()
            for (i in 0 until optionsArray.length()) {
                options.add(optionsArray.getString(i))
            }

            if (options.size != 4) return null

            val correctIndex = when {
                obj.has("correctOptionIndex") -> obj.getInt("correctOptionIndex")
                obj.has("correctAnswer") -> {
                    // Handle case where AI returns "correctAnswer": "Option text"
                    val answer = obj.getString("correctAnswer")
                    val idx = options.indexOf(answer)
                    if (idx >= 0) idx else 0
                }
                else -> 0
            }

            Question(
                type = "MCQ",
                text = questionText,
                options = options,
                correctOptionIndex = correctIndex.coerceIn(0, 3)
            )
        } catch (e: Exception) {
            null
        }
    }
}
