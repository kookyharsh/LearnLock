package com.example.ui.quiz

import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.room.withTransaction
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

data class QuizResult(
    val passed: Boolean,
    val correctCount: Int,
    val total: Int,
    val masteryBefore: Double?,
    val masteryAfter: Double?,
    val nextReviewDays: Int?,
    val srsStatus: String?,
)

private val SelectedOptionsSaver: Saver<SnapshotStateMap<Int, Int>, IntArray> = Saver(
    save = { state: SnapshotStateMap<Int, Int> ->
        state.entries.flatMap { entry -> listOf(entry.key, entry.value) }.toIntArray()
    },
    restore = { saved: IntArray ->
        mutableStateMapOf<Int, Int>().apply {
            saved.toList().chunked(2).forEach { pair ->
                if (pair.size == 2) put(pair[0], pair[1])
            }
        }
    },
)

private val CodeAnswersSaver: Saver<SnapshotStateMap<Int, String>, Bundle> = Saver(
    save = { state: SnapshotStateMap<Int, String> ->
        Bundle().apply { state.forEach { key, value -> putString(key.toString(), value) } }
    },
    restore = { saved: Bundle ->
        mutableStateMapOf<Int, String>().apply {
            saved.keySet().forEach { key ->
                key.toIntOrNull()?.let { put(it, saved.getString(key) ?: "") }
            }
        }
    },
)

