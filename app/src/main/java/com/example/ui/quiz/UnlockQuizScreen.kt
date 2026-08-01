package com.example.ui.quiz

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.entity.ConceptItem
import com.example.data.entity.QuestionHistory
import com.example.data.preferences.AppPreferencesManager
import com.example.service.UnlockReceiver
import com.example.ui.components.MarkdownView
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Date

data class QuizQuestion(
    val questionText: String,
    val questionType: String,
    val optionsList: List<String>,
    val codeSnippetPrefix: String?,
    val correctAnswer: String,
    val explanation: String
)

@Composable
fun UnlockQuizScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val prefsManager = remember { AppPreferencesManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var pendingRetryItem by remember { mutableStateOf<QuestionHistory?>(null) }
    var currentConcept by remember { mutableStateOf<ConceptItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var isQuizStarted by remember { mutableStateOf(false) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    
    // Track selected option indices and code answers per question index
    val selectedOptionIndices = remember { mutableStateMapOf<Int, Int>() }
    val codeAnswers = remember { mutableStateMapOf<Int, String>() }

    var showResultDialog by remember { mutableStateOf(false) }
    var totalScore by remember { mutableIntStateOf(0) }
    var totalQuestionsCount by remember { mutableIntStateOf(1) }
    var isPassed by remember { mutableStateOf(false) }
    var resultBreakdownText by remember { mutableStateOf("") }
    var isStarred by remember { mutableStateOf(false) }

    val currentTime = remember {
        DateFormat.format("hh:mm a", Date()).toString()
    }

    LaunchedEffect(Unit) {
        val retryItem = db.historyDao().getPendingRetryQuestion()
        if (retryItem != null) {
            pendingRetryItem = retryItem
            isStarred = retryItem.isStarred
            val originalConcept = db.conceptDao().getConceptByTitle(retryItem.conceptTitle)
            if (originalConcept != null) {
                currentConcept = originalConcept
            }
        } else {
            val selectedTopics = prefsManager.getSelectedTopics().toList()
            val concept = if (selectedTopics.isEmpty()) {
                db.conceptDao().getNextUnusedConcept()
            } else {
                db.conceptDao().getNextUnusedConceptForTopics(selectedTopics)
            } ?: db.conceptDao().getNextUnusedConcept()

            currentConcept = concept
            if (concept != null) {
                isStarred = concept.isStarred
            }
        }
        isLoading = false
        UnlockReceiver.pregenerateConceptsIfNeeded(context, prefsManager)
    }

    val title = pendingRetryItem?.conceptTitle ?: currentConcept?.conceptTitle ?: "CS Concept"

    Scaffold(
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets.statusBars
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ElegantPrimary)
            }
        } else if (pendingRetryItem == null && currentConcept == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No concepts available yet",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Configure your Gemini API key in Settings or generate concepts from the Learn tab to start learning on device unlocks.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onDismiss,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElegantPrimary,
                                contentColor = ElegantOnPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Unlock Device", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            val topic = pendingRetryItem?.topic ?: currentConcept?.topic ?: "General CS"
            val summary = currentConcept?.conceptSummary ?: "Master this fundamental software engineering concept to complete your unlock."
            val codeExample = currentConcept?.codeExample

            val rawQuestionsJson = pendingRetryItem?.questionsJson ?: currentConcept?.questionsJson
            val questionsList = remember(pendingRetryItem, currentConcept) {
                parseQuestionsList(
                    questionsJson = rawQuestionsJson,
                    fallbackQuestionText = pendingRetryItem?.questionText ?: currentConcept?.questionText ?: "Answer the question to proceed.",
                    fallbackQuestionType = pendingRetryItem?.questionType ?: currentConcept?.questionType ?: "MCQ",
                    fallbackOptionsJson = pendingRetryItem?.optionsJson ?: currentConcept?.optionsJson,
                    fallbackCodePrefix = pendingRetryItem?.codeSnippetPrefix ?: currentConcept?.codeSnippetPrefix,
                    fallbackCorrectAnswer = pendingRetryItem?.correctAnswer ?: currentConcept?.correctAnswer ?: "0",
                    fallbackExplanation = pendingRetryItem?.explanation ?: currentConcept?.explanation ?: "Correct answer verified!"
                )
            }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Meta Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Unlock Quiz",
                            tint = ElegantPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "UNLOCK QUIZ • $currentTime",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = TextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                isStarred = !isStarred
                                coroutineScope.launch {
                                    db.conceptDao().updateStarStatusByTitle(title, isStarred)
                                    db.historyDao().updateStarStatusByTitle(title, isStarred)
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isStarred) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Star Concept",
                                tint = if (isStarred) GoldStar else TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = TextMuted
                            )
                        }
                    }
                }

                // AI Tutor Concept Display (Unboxed / Full-width for maximum readability)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(ElegantPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = ElegantOnPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = topic.uppercase(),
                                color = ElegantPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurface
                        ) {
                            Text(
                                text = "AI TUTOR CONCEPT",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    MarkdownView(markdownText = summary)

                    if (codeExample.isValidSnippet()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurface,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = codeExample!!.replace("\\n", "\n"),
                                color = CodeBlue,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isQuizStarted) {
                    Button(
                        onClick = { isQuizStarted = true },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElegantPrimary,
                            contentColor = ElegantOnPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "START QUIZ (${questionsList.size} QUESTIONS)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    val currentQ = questionsList[currentQuestionIndex]
                    val qIndex = currentQuestionIndex
                    val selectedIdx = selectedOptionIndices[qIndex] ?: -1
                    val currentCodeInput = codeAnswers[qIndex] ?: ""
                    val isTextInputQuestion = currentQ.questionType == "CODE" || 
                                              currentQ.questionType.contains("FILL", ignoreCase = true) || 
                                              currentQ.optionsList.isEmpty()

                    // Progress bar & Question Step Badge
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
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
                                        text = "QUESTION ${qIndex + 1} OF ${questionsList.size}",
                                        color = ElegantPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = "${qIndex + 1}/${questionsList.size}",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { (qIndex + 1).toFloat() / questionsList.size },
                                color = ElegantPrimary,
                                trackColor = DarkBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Question Text
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(ElegantPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "0${qIndex + 1}",
                                        color = ElegantOnPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = currentQ.questionText,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Render choices (CODE / FILL_BLANK vs MCQ / TRUE_FALSE)
                            if (isTextInputQuestion) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DarkBackground,
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (currentQ.questionType == "CODE") "-- Fill in code answer" else "-- Type your answer",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                if (currentQ.codeSnippetPrefix.isValidSnippet()) {
                                    Text(
                                        text = currentQ.codeSnippetPrefix!!.replace("\\n", "\n"),
                                        color = CodeBlue,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                OutlinedTextField(
                                    value = currentCodeInput,
                                    onValueChange = { codeAnswers[qIndex] = it },
                                    placeholder = { Text("type answer here...", color = TextMuted, fontSize = 13.sp) },
                                    textStyle = LocalTextStyle.current.copy(
                                        color = TextPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElegantPrimary,
                                        unfocusedBorderColor = DarkBorder,
                                        focusedContainerColor = DarkSurface,
                                        unfocusedContainerColor = DarkSurface
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (currentQ.codeSnippetPrefix.isValidSnippet()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = DarkBackground,
                                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = currentQ.codeSnippetPrefix!!.replace("\\n", "\n"),
                                        color = CodeBlue,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            currentQ.optionsList.forEachIndexed { index, optionText ->
                                val isSelected = selectedIdx == index
                                val borderColor = if (isSelected) ElegantPrimary else DarkBorder
                                val bgColor = if (isSelected) ElegantPrimaryContainer.copy(alpha = 0.3f) else DarkSurface

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = bgColor,
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(borderColor)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { selectedOptionIndices[qIndex] = index }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .border(2.dp, borderColor, CircleShape)
                                                .background(if (isSelected) ElegantPrimary else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = optionText,
                                            color = if (isSelected) TextPrimary else TextSecondary,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val isCurrentAnswered = if (isTextInputQuestion) {
                currentCodeInput.isNotBlank()
            } else {
                selectedIdx != -1
            }

                    if (qIndex < questionsList.size - 1) {
                        Button(
                            onClick = { currentQuestionIndex++ },
                            enabled = isCurrentAnswered,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElegantPrimary,
                                contentColor = ElegantOnPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                text = "NEXT QUESTION (${qIndex + 1}/${questionsList.size})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                        }
                    } else {
                        // Submit All Quiz Questions
                        Button(
                            onClick = {
                                var correctCount = 0
                                val breakdownBuilder = StringBuilder()

                                questionsList.forEachIndexed { i, q ->
                                    val userSelIdx = selectedOptionIndices[i] ?: -1
                                    val userCode = codeAnswers[i] ?: ""
                                    val isTextQ = q.questionType == "CODE" || q.questionType.contains("FILL", ignoreCase = true) || q.optionsList.isEmpty()
                                    
                                    val isQCorrect = if (isTextQ) {
                                        userCode.trim().equals(q.correctAnswer.trim(), ignoreCase = true)
                                    } else {
                                        val letter = when (userSelIdx) {
                                            0 -> "A"; 1 -> "B"; 2 -> "C"; 3 -> "D"; else -> ""
                                        }
                                        letter.equals(q.correctAnswer.trim(), ignoreCase = true) ||
                                                userSelIdx.toString() == q.correctAnswer.trim()
                                    }

                                    if (isQCorrect) correctCount++

                                    val savedUserAns = if (isTextQ) userCode.trim() else q.optionsList.getOrNull(userSelIdx) ?: userSelIdx.toString()
                                    breakdownBuilder.append("Q${i + 1}: ${if (isQCorrect) "✅ Correct" else "❌ Wrong"}\n")
                                    breakdownBuilder.append("   Your: $savedUserAns\n")
                                    if (!isQCorrect) {
                                        breakdownBuilder.append("   Explanation: ${q.explanation}\n")
                                    }
                                    breakdownBuilder.append("\n")
                                }

                                totalScore = correctCount
                                totalQuestionsCount = questionsList.size
                                val passed = if (questionsList.size > 1) correctCount >= 2 else correctCount >= 1
                                isPassed = passed
                                resultBreakdownText = breakdownBuilder.toString().trim()
                                showResultDialog = true

                                coroutineScope.launch {
                                    val userAnswersList = mutableListOf<String>()
                                    questionsList.forEachIndexed { i, q ->
                                        val selIdx = selectedOptionIndices[i] ?: -1
                                        val code = codeAnswers[i] ?: ""
                                        val isTextQ = q.questionType == "CODE" || q.questionType.contains("FILL", ignoreCase = true) || q.optionsList.isEmpty()
                                        val ans = if (isTextQ) code.trim() else q.optionsList.getOrNull(selIdx) ?: if (selIdx != -1) selIdx.toString() else "Unanswered"
                                        userAnswersList.add(ans)
                                    }

                                    val formattedUserAnswer = if (userAnswersList.size == 1) {
                                        userAnswersList.first()
                                    } else {
                                        userAnswersList.mapIndexed { idx, a -> "Q${idx + 1}: $a" }.joinToString(" | ")
                                    }

                                    val firstQ = questionsList.first()
                                    val newStatus = if (passed) "PASSED" else "RETRY_PENDING"

                                    // Always insert a new QuestionHistory entry so recent attempts show at top of History
                                    db.historyDao().insertHistory(
                                        QuestionHistory(
                                            id = 0,
                                            conceptTitle = title,
                                            topic = topic,
                                            questionText = if (questionsList.size > 1) "${questionsList.size}-Question Quiz ($correctCount/${questionsList.size} Correct)" else firstQ.questionText,
                                            userAnswer = formattedUserAnswer,
                                            correctAnswer = firstQ.correctAnswer,
                                            isCorrect = passed,
                                            status = newStatus,
                                            explanation = firstQ.explanation,
                                            optionsJson = JSONArray(firstQ.optionsList).toString(),
                                            questionType = firstQ.questionType,
                                            codeSnippetPrefix = firstQ.codeSnippetPrefix,
                                            questionsJson = rawQuestionsJson,
                                            conceptSummary = summary,
                                            isStarred = isStarred,
                                            answeredAt = System.currentTimeMillis()
                                        )
                                    )

                                    val currentItem = pendingRetryItem
                                    if (currentItem != null) {
                                        if (passed) {
                                            db.historyDao().markConceptPassed(currentItem.conceptTitle)
                                        } else {
                                            db.historyDao().updateHistory(currentItem.copy(status = "RETRY_RESOLVED"))
                                        }
                                    }

                                    val concept = currentConcept
                                    if (concept != null) {
                                        db.conceptDao().markConceptUsed(concept.id)
                                    }
                                    if (passed) {
                                        db.historyDao().markConceptPassed(title)
                                    }
                                }
                            },
                            enabled = isCurrentAnswered,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElegantPrimary,
                                contentColor = ElegantOnPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Submit",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SUBMIT QUIZ & UNLOCK",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Answer Result Dialog (Popup)
            if (showResultDialog) {
                AlertDialog(
                    onDismissRequest = { /* Modal forces choice */ },
                    containerColor = DarkSurface,
                    shape = RoundedCornerShape(28.dp),
                    title = {
                        Surface(
                            color = if (isPassed) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isPassed) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isPassed) SuccessGreen else ErrorRed
                                    )
                                    Text(
                                        text = if (isPassed) "Quiz Passed ($totalScore/$totalQuestionsCount)" else "Quiz Failed ($totalScore/$totalQuestionsCount)",
                                        color = if (isPassed) SuccessGreen else ErrorRed,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        isStarred = !isStarred
                                        val conceptTitle = pendingRetryItem?.conceptTitle ?: currentConcept?.conceptTitle ?: "CS Concept"
                                        coroutineScope.launch {
                                            db.conceptDao().updateStarStatusByTitle(conceptTitle, isStarred)
                                            db.historyDao().updateStarStatusByTitle(conceptTitle, isStarred)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isStarred) Icons.Default.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Star Concept",
                                        tint = if (isStarred) GoldStar else TextMuted
                                    )
                                }
                            }
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (isPassed) "Great job! You answered $totalScore out of $totalQuestionsCount correctly and unlocked your phone." else "You scored $totalScore out of $totalQuestionsCount. Retry now or practice again later?",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DarkBackground,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = resultBreakdownText,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        if (isPassed) {
                            Button(
                                onClick = onDismiss,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary)
                            ) {
                                Text("Dismiss & Return to Phone", color = ElegantOnPrimary, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    currentQuestionIndex = 0
                                    selectedOptionIndices.clear()
                                    codeAnswers.clear()
                                    showResultDialog = false
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Quiz Now", color = ElegantOnPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        if (!isPassed) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = CircleShape,
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Try Later", color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
            }
        }
    }
}

fun String?.isValidSnippet(): Boolean {
    if (this == null) return false
    val trimmed = this.trim()
    return trimmed.isNotEmpty() && !trimmed.equals("null", ignoreCase = true)
}

fun parseQuestionsList(
    questionsJson: String?,
    fallbackQuestionText: String,
    fallbackQuestionType: String,
    fallbackOptionsJson: String?,
    fallbackCodePrefix: String?,
    fallbackCorrectAnswer: String,
    fallbackExplanation: String
): List<QuizQuestion> {
    val cleanFallbackPrefix = fallbackCodePrefix?.takeIf { it.isValidSnippet() }
    if (!questionsJson.isNullOrBlank()) {
        try {
            val array = JSONArray(questionsJson)
            val list = mutableListOf<QuizQuestion>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val qText = obj.optString("questionText", obj.optString("question", fallbackQuestionText))
                val qType = obj.optString("questionType", fallbackQuestionType)
                val optArray = obj.optJSONArray("options")
                val opts = mutableListOf<String>()
                if (optArray != null) {
                    for (j in 0 until optArray.length()) {
                        opts.add(optArray.getString(j))
                    }
                } else if (qType == "TRUE_FALSE") {
                    opts.addAll(listOf("True", "False"))
                }
                val rawCodePref = if (obj.isNull("codeSnippetPrefix")) null else obj.getString("codeSnippetPrefix")
                val codePref = rawCodePref?.takeIf { it.isValidSnippet() }
                val cAns = obj.optString("correctAnswer", "0")
                val exp = obj.optString("explanation", fallbackExplanation)
                list.add(QuizQuestion(qText, qType, opts, codePref, cAns, exp))
            }
            if (list.isNotEmpty()) return list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    return listOf(
        QuizQuestion(
            questionText = fallbackQuestionText,
            questionType = fallbackQuestionType,
            optionsList = parseOptionsJson(fallbackOptionsJson),
            codeSnippetPrefix = cleanFallbackPrefix,
            correctAnswer = fallbackCorrectAnswer,
            explanation = fallbackExplanation
        )
    )
}

private fun parseOptionsJson(jsonStr: String?): List<String> {
    if (jsonStr.isNullOrBlank()) {
        return listOf("Option A", "Option B", "Option C", "Option D")
    }
    return try {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        if (list.size >= 2) list else listOf("Option A", "Option B", "Option C", "Option D")
    } catch (_: Exception) {
        listOf("Option A", "Option B", "Option C", "Option D")
    }
}

