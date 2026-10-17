package com.learnlock.feature.unlock

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.learnlock.data.entity.ConceptItem
import com.learnlock.data.entity.QuestionHistory
import com.learnlock.domain.scheduler.AdaptiveScheduler
import com.learnlock.ui.components.MarkdownView
import com.learnlock.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockQuizScreen(
    onDismiss: () -> Unit,
    retryHistoryId: Long? = null,
    modifier: Modifier = Modifier,
    viewModel: UnlockQuizViewModel = hiltViewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
    val multiSelections = rememberSaveable(saver = CodeAnswersSaver) {
        mutableStateMapOf<Int, String>()
    }
    val codeAnswers = rememberSaveable(saver = CodeAnswersSaver) {
        mutableStateMapOf<Int, String>()
    }

    var isStarred by rememberSaveable { mutableStateOf(false) }
    var showHint by rememberSaveable { mutableStateOf(false) }

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
        val loaded = viewModel.load(retryHistoryId, restoredConceptId, restoredRetryId)
        pendingRetryItem = loaded.retryItem
        currentConcept = loaded.concept
        isStarred = loaded.isStarred
        restoredConceptId = loaded.concept?.id
        restoredRetryId = loaded.retryItem?.id
        isLoading = false
    }

    val title = pendingRetryItem?.conceptTitle ?: currentConcept?.conceptTitle ?: "CS Concept"
    val topic = pendingRetryItem?.topic ?: currentConcept?.topic ?: "General Knowledge"

    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    var showQuitConfirm by rememberSaveable { mutableStateOf(false) }
    // Dismissing freely is safe before starting or after passing. Any other
    // exit (including a failed attempt) asks for confirmation so progress
    // and the queued retry are never lost by accident.
    val canDismissFreely = !isQuizStarted || quizResult != null
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
                            viewModel.toggleStar(topic, title, isStarred)
                        },
                    ) {
                        Icon(
                            imageVector = if (isStarred) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isStarred) {
                                "Remove from favorites"
                            } else {
                                "Add to favorites"
                            },
                            tint = if (isStarred) colors.star else colors.onSurfaceVariant,
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
            val questionsList = remember(pendingRetryItem, currentConcept, viewModel.questionTypes, viewModel.questionsPerQuiz) {
                val parsedQuestions = parseQuestionsList(
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
                val selectedTypes = viewModel.questionTypes
                val eligible = if (pendingRetryItem != null) parsedQuestions else parsedQuestions.filter { it.questionType in selectedTypes }
                eligible.take(viewModel.questionsPerQuiz).ifEmpty { parsedQuestions.take(1) }
            }

            val qIndex = currentQuestionIndex.coerceIn(0, questionsList.lastIndex)
            val currentQ = questionsList[qIndex]
            LaunchedEffect(qIndex) { showHint = false }
            val selectedIdx = selectedOptionIndices[qIndex] ?: -1
            val currentCodeInput = codeAnswers[qIndex] ?: ""
            val isTextInputQuestion = isTextAnswerQuestion(currentQ)
            val isCurrentAnswered = if (isTextInputQuestion) {
                currentCodeInput.isNotBlank()
            } else if (currentQ.questionType == "MULTI_SELECT") {
                (multiSelections[qIndex]?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()).isNotEmpty()
            } else if (currentQ.questionType == "DRAG_BLANK") {
                currentCodeInput.isNotBlank()
            } else {
                selectedIdx != -1
            }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding(),
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
                                    color = if (isMastered) colors.star else colors.primary,
                                    trackColor = colors.surfaceContainerHighest,
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Text(
                                    text = "$masteryPercent%",
                                    style = type.labelSmall,
                                    color = if (isMastered) colors.star else colors.onSurfaceVariant,
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
                        if (currentQ.hint != null) {
                            TextButton(onClick = { showHint = !showHint }) {
                                Text(if (showHint) "Hide hint" else "Show hint")
                            }
                            if (showHint) Text(currentQ.hint, style = type.bodySmall, color = colors.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Render choices (CODE / FILL_BLANK vs MCQ / TRUE_FALSE)
                        if (isTextInputQuestion) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.surfaceContainerHighest, MaterialTheme.shapes.medium)
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Text(
                                        text = when (currentQ.questionType) {
                                            "CODE" -> "Fill in the code answer"
                                            "SHORT_ANSWER" -> "Your answer is reviewed after the quiz"
                                            else -> "Fill in the blank"
                                        },
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
                                        onValueChange = { codeAnswers[qIndex] = it.take(500) },
                                        label = { Text("Your answer") },
                                        placeholder = {
                                            Text("Type answer here…")
                                        },
                                        textStyle = LocalTextStyle.current.copy(
                                            fontFamily = if (currentQ.questionType == "CODE") FontFamily.Monospace else FontFamily.Default,
                                        ),
                                        minLines = if (currentQ.questionType == "SHORT_ANSWER") 4 else 1,
                                        maxLines = if (currentQ.questionType == "SHORT_ANSWER") 8 else 1,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (currentQ.questionType == "SHORT_ANSWER") {
                                        Text("${currentCodeInput.length}/500", style = type.labelSmall, color = colors.onSurfaceVariant, modifier = Modifier.align(Alignment.End))
                                    }
                                }
                            }
                        } else if (currentQ.questionType == "MULTI_SELECT") {
                            val selectedSet = multiSelections[qIndex]?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet().orEmpty()
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Select all that apply", style = type.labelMedium, color = colors.onSurfaceVariant)
                                currentQ.optionsList.forEachIndexed { index, option ->
                                    val checked = index in selectedSet
                                    Row(
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(MaterialTheme.shapes.medium)
                                            .background(if (checked) colors.secondaryContainer else colors.surfaceContainerHigh)
                                            .clickable {
                                                val next = if (checked) selectedSet - index else selectedSet + index
                                                multiSelections[qIndex] = next.sorted().joinToString(",")
                                            }.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Checkbox(checked = checked, onCheckedChange = { enabled ->
                                            val next = if (enabled) selectedSet + index else selectedSet - index
                                            multiSelections[qIndex] = next.sorted().joinToString(",")
                                        })
                                        Text(option, style = type.bodyMedium)
                                    }
                                }
                            }
                        } else if (currentQ.questionType == "DRAG_BLANK") {
                            val placed = currentCodeInput.split(",").mapNotNull { it.toIntOrNull() }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Tap or drag choices to place answers in order", style = type.labelMedium, color = colors.onSurfaceVariant)
                                currentQ.optionsList.forEachIndexed { index, option ->
                                    FilterChip(
                                        selected = index in placed,
                                        onClick = {
                                            val next = if (index in placed) placed - index else placed + index
                                            codeAnswers[qIndex] = next.joinToString(",")
                                        },
                                        label = { Text(option) },
                                        leadingIcon = if (index in placed) { { Text("${placed.indexOf(index) + 1}") } } else null,
                                        modifier = Modifier.pointerInput(qIndex, index) {
                                            detectDragGestures(
                                                onDragEnd = {
                                                    val latest = codeAnswers[qIndex]?.split(",")?.mapNotNull { it.toIntOrNull() }.orEmpty()
                                                    if (index !in latest) codeAnswers[qIndex] = (latest + index).joinToString(",")
                                                },
                                                onDrag = { change, _ -> change.consume() },
                                            )
                                        },
                                    )
                                }
                                if (placed.isNotEmpty()) Text("Placed: ${placed.mapNotNull { currentQ.optionsList.getOrNull(it) }.joinToString(" → ")}", color = colors.primary)
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.selectableGroup(),
                            ) {
                                if (currentQ.codeSnippetPrefix.isValidSnippet()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(colors.surfaceContainerHighest, MaterialTheme.shapes.small)
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = currentQ.codeSnippetPrefix!!.replace(
                                                "\\n",
                                                "\n"
                                            ),
                                            style = type.bodySmall,
                                            fontFamily = if (programmingSubject) FontFamily.Monospace else FontFamily.Default,
                                            color = colors.onSurface
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
                                                colors.success,
                                                colors.successContainer,
                                                colors.success
                                            )
                                        }

                                        isSelected && !isCorrect -> {
                                            Triple(
                                                colors.error,
                                                colors.errorContainer,
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

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.medium)
                                            .background(bgColor)
                                            .heightIn(min = 48.dp)
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
                                                        tint = colors.onSuccess,
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
                                                    color = colors.success
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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = if (selectedIdx == correctOptIdx) colors.success else colors.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = if (selectedIdx == correctOptIdx) "Explanation (correct!)" else "Explanation",
                                                    style = type.labelSmall,
                                                    color = if (selectedIdx == correctOptIdx) colors.success else colors.primary,
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
                if (pendingRetryItem != null && isCurrentAnswered) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceContainerHigh, MaterialTheme.shapes.medium)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Previous answer", style = type.labelMedium, color = colors.onSurfaceVariant)
                            Text(pendingRetryItem!!.userAnswer, style = type.bodyMedium, color = colors.onSurface)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                }

                Surface(
                    color = colors.background,
                    modifier = Modifier
                        .fillMaxWidth(),
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
                            Column(modifier = Modifier.fillMaxWidth()) {
                            if (qIndex > 0) {
                                TextButton(onClick = { currentQuestionIndex-- }, modifier = Modifier.align(Alignment.Start)) {
                                    Text("Previous question")
                                }
                            }
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
                            }
                        } else {
                            // Submit All Quiz Questions
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        quizResult = viewModel.submit(
                                            questions = questionsList,
                                            selectedOptionIndices = selectedOptionIndices,
                                            multiSelections = multiSelections,
                                            codeAnswers = codeAnswers,
                                            title = title,
                                            topic = topic,
                                            summary = summary,
                                            rawQuestionsJson = rawQuestionsJson,
                                            isStarred = isStarred,
                                            concept = currentConcept,
                                            pendingRetryItem = pendingRetryItem,
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
internal fun QuizResultSheet(
    result: QuizResult,
    onUnlock: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val accent = if (result.passed) colors.success else colors.error
    val scale = remember { Animatable(0.82f) }
    val sheetAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        sheetAlpha.animateTo(1f, animationSpec = AppMotion.fade)
        scale.animateTo(1f, animationSpec = AppMotion.heroSpatial)
    }
    val masteryAnim = animateFloatAsState(
        targetValue = (result.masteryAfter ?: 0.0).toFloat(),
        animationSpec = AppMotion.progress
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
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(if (result.passed) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.medium)
                        .background(if (result.passed) colors.successContainer else colors.errorContainer),
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
                    text = "Quiz complete",
                    style = type.titleLarge,
                    color = accent,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${result.correctCount}/${result.total - result.pendingCount} graded correct · ${result.total - result.correctCount - result.pendingCount} to retry",
                    style = type.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                if (result.pendingCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${result.pendingCount} short answer${if (result.pendingCount == 1) " is" else "s are"} queued for review", style = type.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                }
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





