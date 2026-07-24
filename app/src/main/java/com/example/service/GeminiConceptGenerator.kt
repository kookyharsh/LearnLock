package com.example.service

import android.content.Context
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
    private val context: Context,
    private val prefsManager: AppPreferencesManager
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
            val response = if (apiKey.startsWith("sk-or-")) {
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
                    .build()
                client.newCall(requestBuilder.build()).execute()
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
                    .build()
                client.newCall(requestBuilder.build()).execute()
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
                    .build()
                client.newCall(requestBuilder.build()).execute()
            }

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
        } catch (e: Exception) {
            return@withContext Pair(false, "Network error: ${e.message}")
        }
    }

    suspend fun generateBatchConcepts(
        topics: Set<String>,
        count: Int = 3
    ): List<ConceptItem> = withContext(Dispatchers.IO) {
        val apiKey = prefsManager.getApiKey().trim()
        if (apiKey.isBlank()) {
            Log.w("GeminiGenerator", "No API Key configured.")
            return@withContext emptyList<ConceptItem>()
        }

        if (topics.isEmpty()) {
            Log.w("GeminiGenerator", "No topics selected by user.")
            return@withContext emptyList<ConceptItem>()
        }

        val selectedTopic = topics.toList().random()

        val isTechnicalTopic = selectedTopic.contains(
            Regex("(?i)code|coding|python|java|javascript|kotlin|react|sql|dsa|algorithm|data structure|system design|android|dev|programming|web|css|html|git|backend|frontend|computer science")
        )

        val personaInstruction = if (isTechnicalTopic) {
            "You are an expert Computer Science and Software Engineering tutor."
        } else {
            "You are an expert tutor specializing exclusively in '$selectedTopic'."
        }

        val subjectRules = if (isTechnicalTopic) {
            """
            - You may generate code examples and use questionType "MCQ" or "CODE" or "FILL IN THE BLANKS".
            """.trimIndent()
        } else {
            """
            - CRITICAL: Focus EXCLUSIVELY on '$selectedTopic'.
            - Set questionType STRICTLY to "MCQ". Do NOT use "CODE" or fill-in-the-blank programming questions.
            - Set codeExample and codeSnippetPrefix to null.
            - For options (4 choices), provide 4 distinct, unambiguous choices. If asking about symbols or punctuation, render the actual symbols or clear choice text directly in the choices.
            """.trimIndent()
        }

        val prompt = """
            $personaInstruction
            Generate $count unique concepts for the topic '$selectedTopic'. For each concept, provide a concise explanation and exactly 3 distinct questions.

            Rules:
            1. Provide a short, relevant concept title.
            2. Provide a detailed concept explanation (strictly 50-100 words) formatted in markdown (`**bold**`, `*italic*`, `<u>underline</u>`, code backticks where relevant). Explain what it is, why it matters, and a quick real-world example. Make it read like an engaging AI tutor message!
            3. Provide an array "questions" containing EXACLTY 3 distinct questions for this concept (e.g. MCQ, True/False, Fill in the Blank, or Code).
               - For MCQ: provide 4 distinct choices in "options" array, and set "correctAnswer" to option index ("0", "1", "2", or "3").
               - For TRUE_FALSE: set "options" to ["True", "False"], and set "correctAnswer" to "0" or "1".
               - For FILL_BLANK: set "questionType" to "FILL_BLANK", provide expected word in "correctAnswer".
               - For CODE: set "questionType" to "CODE", provide prefix in "codeSnippetPrefix", expected answer in "correctAnswer".
            $subjectRules

            Return ONLY a valid JSON array matching this structure:
            [
              {
                "topic": "$selectedTopic",
                "conceptTitle": "Title",
                "conceptSummary": "Detailed summary (50-100 words with **markdown**)...",
                "codeExample": null,
                "questions": [
                  {
                    "questionType": "MCQ",
                    "questionText": "Question 1 text...",
                    "options": ["Option A", "Option B", "Option C", "Option D"],
                    "codeSnippetPrefix": null,
                    "correctAnswer": "0",
                    "explanation": "Why answer 0 is correct..."
                  },
                  {
                    "questionType": "TRUE_FALSE",
                    "questionText": "Question 2 text...",
                    "options": ["True", "False"],
                    "codeSnippetPrefix": null,
                    "correctAnswer": "0",
                    "explanation": "Why True is correct..."
                  },
                  {
                    "questionType": "MCQ",
                    "questionText": "Question 3 text...",
                    "options": ["Option A", "Option B", "Option C", "Option D"],
                    "codeSnippetPrefix": null,
                    "correctAnswer": "2",
                    "explanation": "Why Option C is correct..."
                  }
                ]
              }
            ]
        """.trimIndent()

        try {
            var textResponse = ""
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
                    
                val response = client.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                    Log.e("GeminiGenerator", "OpenRouter request failed code=${response.code}, body=$responseBody")
                    return@withContext emptyList<ConceptItem>()
                }

                val rootJson = JSONObject(responseBody)
                val choices = rootJson.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext emptyList<ConceptItem>()
                }
                textResponse = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""

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
                    
                val response = client.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                    Log.e("GeminiGenerator", "OpenAI request failed code=${response.code}, body=$responseBody")
                    return@withContext emptyList<ConceptItem>()
                }

                val rootJson = JSONObject(responseBody)
                val choices = rootJson.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext emptyList<ConceptItem>()
                }
                textResponse = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""

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

                val response = client.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                    Log.e("GeminiGenerator", "Gemini request failed code=${response.code}, body=$responseBody")
                    return@withContext emptyList<ConceptItem>()
                }

                val rootJson = JSONObject(responseBody)
                val candidates = rootJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext emptyList<ConceptItem>()
                }

                val content = candidates.getJSONObject(0).optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                textResponse = parts?.getJSONObject(0)?.optString("text") ?: ""
            }

            val cleanedJsonText = textResponse.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonArray = JSONArray(cleanedJsonText)
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
                        questionsJson = questionsStr
                    )
                )
            }

            results
        } catch (e: Exception) {
            Log.e("GeminiGenerator", "Error calling Gemini API: ${e.message}", e)
            emptyList<ConceptItem>()
        }
    }

    companion object {
        fun getDefaultConcepts(): List<ConceptItem> {
            return emptyList()
        }
    }
}
