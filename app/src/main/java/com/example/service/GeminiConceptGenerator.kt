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
                val modelName = configuredModel ?: "gemini-1.5-flash"
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

        val topicList = if (topics.isEmpty()) listOf("React", "SQL", "DSA", "Python") else topics.toList()
        val selectedTopic = topicList.random()

        val prompt = """
            You are a general knowledge and learning tutor.
            Generate $count unique concepts and quiz questions for the topic '$selectedTopic'.
            For each concept:
            1. Provide a short concept title.
            2. Provide a detailed concept Explanation (200-300 words). Explain what it is, why it is important, where it is used, and provide a concrete example.
            3. Provide a brief code or text example snippet if applicable.
            4. Provide either an "MCQ" (4 options) or "CODE" (fill-in-the-blank or text completion query) question to test understanding.
            5. Provide the exact correctAnswer ("0", "1", "2", "3" for MCQ index, or fill-in string for CODE).
            6. Provide a pregenerated explanation detailing why the answer is correct and common pitfalls.

            Return ONLY a valid JSON array matching this structure:
            [
              {
                "topic": "$selectedTopic",
                "conceptTitle": "Title",
                "conceptSummary": "Detailed summary...",
                "codeExample": "example sample or null",
                "questionType": "MCQ",
                "questionText": "Question text",
                "options": ["Option A", "Option B", "Option C", "Option D"],
                "codeSnippetPrefix": null,
                "correctAnswer": "0",
                "explanation": "Explanation text..."
              },
              {
                "topic": "$selectedTopic",
                "conceptTitle": "Title",
                "conceptSummary": "Detailed summary...",
                "codeExample": "example sample or null",
                "questionType": "CODE",
                "questionText": "Question text",
                "options": null,
                "codeSnippetPrefix": "Fill in: ",
                "correctAnswer": "answer",
                "explanation": "Explanation text..."
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
                val optionsJsonArray = item.optJSONArray("options")
                val optionsString = optionsJsonArray?.toString()

                results.add(
                    ConceptItem(
                        topic = item.optString("topic", selectedTopic),
                        conceptTitle = item.optString("conceptTitle", "CS Concept"),
                        conceptSummary = item.optString("conceptSummary", "Concept explanation..."),
                        codeExample = item.optString("codeExample").takeIf { it.isNotBlank() && it != "null" },
                        questionType = item.optString("questionType", "MCQ"),
                        questionText = item.optString("questionText", "What is the answer?"),
                        optionsJson = optionsString,
                        codeSnippetPrefix = item.optString("codeSnippetPrefix").takeIf { it.isNotBlank() && it != "null" },
                        correctAnswer = item.optString("correctAnswer", "0"),
                        explanation = item.optString("explanation", "Great job! This is the correct concept."),
                        isUsed = false
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
            return listOf(
                ConceptItem(
                    topic = "Data Structures",
                    conceptTitle = "Hash Maps & O(1) Lookups",
                    conceptSummary = "Hash maps map unique keys to values using a hashing function. In average cases, inserting, searching, and deleting keys takes O(1) constant time.",
                    codeExample = "val map = HashMap<String, Int>()\nmap[\"Alice\"] = 95\nval score = map[\"Alice\"] // O(1) average lookup",
                    questionType = "MCQ",
                    questionText = "What is the average time complexity for key lookups in a Hash Map?",
                    optionsJson = "[\"O(1)\", \"O(log n)\", \"O(n)\", \"O(n²)\"]",
                    correctAnswer = "0",
                    explanation = "Hash Maps calculate array indices directly using a hash function, resulting in average O(1) constant time lookups."
                ),
                ConceptItem(
                    topic = "Algorithms",
                    conceptTitle = "Binary Search Algorithm",
                    conceptSummary = "Binary Search locates a target value in a sorted array by repeatedly dividing the search range in half, achieving logarithmic O(log n) efficiency.",
                    codeExample = "fun binarySearch(arr: IntArray, target: Int): Int {\n    var low = 0; var high = arr.size - 1\n    while (low <= high) {\n        val mid = (low + high) / 2\n        if (arr[mid] == target) return mid\n        if (arr[mid] < target) low = mid + 1 else high = mid - 1\n    }\n    return -1\n}",
                    questionType = "MCQ",
                    questionText = "What prerequisite MUST an array satisfy for Binary Search to work?",
                    optionsJson = "[\"The array must be sorted\", \"Elements must all be positive\", \"The size must be even\", \"It must fit in RAM\"]",
                    correctAnswer = "0",
                    explanation = "Binary Search depends on comparing the middle element to narrow down the remaining half, which requires a sorted array."
                ),
                ConceptItem(
                    topic = "System Design",
                    conceptTitle = "Caching & LRU Eviction Policy",
                    conceptSummary = "Caches store frequent data in fast memory. A Least Recently Used (LRU) policy automatically evicts the entry that hasn't been accessed for the longest time.",
                    codeExample = "val cache = LinkedHashMap<Int, String>(capacity, 0.75f, accessOrder = true)",
                    questionType = "MCQ",
                    questionText = "Which item is discarded when an LRU Cache reaches full capacity?",
                    optionsJson = "[\"The item least recently accessed\", \"The newest item added\", \"The largest file in memory\", \"A randomly chosen key\"]",
                    correctAnswer = "0",
                    explanation = "LRU stands for Least Recently Used; it removes the oldest unaccessed item to free up memory space."
                )
            )
        }
    }
}
