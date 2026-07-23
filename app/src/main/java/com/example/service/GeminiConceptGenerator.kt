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

    suspend fun generateBatchConcepts(
        topics: Set<String>,
        count: Int = 3
    ): List<ConceptItem> = withContext(Dispatchers.IO) {
        val apiKey = prefsManager.getApiKey()
        if (apiKey.isBlank()) {
            Log.w("GeminiGenerator", "No API Key available, generating local fallback concepts.")
            return@withContext getFallbackConcepts(topics, count)
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
            val requestBuilder = Request.Builder()
            
            if (apiKey.startsWith("sk-")) {
                // OpenAI format (auto-detected via sk- prefix)
                val url = "https://api.openai.com/v1/chat/completions"
                val jsonPayload = JSONObject().apply {
                    put("model", "gpt-3.5-turbo") // Default model for OpenAI format
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
                    Log.e("GeminiGenerator", "OpenAI request failed code=${response.code}, falling back.")
                    return@withContext getFallbackConcepts(topics, count)
                }

                val rootJson = JSONObject(responseBody)
                val choices = rootJson.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext getFallbackConcepts(topics, count)
                }
                textResponse = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""

            } else {
                // Gemini format (default for non-sk keys)
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
                
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
                    Log.e("GeminiGenerator", "Gemini request failed code=${response.code}, falling back.")
                    return@withContext getFallbackConcepts(topics, count)
                }

                val rootJson = JSONObject(responseBody)
                val candidates = rootJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext getFallbackConcepts(topics, count)
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

            if (results.isNotEmpty()) results else getFallbackConcepts(topics, count)
        } catch (e: Exception) {
            Log.e("GeminiGenerator", "Error calling Gemini API: ${e.message}", e)
            getFallbackConcepts(topics, count)
        }
    }

    private fun getFallbackConcepts(topics: Set<String>, count: Int): List<ConceptItem> {
        val topicList = if (topics.isEmpty()) listOf("React", "SQL", "DSA", "Python", "Next.js") else topics.toList()
        val seeds = listOf(
            ConceptItem(
                topic = "React",
                conceptTitle = "What is useEffect?",
                conceptSummary = "The `useEffect` hook is an essential part of React's functional component architecture, designed to manage side effects that do not directly relate to the rendering of the UI. When building modern applications, you frequently need to synchronize a component with external systems—this could involve fetching data from a REST API or a GraphQL endpoint, setting up subscriptions or event listeners, or manually interacting with the DOM. Unlike class components where developers used separate lifecycle methods such as `componentDidMount`, `componentDidUpdate`, and `componentWillUnmount` to handle these various stages, `useEffect` consolidates all these capabilities into a single API. \n\nThe hook accepts two arguments: a callback function containing the imperative, potentially effectful code, and an optional dependency array. The dependency array is a crucial mechanism that dictates exactly when the effect should re-run. If you omit the array, the effect runs after every single render, which can lead to performance issues or infinite loops if not handled carefully. If you provide an empty array `[]`, React will only execute the effect once, immediately after the initial render, perfectly mirroring the behavior of `componentDidMount`. If the array contains specific variables, the effect will only re-run if any of those variables change between renders. This granular control allows developers to optimize rendering performance while ensuring the component's state accurately reflects the external data source.",
                codeExample = "useEffect(() => {\n  const timer = setInterval(() => {\n    setCount(c => c + 1);\n  }, 1000);\n  return () => clearInterval(timer);\n}, []);",
                questionType = "MCQ",
                questionText = "When does a useEffect with an empty dependency array [] run?",
                optionsJson = "[\"Only once when component mounts\", \"On every re-render\", \"Only when props change\", \"Never\"]",
                correctAnswer = "0",
                explanation = "An empty dependency array [] tells React to run the effect only once when the component mounts onto the screen."
            ),
            ConceptItem(
                topic = "SQL",
                conceptTitle = "Filtering Records with WHERE",
                conceptSummary = "The `WHERE` clause is a fundamental component of SQL (Structured Query Language), essential for precise data retrieval and manipulation. When dealing with vast databases containing thousands or millions of rows, querying the entire dataset is rarely practical or efficient. The `WHERE` clause acts as a powerful filter, instructing the database engine to isolate and return only the specific rows that satisfy a predefined condition or set of conditions. It is most commonly used in conjunction with `SELECT` statements to extract relevant information, but it is equally critical when executing `UPDATE` or `DELETE` commands to ensure you only modify or remove the intended records, preventing catastrophic data loss.\n\nThe condition specified within the `WHERE` clause evaluates to a boolean value: TRUE, FALSE, or UNKNOWN (in the case of NULLs). SQL provides a rich set of operators to construct these conditions, ranging from simple comparison operators like `=` (equal to), `<>` or `!=` (not equal to), `>`, `<`, `>=`, and `<=`, to more complex logical operators like `AND`, `OR`, and `NOT`, which allow you to combine multiple criteria. You can also utilize specialized operators such as `IN` to match against a list of values, `BETWEEN` to find values within a range, and `LIKE` for pattern matching with wildcards (e.g., finding all names starting with 'A'). By mastering the `WHERE` clause, developers and data analysts can efficiently query subsets of data, ensuring high performance and accurate data analysis.",
                codeExample = "SELECT employee_id, first_name, department\nFROM employees\nWHERE salary > 50000 AND department = 'Engineering';",
                questionType = "CODE",
                questionText = "Complete the SQL query to select all users from 'users' table where age is greater than 21.",
                codeSnippetPrefix = "SELECT * FROM users WHERE ",
                correctAnswer = "age > 21",
                explanation = "Filtering by 'age > 21' ensures only rows matching that boolean expression are returned from the users table."
            ),
            ConceptItem(
                topic = "Python",
                conceptTitle = "List Comprehensions",
                conceptSummary = "List comprehensions represent one of Python's most elegant, expressive, and distinctive features, providing a concise, readable syntax for creating new lists based on the values of existing iterable objects (like lists, tuples, or strings). Traditionally, creating a modified list requires instantiating an empty list, establishing a `for` loop, conditionally evaluating each item, and calling the `.append()` method. List comprehensions condense this entire multi-line process into a single, highly readable line of code.\n\nThe basic syntax follows the pattern: `[expression for item in iterable if condition]`. This structure allows you to simultaneously map (apply a transformation via the expression) and filter (apply a restriction via the condition) the data. Because list comprehensions are optimized at the C level within the standard CPython interpreter, they are generally faster and more memory-efficient than their equivalent `for` loop counterparts. However, developers must exercise caution: while list comprehensions are fantastic for simple to moderately complex transformations, nesting multiple comprehensions or incorporating excessively complex logic can severely degrade code readability. In such cases, falling back to a standard loop or utilizing generator expressions (which evaluate lazily and save memory) is often the more pythonic and maintainable choice.",
                codeExample = "even_squares = [x**2 for x in range(10) if x % 2 == 0]",
                questionType = "MCQ",
                questionText = "What will `[x for x in range(5) if x % 2 == 0]` produce in Python?",
                optionsJson = "[\"[0, 2, 4]\", \"[1, 3, 5]\", \"[0, 1, 2, 3, 4]\", \"[2, 4]\"]",
                correctAnswer = "0",
                explanation = "range(5) evaluates integers 0, 1, 2, 3, 4. The condition `x % 2 == 0` filters even numbers, resulting in [0, 2, 4]."
            ),
            ConceptItem(
                topic = "DSA",
                conceptTitle = "Binary Search Complexity",
                conceptSummary = "Binary Search is a foundational algorithm in computer science, celebrated for its incredible efficiency when searching for a specific target value within a sorted collection, typically an array. Unlike linear search, which must painstakingly iterate through every single element one by one (resulting in a time complexity of O(N)), binary search employs a powerful divide-and-conquer strategy that drastically reduces the search space.\n\nThe algorithm operates by maintaining two pointers—a 'low' and a 'high'—that represent the current bounds of the search space. It calculates the midpoint and compares the element at that index to the target value. If the target matches the midpoint, the search concludes successfully. If the target is smaller, the algorithm intelligently discards the entire upper half of the array by moving the 'high' pointer just below the midpoint. Conversely, if the target is larger, it discards the lower half by moving the 'low' pointer just above the midpoint. This process of halving the search space repeats until the target is found or the pointers cross (indicating the target is not present). Because the dataset is divided by two at each step, the maximum number of iterations required is proportional to the base-2 logarithm of the number of elements. Thus, it achieves a worst-case time complexity of O(log N). To put this immense efficiency into perspective: searching through one million sorted items would require at most 20 comparisons, whereas linear search might require one million.",
                codeExample = "def binary_search(arr, target):\n    low, high = 0, len(arr) - 1\n    while low <= high:\n        mid = (low + high) // 2\n        if arr[mid] == target:\n            return mid\n        elif arr[mid] < target:\n            low = mid + 1\n        else:\n            high = mid - 1\n    return -1",
                questionType = "MCQ",
                questionText = "What is the worst-case time complexity of Binary Search on a sorted array of size N?",
                optionsJson = "[\"O(log N)\", \"O(N)\", \"O(N^2)\", \"O(1)\"]",
                correctAnswer = "0",
                explanation = "By halving the array size at each step, Binary Search completes in at most log2(N) steps."
            ),
            ConceptItem(
                topic = "Next.js",
                conceptTitle = "React Server Components",
                conceptSummary = "React Server Components (RSC) represent a paradigm shift in how modern React applications are architected, moving away from entirely client-side rendering towards a more balanced, server-integrated approach. Introduced fundamentally within the Next.js App Router, Server Components allow developers to natively render React components on the server before any HTML is sent to the browser. This architectural decision brings profound benefits to application performance, security, and developer experience.\n\nThe primary advantage of Server Components is a drastic reduction in the JavaScript payload shipped to the client. Because these components render exclusively on the server, their dependencies (such as large date-formatting libraries or markdown parsers) remain on the server and are never downloaded by the user's browser. This results in faster page loads, quicker Time to Interactive (TTI), and improved SEO. Furthermore, Server Components run in a secure backend environment, allowing developers to directly access databases, internal microservices, and sensitive environment variables (like API keys) without exposing them to the client or requiring the creation of intermediary API routes. While Server Components are the default in the Next.js App Router, developers can still seamlessly interleave 'Client Components' (using the 'use client' directive) for specific UI elements that require interactivity, state management, or access to browser APIs, achieving the best of both worlds.",
                codeExample = "// Next.js Server Component (Default)\nimport db from '@/lib/db'\n\nexport default async function UserProfile({ id }) {\n  // Direct database query on the server!\n  const user = await db.user.findUnique({ where: { id } });\n  return <div>Welcome, {user.name}</div>;\n}",
                questionType = "MCQ",
                questionText = "Where do React Server Components execute in Next.js App Router?",
                optionsJson = "[\"Only on the server\", \"Only in browser\", \"In web worker\", \"On CDN edge only\"]",
                correctAnswer = "0",
                explanation = "Server components render exclusively on the server, producing non-interactive HTML/payload without shipping JS bundle to browser."
            )
        )

        val filtered = seeds.filter { it.topic in topicList }
        val pool = if (filtered.isNotEmpty()) filtered else seeds
        return pool.shuffled().take(count)
    }
}
