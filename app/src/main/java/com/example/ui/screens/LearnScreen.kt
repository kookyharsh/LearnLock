package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.entity.ConceptItem
import com.example.data.preferences.AppPreferencesManager
import com.example.service.UnlockOverlayService
import com.example.ui.theme.*

@Composable
fun LearnScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val prefsManager = remember { AppPreferencesManager(context) }

    var isServiceEnabled by remember { mutableStateOf(prefsManager.isUnlockServiceEnabled()) }
    val dbConcepts by db.conceptDao().getAllConcepts().collectAsState(initial = emptyList())

    val defaultRecentConcepts = remember {
        listOf(
            ConceptItem(
                id = 1,
                topic = "DSA",
                conceptTitle = "Binary Search Complexity",
                conceptSummary = "Binary Search is a foundational algorithm in computer science, celebrated for its incredible efficiency when searching for a specific target value within a sorted collection, typically an array. Unlike linear search, which must painstakingly iterate through every single element one by one (resulting in a time complexity of O(N)), binary search employs a powerful divide-and-conquer strategy that drastically reduces the search space.\n\nThe algorithm operates by maintaining two pointers—a 'low' and a 'high'—that represent the current bounds of the search space. It calculates the midpoint and compares the element at that index to the target value. If the target matches the midpoint, the search concludes successfully. If the target is smaller, the algorithm intelligently discards the entire upper half of the array by moving the 'high' pointer just below the midpoint.",
                codeExample = "def binary_search(arr, target):\n    low, high = 0, len(arr) - 1\n    while low <= high:\n        mid = (low + high) // 2\n        if arr[mid] == target:\n            return mid\n        elif arr[mid] < target:\n            low = mid + 1\n        else:\n            high = mid - 1\n    return -1",
                questionType = "MCQ",
                questionText = "What is the worst-case time complexity of Binary Search on a sorted array of size N?",
                optionsJson = "[\"O(log N)\", \"O(N)\", \"O(N^2)\", \"O(1)\"]",
                correctAnswer = "0",
                explanation = "Binary Search halves the search space with each iteration, yielding logarithmic O(log N) time complexity."
            ),
            ConceptItem(
                id = 2,
                topic = "React",
                conceptTitle = "What is useEffect?",
                conceptSummary = "The `useEffect` hook is an essential part of React's functional component architecture, designed to manage side effects that do not directly relate to the rendering of the UI. When building modern applications, you frequently need to synchronize a component with external systems—this could involve fetching data from a REST API, setting up subscriptions or event listeners, or manually interacting with the DOM.\n\nUnlike class components where developers used separate lifecycle methods such as `componentDidMount`, `componentDidUpdate`, and `componentWillUnmount` to handle these various stages, `useEffect` consolidates all these capabilities into a single API.",
                codeExample = "useEffect(() => {\n  const timer = setInterval(() => {\n    setCount(c => c + 1);\n  }, 1000);\n  return () => clearInterval(timer);\n}, []);",
                questionType = "MCQ",
                questionText = "When does a useEffect with an empty dependency array [] run?",
                optionsJson = "[\"Only once when component mounts\", \"On every re-render\", \"Only when props change\", \"Never\"]",
                correctAnswer = "0",
                explanation = "An empty dependency array [] ensures the effect runs only once after initial component mount."
            ),
            ConceptItem(
                id = 3,
                topic = "SQL",
                conceptTitle = "Filtering Records with WHERE",
                conceptSummary = "The `WHERE` clause is a fundamental component of SQL (Structured Query Language), essential for precise data retrieval and manipulation. When dealing with vast databases containing thousands or millions of rows, querying the entire dataset is rarely practical or efficient. The `WHERE` clause acts as a powerful filter, instructing the database engine to isolate and return only the specific rows that satisfy a predefined condition or set of conditions.",
                codeExample = "SELECT employee_id, first_name, department\nFROM employees\nWHERE salary > 50000 AND department = 'Engineering';",
                questionType = "CODE",
                questionText = "Complete the SQL query to select all users from 'users' table where age is greater than 21.",
                codeSnippetPrefix = "SELECT * FROM users WHERE ",
                correctAnswer = "age > 21",
                explanation = "The WHERE clause filters rows based on conditional logic such as 'age > 21'."
            ),
            ConceptItem(
                id = 4,
                topic = "Python",
                conceptTitle = "List Comprehensions",
                conceptSummary = "List comprehensions represent one of Python's most elegant, expressive, and distinctive features, providing a concise, readable syntax for creating new lists based on the values of existing iterable objects (like lists, tuples, or strings). Traditionally, creating a modified list requires instantiating an empty list, establishing a `for` loop, conditionally evaluating each item, and calling the `.append()` method. List comprehensions condense this entire multi-line process into a single, highly readable line of code.",
                codeExample = "even_squares = [x**2 for x in range(10) if x % 2 == 0]",
                questionType = "MCQ",
                questionText = "What will `[x for x in range(5) if x % 2 == 0]` produce in Python?",
                optionsJson = "[\"[0, 2, 4]\", \"[1, 3, 5]\", \"[0, 1, 2, 3, 4]\", \"[2, 4]\"]",
                correctAnswer = "0",
                explanation = "range(5) produces 0,1,2,3,4. The condition `x % 2 == 0` filters for even numbers: 0, 2, 4."
            )
        )
    }

    val displayedConcepts = if (dbConcepts.isNotEmpty()) dbConcepts else defaultRecentConcepts
    var expandedConceptIds by remember { mutableStateOf(setOf<Long>()) }

    val windowStart = prefsManager.getLearningWindowStart()
    val windowEnd = prefsManager.getLearningWindowEnd()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "UNLOCK & LEARN",
                            color = ElegantPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Phone Unlock Tutor",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Switch(
                        checked = isServiceEnabled,
                        onCheckedChange = { checked ->
                            isServiceEnabled = checked
                            prefsManager.setUnlockServiceEnabled(checked)
                            val serviceIntent = Intent(context, UnlockOverlayService::class.java)
                            if (checked) {
                                try {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        context.startForegroundService(serviceIntent)
                                    } else {
                                        context.startService(serviceIntent)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("LearnScreen", "Failed to start overlay service", e)
                                }
                            } else {
                                context.stopService(serviceIntent)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElegantOnPrimary,
                            checkedTrackColor = ElegantPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isServiceEnabled)
                        "Active: Learns a concept automatically when unlocking device."
                    else
                        "Paused: Enable to receive questions on screen unlock.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Learning Window Status Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = ElegantPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Active Learning Window",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "$windowStart AM - $windowEnd PM",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Recent Concepts Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = ElegantPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Recent Concepts",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap any card to expand and review detailed explanations and code snippets.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        // Expandable Recent Concept Cards
        displayedConcepts.forEach { concept ->
            val isExpanded = expandedConceptIds.contains(concept.id)

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isExpanded) ElegantPrimary else DarkBorder
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expandedConceptIds = if (isExpanded) {
                            expandedConceptIds - concept.id
                        } else {
                            expandedConceptIds + concept.id
                        }
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Card Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ElegantPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = concept.topic,
                                color = ElegantPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = concept.conceptTitle,
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (!isExpanded) {
                        Text(
                            text = concept.conceptSummary,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Full Detailed Summary
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DarkBackground,
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Book,
                                            contentDescription = null,
                                            tint = ElegantPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Detailed Explanation",
                                            color = ElegantPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = concept.conceptSummary,
                                        color = TextSecondary,
                                        fontSize = 14.sp,
                                        lineHeight = 21.sp
                                    )
                                }
                            }

                            // Code / Text Example snippet if present
                            if (!concept.codeExample.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = DarkBackground,
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Code,
                                                contentDescription = null,
                                                tint = CodeBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Example / Code Snippet",
                                                color = CodeBlue,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = concept.codeExample,
                                            color = CodeBlue,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            lineHeight = 19.sp
                                        )
                                    }
                                }
                            }

                            // Question / Practice preview
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DarkBackground,
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HelpOutline,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Practice Question",
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = concept.questionText,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = concept.explanation,
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

