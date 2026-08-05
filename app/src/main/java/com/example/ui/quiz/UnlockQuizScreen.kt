package com.example.ui.quiz

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.data.resolveWeakestTopic
import com.example.data.scheduler.AdaptiveScheduler
import com.example.service.UnlockReceiver
import com.example.ui.components.MarkdownView
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

data class QuizQuestion(
    val questionText: String,
    val questionType: String,
    val optionsList: List<String>,
    val codeSnippetPrefix: String?,
    val correctAnswer: String,
    val explanation: String
)

private fun difficultyColor(difficulty: String?): Color {
    return when (difficulty) {
        "Easy" -> SuccessGreen
        "Hard" -> ErrorRed
        else -> GoldStar
    }
}

data class QuizResult(
    val passed: Boolean,
    val correctCount: Int,
    val total: Int,
    val masteryBefore: Double?,
    val masteryAfter: Double?,
    val nextReviewDays: Int?,
    val srsStatus: String?
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

    var isStarred by remember { mutableStateOf(false) }

    var quizResult by remember { mutableStateOf<QuizResult?>(null) }

    val currentTime = remember {
        DateFormat.format("hh:mm a", Date()).toString()
    }

    LaunchedEffect(Unit) {
        try {
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
                val cooldown24h = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                val recentTitles = db.historyDao().getRecentConceptTitles(cooldown24h)
                val targetCount = prefsManager.getQuestionsPerQuiz()
                val now = System.currentTimeMillis()

                // 1) Spaced repetition: surface due reviews first
                var concept = if (selectedTopics.isEmpty()) {
                    db.conceptDao().getDueReviews(now, 1).firstOrNull()
                } else {
                    db.conceptDao().getDueReviewsForTopics(selectedTopics, now, 1).firstOrNull()
                }

                // 2) Fresh concept from the weakest topic
                if (concept == null) {
                    val weakestTopic = resolveWeakestTopic(db, selectedTopics)
                    if (weakestTopic != null) {
                        concept = db.conceptDao()
                            .getNextUnusedConceptForTopicExcludingRecent(weakestTopic, recentTitles)
                            ?: db.conceptDao().getNextUnusedConceptForTopic(weakestTopic)
                    }
                }

                // 3) Fallback: existing random selection
                if (concept == null) {
                    concept = if (selectedTopics.isEmpty()) {
                        db.conceptDao().getNextUnusedConceptExcludingRecent(recentTitles)
                    } else {
                        db.conceptDao().getNextUnusedConceptForTopicsExcludingRecent(selectedTopics, recentTitles)
                    } ?: if (selectedTopics.isEmpty()) {
                        db.conceptDao().getNextUnusedConcept()
                    } else {
                        db.conceptDao().getNextUnusedConceptForTopics(selectedTopics)
                    }
                }

                // Verify fetched concept matches current user question count preference
                if (concept != null) {
                    val parsedQuestions = parseQuestionsList(
                        questionsJson = concept.questionsJson,
                        fallbackQuestionText = concept.questionText,
                        fallbackQuestionType = concept.questionType,
                        fallbackOptionsJson = concept.optionsJson,
                        fallbackCodePrefix = concept.codeSnippetPrefix,
                        fallbackCorrectAnswer = concept.correctAnswer,
                        fallbackExplanation = concept.explanation
                    )
                    if (parsedQuestions.size != targetCount) {
                        db.conceptDao().markConceptUsed(concept.id)
                        concept = null
                    }
                }

                // If no pre-generated concept matches, generate a fresh AI concept
                if (concept == null && prefsManager.getApiKey().isNotBlank()) {
                    val generator = com.example.service.GeminiConceptGenerator(prefsManager)
                    val fresh = generator.generateBatchConcepts(
                        topics = prefsManager.getSelectedTopics(),
                        count = 1
                    )
                    if (fresh.isNotEmpty()) {
                        val newId = db.conceptDao().insertConcept(fresh.first())
                        concept = fresh.first().copy(id = newId)
                    }
                }

                currentConcept = concept
                if (concept != null) {
                    isStarred = concept.isStarred
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
        UnlockReceiver.pregenerateConceptsIfNeeded(context, prefsManager)
    }

    val title = pendingRetryItem?.conceptTitle ?: currentConcept?.conceptTitle ?: "CS Concept"

    Scaffold(
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets.statusBars
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
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
            val summary = currentConcept?.conceptSummary
                ?: "Master this fundamental software engineering concept to complete your unlock."
            val codeExample = currentConcept?.codeExample

            val rawQuestionsJson = pendingRetryItem?.questionsJson ?: currentConcept?.questionsJson
            val questionsList = remember(pendingRetryItem, currentConcept) {
                parseQuestionsList(
                    questionsJson = rawQuestionsJson,
                    fallbackQuestionText = pendingRetryItem?.questionText
                        ?: currentConcept?.questionText ?: "Answer the question to proceed.",
                    fallbackQuestionType = pendingRetryItem?.questionType
                        ?: currentConcept?.questionType ?: "MCQ",
                    fallbackOptionsJson = pendingRetryItem?.optionsJson
                        ?: currentConcept?.optionsJson,
                    fallbackCodePrefix = pendingRetryItem?.codeSnippetPrefix
                        ?: currentConcept?.codeSnippetPrefix,
                    fallbackCorrectAnswer = pendingRetryItem?.correctAnswer
                        ?: currentConcept?.correctAnswer ?: "0",
                    fallbackExplanation = pendingRetryItem?.explanation
                        ?: currentConcept?.explanation ?: "Correct answer verified!"
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
                            val srsStatus = remember(pendingRetryItem, currentConcept) {
                                val concept = currentConcept
                                when {
                                    pendingRetryItem != null -> "RETRY"
                                    concept == null -> ""
                                    else -> {
                                        val status = AdaptiveScheduler.statusOf(
                                            concept.repetitions,
                                            concept.intervalDays,
                                            concept.nextReviewAt
                                        )
                                        val isDue = concept.nextReviewAt?.let { it <= System.currentTimeMillis() } == true
                                        when {
                                            status == AdaptiveScheduler.STATUS_NEW -> "NEW"
                                            isDue -> "$status • DUE"
                                            else -> status
                                        }
                                    }
                                }
                            }
                            if (srsStatus.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ElegantPrimary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = srsStatus,
                                        color = ElegantPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            val difficulty = pendingRetryItem?.difficulty ?: currentConcept?.difficulty
                            if (difficulty != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = difficultyColor(difficulty).copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = difficulty.uppercase(),
                                        color = difficultyColor(difficulty),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        val mastery = currentConcept?.masteryScore ?: 0.0
                        val isMastered = currentConcept?.let {
                            AdaptiveScheduler.statusOf(
                                it.repetitions,
                                it.intervalDays,
                                it.nextReviewAt
                            ) == AdaptiveScheduler.STATUS_MASTERED
                        } == true
                        if (mastery > 0.0) {
                            Box(
                                modifier = Modifier.size(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { mastery.toFloat() },
                                    color = if (isMastered) GoldStar else ElegantPrimary,
                                    trackColor = DarkSurfaceVariant,
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Text(
                                    text = "${(mastery * 100).toInt()}%",
                                    color = if (isMastered) GoldStar else TextSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
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

                AnimatedContent(
                    targetState = isQuizStarted,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) }
                ) { started ->
                if (!started) {
                    Spacer(modifier = Modifier.height(24.dp))
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    val currentQ = questionsList[currentQuestionIndex]
                    val qIndex = currentQuestionIndex
                    val selectedIdx = selectedOptionIndices[qIndex] ?: -1
                    val currentCodeInput = codeAnswers[qIndex] ?: ""
                    val isTextInputQuestion = false

                // Progress bar & Question Step Badge
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = CardDefaults.outlinedCardBorder()
                        .copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
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

                        val progressAnim = animateFloatAsState(
                            targetValue = (qIndex + 1).toFloat() / questionsList.size,
                            animationSpec = tween(300),
                            label = "quizProgress"
                        )
                        LinearProgressIndicator(
                            progress = { progressAnim.value },
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
                                border = CardDefaults.outlinedCardBorder()
                                    .copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
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
                                            text = currentQ.codeSnippetPrefix!!.replace(
                                                "\\n",
                                                "\n"
                                            ),
                                            color = CodeBlue,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                    OutlinedTextField(
                                        value = currentCodeInput,
                                        onValueChange = { codeAnswers[qIndex] = it },
                                        placeholder = {
                                            Text(
                                                "type answer here...",
                                                color = TextMuted,
                                                fontSize = 13.sp
                                            )
                                        },
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
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(
                                                DarkBorder
                                            )
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = currentQ.codeSnippetPrefix!!.replace(
                                                "\\n",
                                                "\n"
                                            ),
                                            color = CodeBlue,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                                val correctOptIdx = getCorrectOptionIndex(currentQ)
                                val isAnswered = selectedIdx != -1

                                currentQ.optionsList.forEachIndexed { index, optionText ->
                                    val isSelected = selectedIdx == index
                                    val isCorrect = index == correctOptIdx

                                    val (borderColor, bgColor, iconTint) = when {
                                        !isAnswered -> {
                                            if (isSelected) Triple(
                                                ElegantPrimary,
                                                ElegantPrimaryContainer.copy(alpha = 0.3f),
                                                ElegantPrimary
                                            )
                                            else Triple(DarkBorder, DarkSurface, DarkBorder)
                                        }

                                        isCorrect -> {
                                            Triple(
                                                SuccessGreen,
                                                SuccessGreen.copy(alpha = 0.18f),
                                                SuccessGreen
                                            )
                                        }

                                        isSelected && !isCorrect -> {
                                            Triple(ErrorRed, ErrorRed.copy(alpha = 0.18f), ErrorRed)
                                        }

                                        else -> {
                                            Triple(
                                                DarkBorder,
                                                DarkSurface.copy(alpha = 0.4f),
                                                DarkBorder
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = bgColor,
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(
                                                borderColor
                                            )
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { selectedOptionIndices[qIndex] = index }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 14.dp
                                            ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .border(2.dp, borderColor, CircleShape)
                                                    .background(
                                                        if (isAnswered && (isCorrect || isSelected)) iconTint else androidx.compose.ui.graphics.Color.Transparent,
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isAnswered && isCorrect) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Correct",
                                                        tint = DarkBackground,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                } else if (isAnswered && isSelected && !isCorrect) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Incorrect",
                                                        tint = TextPrimary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = optionText,
                                                color = if (isAnswered && (isCorrect || isSelected)) TextPrimary else TextSecondary,
                                                fontSize = 14.sp,
                                                fontWeight = if (isAnswered && (isCorrect || isSelected)) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isAnswered && isCorrect) {
                                                Text(
                                                    text = "Correct ✓",
                                                    color = SuccessGreen,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else if (isAnswered && isSelected && !isCorrect) {
                                                Text(
                                                    text = "Incorrect ✕",
                                                    color = ErrorRed,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Inline Explanation Box when answered
                                AnimatedVisibility(
                                    visible = isAnswered && currentQ.explanation.isNotBlank(),
                                    enter = fadeIn(tween(200)) + expandVertically(),
                                    exit = fadeOut(tween(120))
                                ) {
                                Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = DarkSurface,
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(
                                                if (selectedIdx == correctOptIdx) SuccessGreen else ElegantPrimary
                                            )
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = if (selectedIdx == correctOptIdx) SuccessGreen else ElegantPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = if (selectedIdx == correctOptIdx) "EXPLANATION (CORRECT!)" else "EXPLANATION",
                                                    color = if (selectedIdx == correctOptIdx) SuccessGreen else ElegantPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = currentQ.explanation.replace("\\n", "\n"),
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                lineHeight = 19.sp
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = null
                        )
                    }
                } else {
                    // Submit All Quiz Questions
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                var correctCount = 0
                                questionsList.forEachIndexed { i, q ->
                                    val userSelIdx = selectedOptionIndices[i] ?: -1
                                    val correctOptIdx = getCorrectOptionIndex(q)
                                    if (userSelIdx == correctOptIdx) correctCount++
                                }

                                val passed =
                                    if (questionsList.size > 1) correctCount >= (questionsList.size / 2 + 1) else correctCount >= 1

                                val userAnswersList = mutableListOf<String>()
                                questionsList.forEachIndexed { i, q ->
                                    val selIdx = selectedOptionIndices[i] ?: -1
                                    val ans = q.optionsList.getOrNull(selIdx)
                                        ?: if (selIdx != -1) selIdx.toString() else "Unanswered"
                                    userAnswersList.add(ans)
                                }

                                val formattedUserAnswer = if (userAnswersList.size == 1) {
                                    userAnswersList.first()
                                } else {
                                    userAnswersList.mapIndexed { idx, a -> "Q${idx + 1}: $a" }
                                        .joinToString(" | ")
                                }

                                val firstQ = questionsList.first()
                                val newStatus = if (passed) "PASSED" else "RETRY_PENDING"

                                // Per-question correctness for mastery tracking
                                val perQuestionResults = JSONArray().apply {
                                    questionsList.forEachIndexed { i, q ->
                                        put(JSONObject().apply {
                                            put("idx", i)
                                            put("isCorrect", (selectedOptionIndices[i] ?: -1) == getCorrectOptionIndex(q))
                                        })
                                    }
                                }.toString()
                                val answeredDifficulty = currentConcept?.difficulty
                                    ?: pendingRetryItem?.difficulty
                                    ?: "Medium"

                                // Insert QuestionHistory entry (RETRY_PENDING if failed so user can re-practice in History)
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
                                        answeredAt = System.currentTimeMillis(),
                                        perQuestionResultsJson = perQuestionResults,
                                        difficulty = answeredDifficulty
                                    )
                                )

                                val currentItem = pendingRetryItem
                                if (currentItem != null) {
                                    if (passed) {
                                        db.historyDao().markConceptPassed(currentItem.conceptTitle)
                                    } else {
                                        db.historyDao()
                                            .updateHistory(currentItem.copy(status = "RETRY_RESOLVED"))
                                    }
                                }

                                var masteryAfter: Double? = null
                                var reviewInterval: Int? = null
                                var reviewStatus: String? = null
                                val concept = currentConcept
                                if (concept != null) {
                                    db.conceptDao().markConceptUsed(concept.id)
                                    // Spaced-repetition scheduling + mastery update
                                    val answeredAt = System.currentTimeMillis()
                                    val srs = AdaptiveScheduler.scheduleAnswer(
                                        repetitions = concept.repetitions,
                                        easeFactor = concept.easeFactor,
                                        intervalDays = concept.intervalDays,
                                        nextReviewAt = concept.nextReviewAt,
                                        lapses = concept.lapses,
                                        passed = passed,
                                        now = answeredAt
                                    )
                                    val recentCorrect = db.historyDao()
                                        .getRecentCorrectnessForConcept(title, 10)
                                    val recentTimes = db.historyDao()
                                        .getRecentTimestampsForConcept(title, 10)
                                    val mastery = AdaptiveScheduler.computeMastery(
                                        recentCorrect,
                                        recentTimes,
                                        answeredAt
                                    )
                                    db.conceptDao().updateReviewState(
                                        id = concept.id,
                                        repetitions = srs.repetitions,
                                        easeFactor = srs.easeFactor,
                                        intervalDays = srs.intervalDays,
                                        nextReviewAt = srs.nextReviewAt,
                                        lapses = srs.lapses,
                                        masteryScore = mastery
                                    )
                                    masteryAfter = mastery
                                    reviewInterval = srs.intervalDays
                                    reviewStatus = AdaptiveScheduler.statusOf(
                                        srs.repetitions,
                                        srs.intervalDays,
                                        srs.nextReviewAt
                                    )
                                }
                                if (passed) {
                                    db.historyDao().markConceptPassed(title)
                                }

                                quizResult = QuizResult(
                                    passed = passed,
                                    correctCount = correctCount,
                                    total = questionsList.size,
                                    masteryBefore = concept?.masteryScore,
                                    masteryAfter = masteryAfter,
                                    nextReviewDays = reviewInterval,
                                    srsStatus = reviewStatus
                                )
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
                }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
            quizResult?.let { result ->
                QuizResultSheet(result = result, onUnlock = onDismiss)
            }
        }
    }
}

@Composable
private fun QuizResultSheet(result: QuizResult, onUnlock: () -> Unit) {
    val accent = if (result.passed) SuccessGreen else ErrorRed
    val scale = remember { Animatable(0.82f) }
    val sheetAlpha = remember { Animatable(0f) }
    val scrimAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scrimAlpha.animateTo(1f, animationSpec = tween(180))
        sheetAlpha.animateTo(1f, animationSpec = tween(160))
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
    val masteryAnim = animateFloatAsState(
        targetValue = (result.masteryAfter ?: 0.0).toFloat(),
        animationSpec = tween(700, delayMillis = 250)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f * scrimAlpha.value))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {},
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = DarkSurface,
            border = CardDefaults.outlinedCardBorder().copy(
                brush = SolidColor(accent.copy(alpha = 0.55f))
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    alpha = sheetAlpha.value
                }
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (result.passed) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (result.passed) "QUIZ PASSED!" else "KEEP PRACTICING",
                    color = accent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${result.correctCount}/${result.total} correct",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (!result.passed) {
                    Text(
                        text = "This concept will be queued again tomorrow and kept in History for retry.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center
                    )
                }
                if (result.masteryBefore != null && result.masteryAfter != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MASTERY",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "${(result.masteryBefore * 100).toInt()}% → ${(result.masteryAfter * 100).toInt()}%",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { masteryAnim.value },
                                color = if (result.srsStatus == AdaptiveScheduler.STATUS_MASTERED) GoldStar else ElegantPrimary,
                                trackColor = DarkBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }
                if (result.nextReviewDays != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Next review in ${result.nextReviewDays} day${if (result.nextReviewDays == 1) "" else "s"}",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        result.srsStatus?.let { status ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (status == AdaptiveScheduler.STATUS_MASTERED) GoldStar.copy(alpha = 0.15f) else ElegantPrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = status,
                                    color = if (status == AdaptiveScheduler.STATUS_MASTERED) GoldStar else ElegantPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onUnlock,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElegantPrimary,
                        contentColor = ElegantOnPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "UNLOCK DEVICE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

fun String?.isValidSnippet(): Boolean {
    if (this == null) return false
    val trimmed = this.trim()
    return trimmed.isNotEmpty() && !trimmed.equals("null", ignoreCase = true)
}

fun getCorrectOptionIndex(q: QuizQuestion): Int {
    val cAns = q.correctAnswer.trim()
    val idxAsInt = cAns.toIntOrNull()
    if (idxAsInt != null && idxAsInt in 0 until q.optionsList.size) {
        return idxAsInt
    }
    val letterIdx = when (cAns.uppercase()) {
        "A" -> 0; "B" -> 1; "C" -> 2; "D" -> 3; else -> -1
    }
    if (letterIdx != -1 && letterIdx < q.optionsList.size) {
        return letterIdx
    }
    val textMatchIdx = q.optionsList.indexOfFirst { it.trim().equals(cAns, ignoreCase = true) }
    if (textMatchIdx != -1) return textMatchIdx
    return 0
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
                var qType = obj.optString("questionType", fallbackQuestionType)
                val optArray = obj.optJSONArray("options")
                val opts = mutableListOf<String>()
                if (optArray != null) {
                    for (j in 0 until optArray.length()) {
                        opts.add(optArray.getString(j))
                    }
                }

                if (opts.isEmpty() && qType == "TRUE_FALSE") {
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
    if (jsonStr.isNullOrBlank()) return emptyList()
    return try {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * Picks the topic with the lowest recency-weighted accuracy so fresh concepts
 * are drawn from the user's weakest areas. Topics without history are treated
 * as neutral (0.5).
 */


