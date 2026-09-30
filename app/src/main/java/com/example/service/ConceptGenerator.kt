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

class ConceptGenerator(
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

        val testPrompt = ConceptPrompts.buildConnectionTestPrompt()

        try {
            val configuredModel = prefsManager.getCustomModel().ifBlank { null }
            val requestBuilder = Request.Builder()
            if (apiKey.startsWith("sk-or-")) {
                // OpenRouter endpoint
                val modelName = configuredModel ?: "google/gemini-2.0-flash-001"
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
                val modelName = configuredModel ?: "gemini-1.5-flash"
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"
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
                    .addHeader("x-goog-api-key", apiKey)
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
            Log.w(TAG, "No API Key configured.")
            throw IllegalStateException("API key is empty. Please enter your API key in the 'API & Setup' tab.")
        }

        val selectedTopic = if (topics.isNotEmpty()) topics.toList().random() else "General Knowledge"
        val questionsPerQuiz = prefsManager.getQuestionsPerQuiz()
        val effectiveDifficulty = difficultyOverride ?: prefsManager.getDifficultyLevel()

        // Universal prompt — single source of truth in ConceptPrompts.
        val prompt = ConceptPrompts.buildBatchPrompt(
            topic = selectedTopic,
            count = count,
            questionsPerQuiz = questionsPerQuiz,
            difficulty = effectiveDifficulty,
            focusAreas = focusAreas,
        )

        try {
            var textResponse: String
            val configuredModel = prefsManager.getCustomModel().ifBlank { null }
            val requestBuilder = Request.Builder()
            
            if (apiKey.startsWith("sk-or-")) {
                // OpenRouter endpoint
                val modelName = configuredModel ?: "google/gemini-2.0-flash-001"
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
                    put("max_tokens", 4000)
                }
                
                requestBuilder.url(url)
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("HTTP-Referer", "https://unlocklearn.app")
                    .addHeader("X-Title", "UnlockLearn")
                    
                client.newCall(requestBuilder.build()).execute().use { response ->
                    val responseBody = response.body?.string()

                    if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                        val errDetail = try {
                            val errJson = JSONObject(responseBody ?: "")
                            errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}: ${response.message}"
                        } catch (_: Exception) {
                            "HTTP ${response.code}: ${response.message}"
                        }
                        Log.e(TAG, "OpenRouter request failed code=${response.code}: $errDetail")
                        throw IllegalStateException(errDetail)
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
                    put("max_tokens", 4000)
                }
                
                requestBuilder.url(url)
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $apiKey")
                    
                client.newCall(requestBuilder.build()).execute().use { response ->
                    val responseBody = response.body?.string()

                    if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                        val errDetail = try {
                            val errJson = JSONObject(responseBody ?: "")
                            errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}: ${response.message}"
                        } catch (_: Exception) {
                            "HTTP ${response.code}: ${response.message}"
                        }
                        Log.e(TAG, "OpenAI request failed code=${response.code}: $errDetail")
                        throw IllegalStateException(errDetail)
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
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"
                
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
                        put("maxOutputTokens", 4000)
                    })
                }

                requestBuilder.url(url)
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("x-goog-api-key", apiKey)

                client.newCall(requestBuilder.build()).execute().use { response ->
                    val responseBody = response.body?.string()

                    if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                        val errDetail = try {
                            val errJson = JSONObject(responseBody ?: "")
                            errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}: ${response.message}"
                        } catch (_: Exception) {
                            "HTTP ${response.code}: ${response.message}"
                        }
                        Log.e(TAG, "Gemini request failed code=${response.code}: $errDetail")
                        throw IllegalStateException(errDetail)
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
                if (!isValidGeneratedConcept(item, questionsArray, questionsPerQuiz)) {
                    Log.w(TAG, "Discarded malformed generated concept at index $i")
                    continue
                }
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
            Log.e(TAG, "Error calling AI provider: ${e.message}", e)
            throw e
        }
    }

    private fun isValidGeneratedConcept(
        item: JSONObject,
        questions: JSONArray?,
        expectedQuestionCount: Int,
    ): Boolean {
        if (item.optString("conceptTitle").isBlank()) return false
        if (item.optString("conceptSummary").length < 40) return false
        if (questions == null || questions.length() != expectedQuestionCount) return false

        for (index in 0 until questions.length()) {
            val question = questions.optJSONObject(index) ?: return false
            val type = question.optString("questionType")
            if (type != "MCQ" && type != "TRUE_FALSE") return false
            if (question.optString("questionText").isBlank()) return false
            if (question.optString("explanation").isBlank()) return false

            val options = question.optJSONArray("options") ?: return false
            val expectedOptions = if (type == "TRUE_FALSE") 2 else 4
            if (options.length() != expectedOptions) return false
            val optionValues = buildList {
                for (optionIndex in 0 until options.length()) {
                    val value = options.optString(optionIndex).trim()
                    if (value.isBlank()) return false
                    add(value.lowercase())
                }
            }
            if (optionValues.distinct().size != optionValues.size) return false
            val correctIndex = question.optString("correctAnswer").toIntOrNull() ?: return false
            if (correctIndex !in 0 until options.length()) return false
        }
        return true
    }

    private fun sanitizeAndParseJsonArray(rawText: String): JSONArray {
        // Step 1: Strip special tokens like <unk> or <|endoftext|>
        var trimmed = rawText.replace(Regex("<unk>|<\\|[a-zA-Z0-9_|.-]+>|\\uFFFD"), "").trim()
        
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

        if (jsonString.startsWith("{")) {
            return try {
                JSONArray().put(JSONObject(jsonString))
            } catch (_: Exception) {
                JSONArray(jsonString)
            }
        }

        // Attempt 1: Direct JSON parse
        try {
            return JSONArray(jsonString)
        } catch (_: Exception) {}

        // Attempt 2: Cleaned parse (removing unescaped control chars)
        val cleaned = jsonString.replace(Regex("[\\x00-\\x1F\\x7F]"), " ")
        try {
            return JSONArray(cleaned)
        } catch (_: Exception) {}

        // Attempt 3: Auto-recover complete concept objects from truncated JSON response
        val recoveredArray = JSONArray()
        try {
            val completedObjects = extractCompletedJsonObjects(trimmed)
            for (objStr in completedObjects) {
                try {
                    recoveredArray.put(JSONObject(objStr))
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        if (recoveredArray.length() > 0) {
            Log.i(TAG, "Recovered ${recoveredArray.length()} complete concepts from JSON response")
            return recoveredArray
        }

        // Attempt 4: Auto-repair unclosed strings/objects from truncated output
        try {
            val autoRepaired = autoRepairAndCloseJson(cleaned)
            val repairedArray = JSONArray(autoRepaired)
            if (repairedArray.length() > 0) {
                Log.i(TAG, "Auto-repaired truncated JSON response into ${repairedArray.length()} concepts")
                return repairedArray
            }
        } catch (_: Exception) {}

        // Fallback: throw original parse exception
        return JSONArray(cleaned)
    }

    private fun autoRepairAndCloseJson(raw: String): String {
        val sb = StringBuilder(raw.trim())
        val stack = java.util.ArrayDeque<Char>()
        var inString = false
        var isEscaped = false

        var i = 0
        while (i < sb.length) {
            val char = sb[i]
            if (inString) {
                if (isEscaped) {
                    isEscaped = false
                } else if (char == '\\') {
                    isEscaped = true
                } else if (char == '"') {
                    inString = false
                }
            } else {
                if (char == '"') {
                    inString = true
                } else if (char == '{' || char == '[') {
                    stack.push(char)
                } else if (char == '}' || char == ']') {
                    if (stack.isNotEmpty()) {
                        val top = stack.peek()
                        if ((char == '}' && top == '{') || (char == ']' && top == '[')) {
                            stack.pop()
                        }
                    }
                }
            }
            i++
        }

        if (inString) {
            sb.append('"')
        }

        while (stack.isNotEmpty()) {
            val openChar = stack.pop()
            if (openChar == '{') {
                sb.append('}')
            } else if (openChar == '[') {
                sb.append(']')
            }
        }

        return sb.toString()
    }

    private fun extractCompletedJsonObjects(text: String): List<String> {
        val results = mutableListOf<String>()
        var depth = 0
        var startPos = -1
        var inString = false
        var isEscaped = false

        for (i in text.indices) {
            val char = text[i]
            if (inString) {
                if (isEscaped) {
                    isEscaped = false
                } else if (char == '\\') {
                    isEscaped = true
                } else if (char == '"') {
                    inString = false
                }
            } else {
                if (char == '"') {
                    inString = true
                } else if (char == '{') {
                    if (depth == 0) {
                        startPos = i
                    }
                    depth++
                } else if (char == '}') {
                    depth--
                    if (depth == 0 && startPos != -1) {
                        val candidate = text.substring(startPos, i + 1)
                        results.add(candidate)
                        startPos = -1
                    }
                }
            }
        }
        return results
    }
}

private const val TAG = "ConceptGenerator"

/** Kept for backward compatibility until all external references migrate. */
@Deprecated("Renamed to ConceptGenerator", ReplaceWith("ConceptGenerator"))
typealias GeminiConceptGenerator = ConceptGenerator
