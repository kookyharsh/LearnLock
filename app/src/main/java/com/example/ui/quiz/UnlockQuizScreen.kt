package com.example.ui.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import com.example.data.AppDatabase
import com.example.data.entity.ConceptItem
import com.example.data.entity.QuestionHistory
import com.example.data.preferences.AppPreferencesManager
import com.example.service.GeminiConceptGenerator
import com.example.service.UnlockReceiver
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockQuizScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val prefsManager = remember { AppPreferencesManager(context) }

    var currentConcept by remember { mutableStateOf<ConceptItem?>(null) }
    var pendingRetryItem by remember { mutableStateOf<QuestionHistory?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedOptionIndex by remember { mutableIntStateOf(-1) }
    var codeAnswerText by remember { mutableStateOf("") }

    var isQuizStarted by remember { mutableStateOf(false) }

    var showResultDialog by remember { mutableStateOf(false) }
    var isAnswerCorrect by remember { mutableStateOf(false) }
    var explanationText by remember { mutableStateOf("") }

    val currentTime = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    // Load next question on launch
    LaunchedEffect(Unit) {
        val pending = db.historyDao().getPendingRetryQuestion()
        if (pending != null) {
            pendingRetryItem = pending
            val concept = db.conceptDao().getConceptByTitle(pending.conceptTitle)
            if (concept != null) {
                currentConcept = concept
            }
        } else {
            val selectedTopics = prefsManager.getSelectedTopics().toList()
            var nextConcept = if (selectedTopics.isNotEmpty()) {
                db.conceptDao().getNextUnusedConceptForTopics(selectedTopics)
            } else {
                db.conceptDao().getNextUnusedConcept()
            }



            currentConcept = nextConcept
        }
        isLoading = false

        // Trigger background refill if queue is running low
        UnlockReceiver.pregenerateConceptsIfNeeded(context, prefsManager)
    }

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
            // Empty State when no concepts exist in database
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
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Meta Header
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

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = TextMuted
                        )
                    }
                }

                val title = pendingRetryItem?.conceptTitle ?: currentConcept?.conceptTitle ?: "CS Concept"
                val topic = pendingRetryItem?.topic ?: currentConcept?.topic ?: "General CS"
                val summary = currentConcept?.conceptSummary ?: "Master this fundamental software engineering concept to complete your unlock."
                val codeExample = currentConcept?.codeExample
                val questionText = pendingRetryItem?.questionText ?: currentConcept?.questionText ?: "Answer the question to proceed."
                val questionType = pendingRetryItem?.questionType ?: currentConcept?.questionType ?: "MCQ"
                val optionsJson = pendingRetryItem?.optionsJson ?: currentConcept?.optionsJson
                val codePrefix = pendingRetryItem?.codeSnippetPrefix ?: currentConcept?.codeSnippetPrefix
                val expectedAnswer = pendingRetryItem?.correctAnswer ?: currentConcept?.correctAnswer ?: "0"
                val rawExplanation = pendingRetryItem?.explanation ?: currentConcept?.explanation ?: "Correct answer verified!"

                // Concept Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CURRENT CONCEPT",
                                    color = ElegantPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = title,
                                    color = TextPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ElegantPrimaryContainer,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = topic,
                                    color = ElegantOnPrimaryContainer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Detailed Concept Explanation Container
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DarkBackground,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = parseMarkdownToAnnotatedString(summary),
                                    color = TextSecondary,
                                    fontSize = 15.sp,
                                    lineHeight = 23.sp
                                )
                            }
                        }

                        if (!codeExample.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DarkBackground,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = codeExample,
                                    color = CodeBlue,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(visible = !isQuizStarted) {
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
                            text = "START QUIZ",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                AnimatedVisibility(visible = isQuizStarted) {
                    Column {
                        // Question Section
                        Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
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
                                    text = "01",
                                    color = ElegantOnPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = questionText,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Render question controls (MCQ or CODE)
                        if (questionType == "CODE") {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = DarkBackground,
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "-- Fill in code answer",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (!codePrefix.isNullOrBlank()) {
                                        Text(
                                            text = codePrefix,
                                            color = CodeBlue,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = codeAnswerText,
                                        onValueChange = { codeAnswerText = it },
                                        placeholder = { Text("condition / code expression...", color = TextMuted, fontSize = 13.sp) },
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
                            // Parse MCQ Options
                            val optionsList = remember(optionsJson) {
                                parseOptionsJson(optionsJson)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                optionsList.forEachIndexed { index, optionText ->
                                    val isSelected = selectedOptionIndex == index
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
                                            .clickable { selectedOptionIndex = index }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .border(2.dp, borderColor, CircleShape)
                                                    .padding(3.dp)
                                            ) {
                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(ElegantPrimary, CircleShape)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = optionText,
                                                color = TextPrimary,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Validated",
                                tint = ElegantPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AI VALIDATED",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Action Button
                Button(
                    onClick = {
                        val isCorrect: Boolean
                        val userAnswerStr: String

                        if (questionType == "CODE") {
                            userAnswerStr = codeAnswerText.trim()
                            isCorrect = userAnswerStr.equals(expectedAnswer.trim(), ignoreCase = true) ||
                                    (expectedAnswer.isNotBlank() && userAnswerStr.contains(expectedAnswer.trim(), ignoreCase = true))
                        } else {
                            userAnswerStr = selectedOptionIndex.toString()
                            isCorrect = selectedOptionIndex.toString() == expectedAnswer.trim()
                        }

                        isAnswerCorrect = isCorrect
                        explanationText = rawExplanation

                        coroutineScope.launch {
                            if (pendingRetryItem != null) {
                                val updated = pendingRetryItem!!.copy(
                                    userAnswer = userAnswerStr,
                                    isCorrect = isCorrect,
                                    status = if (isCorrect) "PASSED" else "RETRY_PENDING",
                                    answeredAt = System.currentTimeMillis()
                                )
                                db.historyDao().updateHistory(updated)
                            } else if (currentConcept != null) {
                                val history = QuestionHistory(
                                    conceptTitle = title,
                                    topic = topic,
                                    questionText = questionText,
                                    userAnswer = userAnswerStr,
                                    correctAnswer = expectedAnswer,
                                    isCorrect = isCorrect,
                                    status = if (isCorrect) "PASSED" else "RETRY_PENDING",
                                    explanation = rawExplanation,
                                    optionsJson = optionsJson,
                                    questionType = questionType,
                                    codeSnippetPrefix = codePrefix
                                )
                                db.historyDao().insertHistory(history)
                                db.conceptDao().markConceptUsed(currentConcept!!.id)
                            }
                        }

                        showResultDialog = true
                    },
                    enabled = (questionType == "MCQ" && selectedOptionIndex != -1) ||
                            (questionType == "CODE" && codeAnswerText.isNotBlank()),
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
                        text = "SUBMIT ANSWER",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                    }
                }
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
                        color = if (isAnswerCorrect) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isAnswerCorrect) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isAnswerCorrect) SuccessGreen else ErrorRed
                            )
                            Text(
                                text = if (isAnswerCorrect) "Correct Answer!" else "Wrong Answer",
                                color = if (isAnswerCorrect) SuccessGreen else ErrorRed,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isAnswerCorrect) "Great job! You have unlocked your phone." else "Retry now or try again later?",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isAnswerCorrect) explanationText else "If you choose to try later, this question will appear again the next time you unlock your device to help reinforce the concept.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                },
                confirmButton = {
                    if (isAnswerCorrect) {
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
                                selectedOptionIndex = -1
                                codeAnswerText = ""
                                showResultDialog = false
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Now", color = ElegantOnPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    if (!isAnswerCorrect) {
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
    } catch (e: Exception) {
        listOf("Option A", "Option B", "Option C", "Option D")
    }
}

private fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            when {
                text.startsWith("**", index) && text.indexOf("**", index + 2) != -1 -> {
                    val end = text.indexOf("**", index + 2)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(text.substring(index + 2, end))
                    pop()
                    index = end + 2
                }
                text.startsWith("__", index) && text.indexOf("__", index + 2) != -1 -> {
                    val end = text.indexOf("__", index + 2)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(text.substring(index + 2, end))
                    pop()
                    index = end + 2
                }
                text.startsWith("<u>", index) && text.indexOf("</u>", index + 3) != -1 -> {
                    val end = text.indexOf("</u>", index + 3)
                    pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    append(text.substring(index + 3, end))
                    pop()
                    index = end + 4
                }
                text.startsWith("*", index) && text.indexOf("*", index + 1) != -1 -> {
                    val end = text.indexOf("*", index + 1)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(text.substring(index + 1, end))
                    pop()
                    index = end + 1
                }
                text.startsWith("_", index) && text.indexOf("_", index + 1) != -1 -> {
                    val end = text.indexOf("_", index + 1)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(text.substring(index + 1, end))
                    pop()
                    index = end + 1
                }
                else -> {
                    append(text[index])
                    index++
                }
            }
        }
    }
}

