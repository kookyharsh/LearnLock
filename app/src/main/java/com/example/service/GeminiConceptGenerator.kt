package com.example.service

import android.util.Log
import com.example.data.entity.ConceptItem
import com.example.data.preferences.AppPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiConceptGenerator(
    private val prefsManager: AppPreferencesManager,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun testApiKeyConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val apiKey = prefsManager.getApiKey().trim()
        if (apiKey.isBlank()) {
            return@withContext Pair(false, "API Key is empty. Please enter your key.")
        }

        val testPrompt = "Return JSON array: [{\"topic\": \"Test\", \"conceptTitle\": \"Test\", \"conceptSummary\": \"Test summary\", \"codeExample\": null, \"questionType\": \"MCQ\", \"questionText\": \"Test?\", \"options\": [\"A\", \"B\", \"C\", \"D\"], \"codeSnippetPrefix\": null, \"correctAnswer\": \"0\", \"explanation\": \"Test explanation\"}]"

        try {
            val configuredModel = prefsManager.getCustomModel().ifBlank { null }
            val requestBuilder = Request.Builder()
            if (apiKey.startsWith("sk-or-")) {
                // OpenRouter endpoint
                val modelName = configuredModel ?: "google/gemini-2.5-flash"
                val jsonPayload = JSONObject().apply {
                    put("model", modelName)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", testPrompt)
                        })
                    })
                    put("temperature", 0.7)
                }
                requestBuilder.url("https://openrouter.ai/api/v1/chat/completions")
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("HTTP-Referer", "https://unlocklearn.app")
                    .addHeader("X-Title", "UnlockLearn")
            } else if (apiKey.startsWith("sk-")) {
                // OpenAI endpoint
                val modelName = configuredModel ?: "gpt-4o-mini"
                val jsonPayload = JSONObject().apply {
                    put("model", modelName)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", testPrompt)
                        })
                    })
                    put("temperature", 0.7)
                }
                requestBuilder.url("https://api.openai.com/v1/chat/completions")
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $apiKey")
            } else {
                // Google Gemini endpoint
                val modelName = configuredModel ?: "gemini-3.5-flash"
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val jsonPayload = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", testPrompt)
                                })
                            })
                        })
                    })
                }
                requestBuilder.url(url)
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    val errDetail = try {
                        val errJson = JSONObject(responseBody ?: "")
                        errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                    } catch (e: Exception) {
                        "HTTP ${response.code}: ${response.message}"
                    }
                    return@withContext Pair(false, "Connection failed: $errDetail")
                }

                if (responseBody.isNullOrBlank()) {
                    return@withContext Pair(false, "Received empty response from provider.")
                }

                return@withContext Pair(true, "API Key Verified & Connected Successfully!")
            }
        } catch (e: Exception) {
            return@withContext Pair(false, "Network error: ${e.message}")
        }
    }

    suspend fun generateBatchConcepts(
        topics: Set<String>,
        count: Int = 3,
        difficultyOverride: String? = null,
        focusAreas: List<String> = emptyList()
    ): List<ConceptItem> = withContext(Dispatchers.IO) {
        val apiKey = prefsManager.getApiKey().trim()
        if (apiKey.isBlank()) {
            Log.w("GeminiGenerator", "No API Key configured.")
            return@withContext emptyList()
        }

        if (topics.isEmpty()) {
            Log.w("GeminiGenerator", "No topics selected by user.")
            return@withContext emptyList()
        }

        val selectedTopic = topics.toList().random()
        val questionsPerQuiz = prefsManager.getQuestionsPerQuiz()
        val effectiveDifficulty = difficultyOverride ?: prefsManager.getDifficultyLevel()
        val focusAreasLine = if (focusAreas.isNotEmpty()) {
            "The learner has struggled with these concepts recently; prefer generating concepts closely related to them: ${focusAreas.take(5).joinToString(", ")}."
        } else {
            "Prefer generating a balanced mix of foundational and practical concepts."
        }

        val prompt = """
            You are an expert tutor in '$selectedTopic'.
            Target Difficulty Level: '$effectiveDifficulty' (Adapt depth and question difficulty to '$effectiveDifficulty').
            $focusAreasLine
            Generate $count unique concepts for the topic '$selectedTopic'.
            Each concept must include a structured, easy-to-read explanation and an array of EXACTLY $questionsPerQuiz quiz questions.

            CRITICAL RULE FOR QUESTIONS:
            - ALL $questionsPerQuiz questions MUST be directly answerable using ONLY the facts and concepts explicitly taught in the 'conceptSummary' (or 'codeExample' / 'codeSnippetPrefix') for that card.
            - Do NOT ask outside trivia or details that are not explicitly covered in the concept summary text!

            Rules:
            1. conceptTitle: Short, clear concept title.
            2. conceptSummary: Highly structured 50-100 words explanation using markdown (`**bold**`, bullet points `- `, and double line breaks `\n\n`).
               CRITICAL FORMATTING (DO NOT RETURN A WALL-OF-TEXT PARAGRAPH):
               - 1-sentence core definition at the top.
               - 2-3 bullet points (`- **Point**: detail`) breaking down key mechanics/properties.
               - 1-sentence quick takeaway or real-world example at the bottom.
            3. codeExample: A short example, formula, SQL query, code snippet, or illustration relevant to '$selectedTopic' (or null if not needed). If code, format with clean line breaks (`\n`).
            4. questions: Array of EXACTLY $questionsPerQuiz questions. Allowed question types: randomly mix "MCQ" or "TRUE_FALSE".
               - For MCQ: Provide 4 distinct choices in "options", set "correctAnswer" to index "0", "1", "2", or "3".
               - For TRUE_FALSE: "options" = ["True", "False"], set "correctAnswer" to "0" or "1".
               - codeSnippetPrefix: Optional short 1-3 line text excerpt, formula, code, or context for the question (or null).

            Return ONLY a valid JSON array matching this structure:
            [
              {
                "topic": "$selectedTopic",
                "conceptTitle": "Title",
                "conceptSummary": "Core definition here...\n\n- **Key Point 1**: Detail 1\n- **Key Point 2**: Detail 2\n\nTakeaway example...",
                "codeExample": null,
                "difficulty": "$effectiveDifficulty",
                "questions": [
                  {
                    "questionType": "MCQ",
                    "questionText": "Question 1 text...",
                    "options": ["Option A", "Option B", "Option C", "Option D"],
                    "codeSnippetPrefix": null,
                    "correctAnswer": "0",
                    "explanation": "Why Option A is correct based on the summary."
                  }
                ]
              }
            ]
        """.trimIndent()

        try {
            var textResponse: String
            val configuredModel = prefsManager.getCustomModel().ifBlank { null }
            val requestBuilder = Request.Builder()
            
            if (apiKey.startsWith("sk-or-")) {
                // OpenRouter endpoint
                val modelName = configuredModel ?: "google/gemini-2.5-flash"
                val url = "https://openrouter.ai/api/v1/chat/completions"
                val jsonPayload = JSONObject().apply {
                    put("model", modelName)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                    put("temperature", 0.7)
                }
                
                requestBuilder.url(url)
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("HTTP-Referer", "https://unlocklearn.app")
                    .addHeader("X-Title", "UnlockLearn")
                    
                client.newCall(requestBuilder.build()).execute().use { response ->
                    val responseBody = response.body?.string()

                    if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                        Log.e("GeminiGenerator", "OpenRouter request failed code=${response.code}, body=$responseBody")
                        return@withContext emptyList()
                    }

                    val rootJson = JSONObject(responseBody)
                    val choices = rootJson.optJSONArray("choices")
                    if (choices == null || (choices.length() == 0)) {
                        return@withContext emptyList()
                    }
                    textResponse = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""
                }

            } else if (apiKey.startsWith("sk-")) {
                // OpenAI format
                val modelName = configuredModel ?: "gpt-4o-mini"
                val url = "https://api.openai.com/v1/chat/completions"
                val jsonPayload = JSONObject().apply {
                    put("model", modelName)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                    put("temperature", 0.7)
                }
                
                requestBuilder.url(url)
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $apiKey")
                    
                client.newCall(requestBuilder.build()).execute().use { response ->
                    val responseBody = response.body?.string()

                    if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                        Log.e("GeminiGenerator", "OpenAI request failed code=${response.code}, body=$responseBody")
                        return@withContext emptyList()
                    }

                    val rootJson = JSONObject(responseBody)
                    val choices = rootJson.optJSONArray("choices")
                    if (choices == null || (choices.length() == 0)) {
                        return@withContext emptyList()
                    }
                    textResponse = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""
                }

            } else {
                // Gemini format (default for non-sk keys)
                val modelName = configuredModel ?: "gemini-1.5-flash"
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                
                val jsonPayload = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("responseMimeType", "application/json")
                    })
                }

                requestBuilder.url(url)
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))

                client.newCall(requestBuilder.build()).execute().use { response ->
                    val responseBody = response.body?.string()

                    if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                        Log.e("GeminiGenerator", "Gemini request failed code=${response.code}, body=$responseBody")
                        return@withContext emptyList()
                    }

                    val rootJson = JSONObject(responseBody)
                    val candidates = rootJson.optJSONArray("candidates")
                    if (candidates == null || candidates.length() == 0) {
                        return@withContext emptyList()
                    }

                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    textResponse = parts?.getJSONObject(0)?.optString("text") ?: ""
                }
            }

            val jsonArray = sanitizeAndParseJsonArray(textResponse)
            val results = mutableListOf<ConceptItem>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val questionsArray = item.optJSONArray("questions")
                val questionsStr = questionsArray?.toString()

                val firstQ = if (questionsArray != null && questionsArray.length() > 0) {
                    questionsArray.getJSONObject(0)
                } else item

                val optionsJsonArray = firstQ.optJSONArray("options") ?: item.optJSONArray("options")
                val optionsString = optionsJsonArray?.toString()

                results.add(
                    ConceptItem(
                        topic = item.optString("topic", selectedTopic),
                        conceptTitle = item.optString("conceptTitle", "$selectedTopic Concept"),
                        conceptSummary = item.optString("conceptSummary", "Concept explanation..."),
                        codeExample = item.optString("codeExample").takeIf { it.isNotBlank() && it != "null" },
                        questionType = firstQ.optString("questionType", item.optString("questionType", "MCQ")),
                        questionText = firstQ.optString("questionText", item.optString("questionText", "Answer to complete unlock.")),
                        optionsJson = optionsString,
                        codeSnippetPrefix = firstQ.optString("codeSnippetPrefix").takeIf { it.isNotBlank() && it != "null" }
                            ?: item.optString("codeSnippetPrefix").takeIf { it.isNotBlank() && it != "null" },
                        correctAnswer = firstQ.optString("correctAnswer", item.optString("correctAnswer", "0")),
                        explanation = firstQ.optString("explanation", item.optString("explanation", "Correct answer verified!")),
                        isUsed = false,
                        questionsJson = questionsStr,
                        difficulty = item.optString("difficulty", effectiveDifficulty)
                    )
                )
            }

            results
        } catch (e: Exception) {
            Log.e("GeminiGenerator", "Error calling Gemini API: ${e.message}", e)
            emptyList()
        }
    }

    private fun sanitizeAndParseJsonArray(rawText: String): JSONArray {
        var trimmed = rawText.trim()
        
        // Strip markdown code fences if present
        if (trimmed.startsWith("```")) {
            val firstLineEnd = trimmed.indexOf('\n')
            if (firstLineEnd != -1) {
                trimmed = trimmed.substring(firstLineEnd + 1)
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length - 3)
            }
            trimmed = trimmed.trim()
        }

        // Extract exact JSON array bounds between first '[' and last ']'
        val startIdx = trimmed.indexOf('[')
        val endIdx = trimmed.lastIndexOf(']')
        var jsonString = if (startIdx != -1 && endIdx > startIdx) {
            trimmed.substring(startIdx, endIdx + 1)
        } else {
            trimmed
        }

        // Repair common LLM JSON syntax anomalies:
        // 1. Keys with leading spaces like " "conceptTitle": -> "conceptTitle":
        jsonString = jsonString.replace(Regex("\"\\s+([a-zA-Z0-9_]+)\"\\s*:"), "\"$1\":")
        // 2. Trailing commas before closing brackets or braces
        jsonString = jsonString.replace(Regex(",\\s*([\\]}])"), "$1")

        return try {
            JSONArray(jsonString)
        } catch (e: Exception) {
            Log.w("GeminiGenerator", "Initial JSON parse failed (${e.message}). Attempting fallback cleanup...", e)
            val cleaned = jsonString.replace(Regex("[\\x00-\\x1F\\x7F]"), " ")
            JSONArray(cleaned)
        }
    }

    companion object {
    }
}