private val QuizResultStateSaver: Saver<MutableState<QuizResult?>, Bundle> = Saver(
    save = { state: MutableState<QuizResult?> ->
        Bundle().apply {
            state.value?.let { r ->
                putBoolean("hasResult", true)
                putBoolean("passed", r.passed)
                putInt("correct", r.correctCount)
                putInt("total", r.total)
                r.masteryBefore?.let { putDouble("masteryBefore", it) }
                r.masteryAfter?.let { putDouble("masteryAfter", it) }
                r.nextReviewDays?.let { putInt("reviewDays", it) }
                r.srsStatus?.let { putString("srs", it) }
            }
        }
    },
    restore = { saved: Bundle ->
        mutableStateOf(
            if (saved.getBoolean("hasResult", false)) {
                QuizResult(
                    passed = saved.getBoolean("passed"),
                    correctCount = saved.getInt("correct"),
                    total = saved.getInt("total"),
                    masteryBefore = if (saved.containsKey("masteryBefore")) saved.getDouble("masteryBefore") else null,
                    masteryAfter = if (saved.containsKey("masteryAfter")) saved.getDouble("masteryAfter") else null,
                    nextReviewDays = if (saved.containsKey("reviewDays")) saved.getInt("reviewDays") else null,
                    srsStatus = saved.getString("srs"),
                )
            } else {
                null
            },
        )
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockQuizScreen(
    onDismiss: () -> Unit,
    retryHistoryId: Long? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val prefsManager = remember { AppPreferencesManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var pendingRetryItem by remember { mutableStateOf<QuestionHistory?>(null) }
    var currentConcept by remember { mutableStateOf<ConceptItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var isQuizStarted by rememberSaveable { mutableStateOf(false) }
    var currentQuestionIndex by rememberSaveable { mutableIntStateOf(0) }

    // Track selected option indices and code answers per question index.
    // Custom savers keep answers across recreation so progress is never lost.
    val selectedOptionIndices = rememberSaveable(saver = SelectedOptionsSaver) {
        mutableStateMapOf<Int, Int>()
    }
    val codeAnswers = rememberSaveable(saver = CodeAnswersSaver) {
        mutableStateMapOf<Int, String>()
    }

    var isStarred by rememberSaveable { mutableStateOf(false) }

    val quizResultHolder = rememberSaveable(saver = QuizResultStateSaver) {
        mutableStateOf<QuizResult?>(null)
    }
    var quizResult by quizResultHolder

    // Content identity survives recreation so a rotation reloads the same
    // concept from the database instead of regenerating (and re-billing).
    var restoredConceptId by rememberSaveable { mutableStateOf<Long?>(null) }
    var restoredRetryId by rememberSaveable { mutableStateOf<Long?>(null) }

    val currentTime = remember {
        DateFormat.format("hh:mm a", Date()).toString()
    }

    LaunchedEffect(Unit) {
        val didFreshLoad = restoredConceptId == null && restoredRetryId == null
        try {
            if (!didFreshLoad) {
                // Recreation (e.g. rotation): reload the same content from the
                // database. Never regenerate and never re-bill the API.
                restoredRetryId?.let { id ->
                    db.historyDao().getHistoryByIdOnce(id)?.let { item ->
                        pendingRetryItem = item
                        isStarred = item.isStarred
                    }
                }
                restoredConceptId?.let { id ->
                    db.conceptDao().getConceptById(id)?.let { concept ->
                        currentConcept = concept
                        if (pendingRetryItem == null) isStarred = concept.isStarred
                    }
                }
            } else {
                // A retry opened for one specific history entry wins over the
                // global newest retry, so "Retry Question Now" quizzes that card.
                val requestedId = retryHistoryId
                val requestedRetry: QuestionHistory? = if (requestedId != null) {
                    db.historyDao().getHistoryByIdOnce(requestedId)
                } else {
                    null
                }
                val retryItem: QuestionHistory? =
                    requestedRetry ?: db.historyDao().getPendingRetryQuestion()
                if (retryItem != null) {
                    pendingRetryItem = retryItem
                    isStarred = retryItem.isStarred
                    val originalConcept = db.conceptDao().getConceptByTopicAndTitle(
                        retryItem.topic,
                        retryItem.conceptTitle,
                    )
                    if (originalConcept != null) {
                        currentConcept = originalConcept
                    }
                } else {
                val selectedTopics = prefsManager.getSelectedTopics().toList()
                val cooldown24h = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                val recentTitles = db.historyDao().getRecentConceptTitles(cooldown24h)
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
                    // A stored concept whose question count differs from the
                    // current preference is still usable as-is: the quiz simply
                    // renders however many questions it has. It must not be
                    // burned, or the queue silently shrinks on every slider move.
                    if (parsedQuestions.isEmpty()) {
                        concept = null
                    }
                }

                // If no pre-generated concept matches, generate a fresh AI concept
                if (concept == null && prefsManager.getApiKey().isNotBlank()) {
                    val generator = com.example.service.ConceptGenerator(prefsManager)
                    val fresh = generator.generateBatchConcepts(
                        topics = prefsManager.getSelectedTopics(),
                        count = 1
                    )
                    if (fresh.isNotEmpty()) {
                        val newId = db.conceptDao().insertConcept(fresh.first())
                        concept = fresh.first().copy(id = newId)
                    }
                }

                // 4) Offline / empty queue fallback: recycle a previously answered concept for review
                if (concept == null) {
                    concept = if (selectedTopics.isEmpty()) {
                        db.conceptDao().getRecycledReviewConcept()
                    } else {
                        db.conceptDao().getRecycledReviewConceptForTopics(selectedTopics)
                            ?: db.conceptDao().getRecycledReviewConcept()
                    }
                }

                currentConcept = concept
                if (concept != null) {
                    isStarred = concept.isStarred
                }
                restoredConceptId = currentConcept?.id
                restoredRetryId = pendingRetryItem?.id
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
        if (didFreshLoad) {
            UnlockReceiver.pregenerateConceptsIfNeeded(context, prefsManager)
        }
    }

    val title = pendingRetryItem?.conceptTitle ?: currentConcept?.conceptTitle ?: "CS Concept"
    val topic = pendingRetryItem?.topic ?: currentConcept?.topic ?: "General Knowledge"

    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    var showQuitConfirm by rememberSaveable { mutableStateOf(false) }
    // Dismissing freely is safe before starting or after passing. Any other
    // exit (including a failed attempt) asks for confirmation so progress
    // and the queued retry are never lost by accident.
    val canDismissFreely = !isQuizStarted || quizResult?.passed == true
    val requestDismiss: () -> Unit = {
        if (canDismissFreely) onDismiss() else showQuitConfirm = true
    }
    val retryQuiz: () -> Unit = {
        selectedOptionIndices.clear()
        codeAnswers.clear()
        currentQuestionIndex = 0
        quizResult = null
        isQuizStarted = true
    }

    BackHandler(enabled = !canDismissFreely) { showQuitConfirm = true }

    if (showQuitConfirm) {
        AlertDialog(
            onDismissRequest = { showQuitConfirm = false },
            title = { Text("Quit quiz?") },
            text = { Text("Your answers so far are kept, and this concept stays queued for retry.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showQuitConfirm = false
                        onDismiss()
                    },
                ) {
                    Text("Quit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuitConfirm = false }) {
                    Text("Keep practicing")
                }
            },
        )
    }

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Unlock quiz • $currentTime",
                            style = type.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isStarred = !isStarred
                            coroutineScope.launch {
                                db.conceptDao().updateStarStatusForConcept(topic, title, isStarred)
                                db.historyDao().updateStarStatusForConcept(topic, title, isStarred)
                            }
                        },
                    ) {
                        Icon(
                            imageVector = if (isStarred) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isStarred) {
                                "Remove from favorites"
                            } else {
                                "Add to favorites"
                            },
                            tint = if (isStarred) GoldStar else colors.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = requestDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close quiz",
                            tint = colors.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
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
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No concepts available yet",
                            style = type.titleMedium,
                            color = colors.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Configure your API key in Settings or generate concepts from the Learn tab to start learning on device unlocks.",
                            style = type.bodyMedium,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            // Fail-open: with nothing to quiz, the device must still unlock.
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Unlock device", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
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

            val qIndex = currentQuestionIndex.coerceIn(0, questionsList.lastIndex)
            val currentQ = questionsList[qIndex]
            val selectedIdx = selectedOptionIndices[qIndex] ?: -1
            val currentCodeInput = codeAnswers[qIndex] ?: ""
            val isTextInputQuestion = isTextAnswerQuestion(currentQ)
            val isCurrentAnswered = if (isTextInputQuestion) {
                currentCodeInput.isNotBlank()
            } else {
                selectedIdx != -1
            }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {

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
                                    .background(colors.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = colors.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = topic,
                                style = type.labelSmall,
                                color = colors.primary,
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
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = colors.primaryContainer,
                                ) {
                                    Text(
                                        text = srsStatus,
                                        style = type.labelSmall,
                                        color = colors.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            val difficulty = pendingRetryItem?.difficulty ?: currentConcept?.difficulty
                            if (difficulty != null) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = colors.secondaryContainer,
                                ) {
                                    Text(
                                        text = difficulty,
                                        style = type.labelSmall,
                                        color = colors.onSecondaryContainer,
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
                            val masteryPercent = (mastery * 100).toInt()
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = "Concept mastery $masteryPercent percent"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { mastery.toFloat() },
                                    color = if (isMastered) GoldStar else colors.primary,
                                    trackColor = colors.surfaceContainerHighest,
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Text(
                                    text = "$masteryPercent%",
                                    style = type.labelSmall,
                                    color = if (isMastered) GoldStar else colors.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = title,
                    style = type.headlineSmall,
                    color = colors.onSurface,
                    modifier = Modifier.semantics { heading() },
                )

                Spacer(modifier = Modifier.height(12.dp))

                MarkdownView(markdownText = summary)

                val programmingSubject = isProgrammingSubject(topic)
                val incompatibleLegacyExample = !programmingSubject && looksLikeProgrammingCode(codeExample)
                if (codeExample.isValidSnippet() && !incompatibleLegacyExample) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = colors.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (programmingSubject) "Code example" else "Example",
                                style = type.labelMedium,
                                color = colors.primary,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = codeExample!!.replace("\\n", "\n"),
                                style = type.bodyMedium,
                                fontFamily = if (programmingSubject) FontFamily.Monospace else FontFamily.Default,
                                color = colors.onSurface,
                            )
                        }
                    }
                } else if (incompatibleLegacyExample) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "The saved example did not match this subject, so it was omitted.",
                        style = type.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }

                if (isQuizStarted) {
                    Spacer(modifier = Modifier.height(16.dp))

                // Progress bar & Question Step Badge
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = colors.primaryContainer,
                            ) {
                                Text(
                                    text = "Question ${qIndex + 1} of ${questionsList.size}",
                                    style = type.labelSmall,
                                    color = colors.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Text(
                                text = "${qIndex + 1}/${questionsList.size}",
                                style = type.labelMedium,
                                color = colors.onSurfaceVariant,
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
                            trackColor = colors.surfaceContainerHighest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .semantics(mergeDescendants = true) {
                                    contentDescription =
                                        "Question ${qIndex + 1} of ${questionsList.size}"
                                },
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Question Text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = currentQ.questionText,
                                style = type.titleSmall,
                                color = colors.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Render choices (CODE / FILL_BLANK vs MCQ / TRUE_FALSE)
                        if (isTextInputQuestion) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = colors.surfaceContainerHighest,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = if (currentQ.questionType == "CODE") "Fill in the code answer" else "Type your answer",
                                        style = type.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (currentQ.codeSnippetPrefix.isValidSnippet()) {
                                        Text(
                                            text = currentQ.codeSnippetPrefix!!.replace(
                                                "\\n",
                                                "\n"
                                            ),
                                            style = type.bodyMedium,
                                            fontFamily = if (programmingSubject) FontFamily.Monospace else FontFamily.Default,
                                            color = colors.onSurface,
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                    OutlinedTextField(
                                        value = currentCodeInput,
                                        onValueChange = { codeAnswers[qIndex] = it },
                                        label = { Text("Your answer") },
                                        placeholder = {
                                            Text("Type answer here…")
                                        },
                                        textStyle = LocalTextStyle.current.copy(
                                            fontFamily = FontFamily.Monospace,
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.selectableGroup(),
                            ) {
                                if (currentQ.codeSnippetPrefix.isValidSnippet()) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = colors.surfaceContainerHighest,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = currentQ.codeSnippetPrefix!!.replace(
                                                "\\n",
                                                "\n"
                                            ),
                                            style = type.bodySmall,
                                            fontFamily = if (programmingSubject) FontFamily.Monospace else FontFamily.Default,
                                            color = colors.onSurface,
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
                                                colors.primary,
                                                colors.primaryContainer,
                                                colors.primary
                                            )
                                            else Triple(
                                                colors.outline,
                                                colors.surfaceContainerHigh,
                                                colors.outline
                                            )
                                        }

                                        isCorrect -> {
                                            Triple(
                                                SuccessGreen,
                                                SuccessGreen.copy(alpha = 0.18f),
                                                SuccessGreen
                                            )
                                        }

                                        isSelected && !isCorrect -> {
                                            Triple(
                                                colors.error,
                                                colors.error.copy(alpha = 0.18f),
                                                colors.error
                                            )
                                        }

                                        else -> {
                                            Triple(
                                                colors.outlineVariant,
                                                colors.surfaceContainerLow,
                                                colors.outlineVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = bgColor,
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(
                                                borderColor
                                            )
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.medium)
                                            // Locked once answered: re-tapping a revealed
                                            // correct option must not change the result.
                                            .selectable(
                                                selected = isSelected,
                                                enabled = !isAnswered,
                                                role = Role.RadioButton,
                                                onClick = { selectedOptionIndices[qIndex] = index },
                                            )
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
                                                        tint = colors.inverseOnSurface,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                } else if (isAnswered && isSelected && !isCorrect) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Incorrect",
                                                        tint = colors.onError,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = optionText,
                                                style = type.bodyMedium,
                                                fontWeight = if (isAnswered && (isCorrect || isSelected)) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isAnswered && (isCorrect || isSelected)) colors.onSurface else colors.onSurfaceVariant,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isAnswered && isCorrect) {
                                                Text(
                                                    text = "Correct ✓",
                                                    style = type.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SuccessGreen
                                                )
                                            } else if (isAnswered && isSelected && !isCorrect) {
                                                Text(
                                                    text = "Incorrect ✕",
                                                    style = type.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.error
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
                                        shape = MaterialTheme.shapes.medium,
                                        color = colors.surfaceContainerHighest,
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
                                                    tint = if (selectedIdx == correctOptIdx) SuccessGreen else colors.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = if (selectedIdx == correctOptIdx) "Explanation (correct!)" else "Explanation",
                                                    style = type.labelSmall,
                                                    color = if (selectedIdx == correctOptIdx) SuccessGreen else colors.primary,
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = currentQ.explanation.replace("\\n", "\n"),
                                                style = type.bodySmall,
                                                color = colors.onSurface,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                }

                Surface(
                    color = colors.background,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        if (!isQuizStarted) {
                            Button(
                                onClick = { isQuizStarted = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.onPrimary,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp),
                            ) {
                                Text(
                                    text = "Start quiz",
                                    style = type.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                    contentDescription = null,
                                )
                            }
                        } else if (qIndex < questionsList.size - 1) {
                            Button(
                                onClick = { currentQuestionIndex++ },
                                enabled = isCurrentAnswered,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.onPrimary,
                                    disabledContainerColor = colors.surfaceContainerHigh,
                                    disabledContentColor = colors.onSurfaceVariant.copy(alpha = 0.5f),
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp),
                            ) {
                                Text(
                                    text = "Next question (${qIndex + 1}/${questionsList.size})",
                                    style = type.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                    contentDescription = null,
                                )
                            }
                        } else {
                            // Submit All Quiz Questions
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        var correctCount = 0
                                        var gradableCount = 0
                                        questionsList.forEachIndexed { i, q ->
                                            if (isQuestionGradable(q)) {
                                                gradableCount++
                                                if (isQuestionCorrect(q, selectedOptionIndices[i], codeAnswers[i])) {
                                                    correctCount++
                                                }
                                            }
                                        }

                                        val passed = gradableCount > 0 &&
                                            if (gradableCount > 1) correctCount >= (gradableCount / 2 + 1) else correctCount >= 1

                                        val userAnswersList = mutableListOf<String>()
                                        questionsList.forEachIndexed { i, q ->
                                            if (isTextAnswerQuestion(q)) {
                                                userAnswersList.add(
                                                    codeAnswers[i]?.trim().takeIf { !it.isNullOrBlank() }
                                                        ?: "Unanswered",
                                                )
                                            } else {
                                                val selIdx = selectedOptionIndices[i] ?: -1
                                                val ans = q.optionsList.getOrNull(selIdx)
                                                    ?: if (selIdx != -1) selIdx.toString() else "Unanswered"
                                                userAnswersList.add(ans)
                                            }
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
                                                    put("isCorrect", isQuestionCorrect(q, selectedOptionIndices[i], codeAnswers[i]))
                                                })
                                            }
                                        }.toString()
                                        val answeredDifficulty = currentConcept?.difficulty
                                            ?: pendingRetryItem?.difficulty
                                            ?: "Medium"

                                        // All submit writes are atomic: a crash mid-submit
                                        // cannot leave history and SRS half-applied.
                                        val submitConcept = currentConcept
                                        var masteryAfter: Double? = null
                                        var reviewInterval: Int? = null
                                        var reviewStatus: String? = null
                                        db.withTransaction {
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
                                            if (currentItem != null && passed) {
                                                // The retried concept is resolved only by passing;
                                                // a failed retry keeps its RETRY_PENDING status.
                                                db.historyDao().markConceptPassed(
                                                    currentItem.topic,
                                                    currentItem.conceptTitle,
                                                )
                                            }

                                            val concept = submitConcept
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
                                                    .getRecentCorrectnessForConcept(topic, title, 10)
                                                val recentTimes = db.historyDao()
                                                    .getRecentTimestampsForConcept(topic, title, 10)
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
                                                db.historyDao().markConceptPassed(topic, title)
                                            }
                                        }

                                        quizResult = QuizResult(
                                            passed = passed,
                                            correctCount = correctCount,
                                            total = gradableCount,
                                            masteryBefore = submitConcept?.masteryScore,
                                            masteryAfter = masteryAfter,
                                            nextReviewDays = reviewInterval,
                                            srsStatus = reviewStatus
                                        )
                                    }
                                },
                                enabled = isCurrentAnswered,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.onPrimary,
                                    disabledContainerColor = colors.surfaceContainerHigh,
                                    disabledContentColor = colors.onSurfaceVariant.copy(alpha = 0.5f),
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Submit quiz",
                                    style = type.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
            quizResult?.let { result ->
                QuizResultSheet(
                    result = result,
                    onUnlock = onDismiss,
                    onRetry = retryQuiz,
                    onClose = requestDismiss,
                )
            }
        }
    }
}

@Composable
private fun QuizResultSheet(
    result: QuizResult,
    onUnlock: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val accent = if (result.passed) SuccessGreen else colors.error
    val scale = remember { Animatable(0.82f) }
    val sheetAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        sheetAlpha.animateTo(1f, animationSpec = tween(160))
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
    val masteryAnim = animateFloatAsState(
        targetValue = (result.masteryAfter ?: 0.0).toFloat(),
        animationSpec = tween(700, delayMillis = 250)
    )

    // A real dialog traps focus and cannot be dismissed by tapping outside:
    // unlocking is an explicit action, especially after a failed attempt.
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = colors.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
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
                    text = if (result.passed) "Quiz passed" else "Keep practicing",
                    style = type.titleLarge,
                    color = accent,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${result.correctCount}/${result.total} correct",
                    style = type.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (!result.passed) {
                    Text(
                        text = "This concept will be queued again tomorrow and kept in History for retry.",
                        style = type.bodySmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                if (result.masteryBefore != null && result.masteryAfter != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = colors.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Mastery",
                                    style = type.labelSmall,
                                    color = colors.onSurfaceVariant,
                                )
                                Text(
                                    text = "${(result.masteryBefore * 100).toInt()}% → ${(result.masteryAfter * 100).toInt()}%",
                                    style = type.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface,
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { masteryAnim.value },
                                trackColor = colors.surfaceContainerHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .semantics(mergeDescendants = true) {
                                        contentDescription =
                                            "Mastery ${(result.masteryAfter * 100).toInt()} percent"
                                    },
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
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Next review in ${result.nextReviewDays} day${if (result.nextReviewDays == 1) "" else "s"}",
                            style = type.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                        result.srsStatus?.let { status ->
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = colors.secondaryContainer,
                            ) {
                                Text(
                                    text = status,
                                    style = type.labelSmall,
                                    color = colors.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                if (result.passed) {
                    Button(
                        onClick = onUnlock,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unlock device",
                            style = type.titleSmall,
                        )
                    }
                } else {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Retry quiz",
                            style = type.titleSmall,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Close without passing")
                    }
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

private fun isProgrammingSubject(topic: String): Boolean {
    val normalized = topic.lowercase()
    return listOf(
        "programming", "computer", "software", "technology", "coding",
        "python", "java", "kotlin", "javascript", "sql", "web development",
    ).any(normalized::contains)
}

private fun looksLikeProgrammingCode(value: String?): Boolean {
    if (value.isNullOrBlank()) return false
    val normalized = value.lowercase()
    return listOf(
        "def ", "return ", "function ", "const ", "let ", "var ",
        "if (", "if ", "{", "};", "public class", "select ", "import ",
    ).any(normalized::contains)
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
    // Ungradable: the stored answer matches nothing. Callers must exclude
    // such questions from grading instead of accepting option A.
    return -1
}

/** CODE / FILL_BLANK questions are answered by typing, not by picking. */
private fun isTextAnswerQuestion(q: QuizQuestion): Boolean =
    q.questionType == "CODE" || q.questionType == "FILL_BLANK"

private fun isQuestionGradable(q: QuizQuestion): Boolean =
    if (isTextAnswerQuestion(q)) q.correctAnswer.isNotBlank() else getCorrectOptionIndex(q) != -1

private fun isQuestionCorrect(q: QuizQuestion, selectedIdx: Int?, codeText: String?): Boolean {
    if (isTextAnswerQuestion(q)) {
        if (q.correctAnswer.isBlank()) return false
        val userText = codeText?.trim().orEmpty()
        return userText.isNotBlank() && userText.equals(q.correctAnswer.trim(), ignoreCase = true)
    }
    val correctOptIdx = getCorrectOptionIndex(q)
    return correctOptIdx != -1 && (selectedIdx ?: -1) == correctOptIdx
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
