package com.learnlock.feature.settings

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnlock.R
import com.learnlock.data.preferences.ThemeMode
import com.learnlock.feature.tutor.TutorTileService
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(
    snackbarHostState: SnackbarHostState,
    onStartTour: (() -> Unit)? = null,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    SettingsScreen(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        onStartTour = onStartTour,
        onThemeModeChange = onThemeModeChange,
        onDynamicColorChange = onDynamicColorChange,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState,
    onStartTour: (() -> Unit)? = null,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val initial = viewModel.initial
    val counts by viewModel.counts.collectAsStateWithLifecycle()

    var apiKeyInput by rememberSaveable { mutableStateOf(initial.apiKey) }
    var customModelInput by rememberSaveable { mutableStateOf(initial.customModel) }
    var isApiKeyVisible by rememberSaveable { mutableStateOf(value = false) }

    var isTestingKey by rememberSaveable { mutableStateOf(value = false) }
    var keyTestError by rememberSaveable { mutableStateOf<String?>(null) }

    var windowEnabled by rememberSaveable { mutableStateOf(initial.learningWindowEnabled) }
    var startTimeInput by rememberSaveable { mutableStateOf(initial.learningWindowStart) }
    var endTimeInput by rememberSaveable { mutableStateOf(initial.learningWindowEnd) }
    var skipDuringActive by rememberSaveable { mutableStateOf(initial.skipDuringActiveWindow) }

    // Backed by prefs on every change, so a plain remember re-reading prefs is
    // equivalent after recreation.
    var selectedTopics by remember { mutableStateOf(initial.selectedTopics) }
    var newTopicInput by rememberSaveable { mutableStateOf("") }

    var questionsCountInput by rememberSaveable { mutableIntStateOf(initial.questionsPerQuiz) }
    var difficultyInput by rememberSaveable { mutableStateOf(initial.difficultyLevel) }
    var questionTypes by remember { mutableStateOf(initial.questionTypes) }
    var dailyGoal by rememberSaveable { mutableIntStateOf(initial.dailyXpGoal) }
    var themeMode by rememberSaveable { mutableStateOf(initial.themeMode) }
    var dynamicColor by rememberSaveable { mutableStateOf(viewModel.isDynamicColor()) }
    var showAppearanceSheet by rememberSaveable { mutableStateOf(value = false) }
    var showGoalSheet by rememberSaveable { mutableStateOf(value = false) }

    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    val requestTileAdd: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val statusBar = context.getSystemService(android.app.StatusBarManager::class.java)
            statusBar.requestAddTileService(
                ComponentName(context, TutorTileService::class.java),
                context.getString(R.string.tile_label),
                android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_tile_tutor),
                context.mainExecutor,
            ) { result ->
                val message = when (result) {
                    android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                        "Tutor tile added to Quick Settings"
                    android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
                        "Tutor tile was not added"
                    else -> null
                }
                if (message != null) {
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                }
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Swipe down twice, tap the pencil icon, and add the Tutor tile.")
            }
        }
    }

    val deleteAllData = {
        coroutineScope.launch {
            try {
                viewModel.deleteAllData()
                snackbarHostState.showSnackbar("All learning data deleted")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to delete data: ${e.message}")
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete all learning data?") },
            text = {
                Text(
                    "This permanently deletes every generated concept and quiz answer, " +
                        "including spaced-repetition and mastery progress. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        deleteAllData()
                    },
                ) {
                    Text("Delete", color = colors.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
            TopAppBar(
            title = { Text("Settings") },
            actions = {
                onStartTour?.let { tourOnClick ->
                    IconButton(onClick = tourOnClick) {
                        Icon(
                            imageVector = Icons.Default.CompassCalibration,
                            contentDescription = "Take the interactive tour",
                        )
                    }
                }
            },
            windowInsets = WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SettingsSection(
                title = "Appearance",
                description = "Choose how LearnLock follows your device theme.",
            ) {
                OutlinedButton(
                    onClick = { showAppearanceSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrightnessAuto,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Theme & colors",
                            modifier = Modifier.weight(1f),
                            style = type.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                        )
                        Text(
                            text = when (themeMode) {
                                ThemeMode.SYSTEM -> "System"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            } + if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) " · dynamic" else "",
                            style = type.labelMedium,
                            color = colors.primary,
                        )
                    }
                }
            }

            // Credentials are a discrete interactive group: keep one card.
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SettingsSectionHeader(
                        icon = Icons.Default.Api,
                        title = "LLM API credentials",
                        description = "Encrypted key used to pre-generate adaptive learning concepts. " +
                            "Supports Gemini (AIza...), OpenRouter (sk-or-...), or OpenAI (sk-...) keys.",
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            viewModel.setApiKey(it)
                            keyTestError = null
                        },
                        label = { Text("API token / key") },
                        placeholder = { Text("sk-or-v1-... or AIzaSy...") },
                        supportingText = {
                            Text(keyTestError ?: "Stored encrypted on this device.")
                        },
                        isError = keyTestError != null,
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) {
                                        Icons.Default.Visibility
                                    } else {
                                        Icons.Default.VisibilityOff
                                    },
                                    contentDescription = if (isApiKeyVisible) {
                                        "Hide API key"
                                    } else {
                                        "Show API key"
                                    },
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customModelInput,
                        onValueChange = {
                            customModelInput = it
                            viewModel.setCustomModel(it)
                        },
                        label = { Text("Model name (optional override)") },
                        placeholder = { Text("Default: google/gemini-2.5-flash or gpt-4o-mini") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isTestingKey = true
                                keyTestError = null
                                try {
                                    val (ok, message) = viewModel.testApiKeyConnection()
                                    if (ok) {
                                        snackbarHostState.showSnackbar(message)
                                    } else {
                                        keyTestError = message
                                    }
                                } catch (e: Exception) {
                                    keyTestError = "Connection failed: ${e.message}"
                                } finally {
                                    isTestingKey = false
                                }
                            }
                        },
                        enabled = !isTestingKey && apiKeyInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isTestingKey) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testing connection…")
                        } else {
                            Icon(
                                Icons.Default.NetworkCheck,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test API key connection", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            SettingsSection(
                title = "Learning window schedule",
                description = "When enabled, unlock quizzes only appear inside this daily window.",
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = windowEnabled,
                            role = Role.Switch,
                            onValueChange = {
                                windowEnabled = it
                                viewModel.setLearningWindowEnabled(it)
                            },
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = colors.primary,
                        )
                        Text(
                            text = "Limit quizzes to a schedule",
                            style = type.bodyMedium,
                            color = colors.onSurface,
                        )
                    }
                    Switch(
                        checked = windowEnabled,
                        onCheckedChange = null,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TimeField(
                        label = "Start time",
                        placeholder = "09:00 AM",
                        value = startTimeInput,
                        defaultHour = 9,
                        defaultMinute = 0,
                        onPicked = {
                            startTimeInput = it
                            viewModel.setLearningWindowStart(it)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    TimeField(
                        label = "End time",
                        placeholder = "09:00 PM",
                        value = endTimeInput,
                        defaultHour = 21,
                        defaultMinute = 0,
                        onPicked = {
                            endTimeInput = it
                            viewModel.setLearningWindowEnd(it)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = skipDuringActive,
                            role = Role.Checkbox,
                            onValueChange = {
                                skipDuringActive = it
                                viewModel.setSkipDuringActiveWindow(it)
                            },
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Allow optional skip during window",
                        style = type.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Checkbox(
                        checked = skipDuringActive,
                        onCheckedChange = null,
                    )
                }
            }

            SettingsSection(
                title = "My learning topics",
                description = "Add the subjects you want the LLM to generate questions for. " +
                    "Removing a topic stops future generation for it.",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newTopicInput,
                        onValueChange = { newTopicInput = it },
                        label = { Text("Add custom topic") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalIconButton(
                        onClick = {
                            if (newTopicInput.isNotBlank()) {
                                val updated = selectedTopics.toMutableSet()
                                updated.add(newTopicInput.trim())
                                selectedTopics = updated
                                viewModel.setSelectedTopics(updated)
                                newTopicInput = ""
                            }
                        },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add topic")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val suggestedTopics = listOf(
                    "General Knowledge", "Technology", "History",
                    "Science", "Mathematics", "Computer Science", "Geography", "Literature",
                )
                Text(
                    text = "Suggested subjects (tap to toggle):",
                    style = type.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    suggestedTopics.forEach { suggested ->
                        val isAdded = selectedTopics.contains(suggested)
                        FilterChip(
                            selected = isAdded,
                            onClick = {
                                val updated = selectedTopics.toMutableSet()
                                if (isAdded) updated.remove(suggested) else updated.add(suggested)
                                selectedTopics = updated
                                viewModel.setSelectedTopics(updated)
                            },
                            label = { Text(suggested) },
                            leadingIcon = {
                                if (isAdded) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Active selected subjects:",
                    style = type.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (selectedTopics.isEmpty()) {
                    Text(
                        text = "No topics selected. Tap a subject above or add custom topics.",
                        style = type.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        selectedTopics.forEach { topic ->
                            AssistChip(
                                onClick = {
                                    val updated = selectedTopics.toMutableSet()
                                    updated.remove(topic)
                                    selectedTopics = updated
                                    viewModel.setSelectedTopics(updated)
                                },
                                label = { Text(topic) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove $topic",
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                            )
                        }
                    }
                }
            }

            SettingsSection(
                title = "Quiz & difficulty",
                description = "Configure question count per unlock and AI concept depth.",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Questions per quiz",
                        style = type.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface,
                    )
                    Text(
                        text = "$questionsCountInput " +
                            if (questionsCountInput == 1) "question" else "questions",
                        style = type.labelMedium,
                        color = colors.primary,
                    )
                }

                Slider(
                    value = questionsCountInput.toFloat(),
                    // Live UI updates while dragging; persist once on release so
                    // a single drag cannot enqueue dozens of DB operations.
                    onValueChange = { newValue ->
                        questionsCountInput = newValue.toInt().coerceIn(1, 5)
                    },
                    onValueChangeFinished = {
                        viewModel.setQuestionsPerQuiz(questionsCountInput)
                    },
                    valueRange = 1f..5f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Difficulty level",
                    style = type.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val levels = listOf("Easy", "Medium", "Hard")
                    levels.forEach { level ->
                        FilterChip(
                            selected = difficultyInput.equals(level, ignoreCase = true),
                            onClick = {
                                difficultyInput = level
                                viewModel.setDifficultyLevel(level)
                            },
                            label = { Text(level) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Question types", style = type.bodyMedium, fontWeight = FontWeight.Medium)
                Text("Choose the formats you want in unlock quizzes.", style = type.bodySmall, color = colors.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                val questionTypeLabels = listOf(
                    "MCQ" to "Multiple choice", "TRUE_FALSE" to "True / false",
                    "MULTI_SELECT" to "Multiple select", "FILL_BLANK" to "Fill in the blank",
                    "SHORT_ANSWER" to "Short answer", "DRAG_BLANK" to "Drag into blanks",
                    "MATCH_PAIRS" to "Match pairs",
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    questionTypeLabels.forEach { (key, label) ->
                        FilterChip(
                            selected = key in questionTypes,
                            onClick = {
                                if ((key in questionTypes) && questionTypes.size > 1) {
                                    questionTypes -= key
                                    viewModel.setQuestionTypes(questionTypes)
                                } else if (key !in questionTypes) {
                                    questionTypes = questionTypes + key
                                    viewModel.setQuestionTypes(questionTypes)
                                }
                            },
                            label = { Text(label) },
                            leadingIcon = if (key in questionTypes) {
                                { Icon(Icons.Default.Check, null) }
                            } else null,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Daily XP goal",
                        style = type.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface,
                    )
                    Text(
                        text = "$dailyGoal XP",
                        style = type.labelMedium,
                        color = colors.primary,
                    )
                }
                OutlinedButton(
                    onClick = { showGoalSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Choose daily goal")
                }
            }

            if (onStartTour != null) {
                SettingsSection(
                    title = "App onboarding & help",
                    description = "Revisit the step-by-step walkthrough of Unlock & Learn features.",
                ) {
                    OutlinedButton(
                        onClick = onStartTour,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompassCalibration,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Take interactive tour (30s)",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            var hasOverlayPermission by remember {
                mutableStateOf(Settings.canDrawOverlays(context))
            }
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        hasOverlayPermission = Settings.canDrawOverlays(context)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            SettingsSection(
                title = "Display over other apps permission",
                description = if (hasOverlayPermission) {
                    "Granted: Unlock & Learn can automatically pop up quiz concepts over your lock screen on unlock."
                } else {
                    "Required: Allows LearnLock to show questions when you unlock your phone."
                },
            ) {
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:${context.packageName}".toUri()
                            )
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Unable to open overlay settings")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (hasOverlayPermission) colors.primary else colors.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasOverlayPermission) "Overlay permission granted ✓" else "Manage overlay permission",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            SettingsSection(
                title = "Quick Settings tile",
                description = "Toggle the tutor on/off from the notification panel. " +
                    "Long-press the tile to pause it for a set time.",
            ) {
                OutlinedButton(
                    onClick = requestTileAdd,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add tutor toggle to Quick Settings",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Destructive zone stays a discrete card, visually separated.
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SettingsSectionHeader(
                        icon = Icons.Default.DeleteForever,
                        title = "Data & privacy",
                        description = "Learning data is stored locally on this device. " +
                            "Your API key and settings are kept.",
                        iconTint = colors.error,
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    val countsText = buildString {
                        append(counts.conceptCount?.let { "$it concepts" } ?: "…")
                        counts.historyCount?.let { append(" · $it quiz answers") }
                    }
                    Text(
                        text = "Currently stored: $countsText",
                        style = type.bodySmall,
                        color = colors.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.error,
                            contentColor = colors.onError,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete all learning data", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    }

    AppearanceBottomSheet(
        show = showAppearanceSheet,
        onDismiss = { showAppearanceSheet = false },
        themeMode = themeMode,
        dynamicColor = dynamicColor,
        onThemeModeSelected = { mode ->
            themeMode = mode
            viewModel.setThemeMode(mode)
            onThemeModeChange(mode)
        },
        onDynamicColorChanged = { enabled ->
            dynamicColor = enabled
            viewModel.setDynamicColor(enabled)
            onDynamicColorChange(enabled)
        },
    )

    GoalBottomSheet(
        show = showGoalSheet,
        onDismiss = { showGoalSheet = false },
        dailyGoal = dailyGoal,
        onGoalSelected = { goal ->
            dailyGoal = goal
            viewModel.setDailyXpGoal(goal)
        },
    )
}

@Composable
private fun SettingsSection(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = type.titleSmall,
            color = colors.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = type.bodySmall,
            color = colors.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
        )
        Text(
            text = title,
            style = type.titleSmall,
            color = colors.onSurface,
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = description,
        style = type.bodySmall,
        color = colors.onSurfaceVariant,
    )
}

/**
 * Read-only text field that stays focusable for accessibility services
 * (unlike a disabled field) and opens the platform time picker on tap.
 */
@Composable
private fun TimeField(
    label: String,
    placeholder: String,
    value: String,
    defaultHour: Int,
    defaultMinute: Int,
    onPicked: (formatted: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier.clickable {
            val (h, m) = try {
                val format = if (value.contains("AM") || value.contains("PM")) {
                    java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                } else {
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                }
                val date = format.parse(value)
                val cal = java.util.Calendar.getInstance().apply {
                    if (date != null) this.time = date
                }
                cal[java.util.Calendar.HOUR_OF_DAY] to cal[java.util.Calendar.MINUTE]
            } catch (_: Exception) {
                defaultHour to defaultMinute
            }

            android.app.TimePickerDialog(
                context,
                { _, hour, minute ->
                    val cal = java.util.Calendar.getInstance()
                    cal[java.util.Calendar.HOUR_OF_DAY] = hour
                    cal[java.util.Calendar.MINUTE] = minute
                    val formatted = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                        .format(cal.time)
                    onPicked(formatted)
                },
                h,
                m,
                false,
            ).show()
        },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearanceBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
) {
    if (!show) return
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text("Theme & colors", Modifier.padding(24.dp), style = MaterialTheme.typography.headlineSmall)
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    PickerOption(
                        title = when (mode) {
                            ThemeMode.SYSTEM -> "System"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                        },
                        description = when (mode) {
                            ThemeMode.SYSTEM -> "Follows your device theme"
                            ThemeMode.LIGHT -> "Always light theme"
                            ThemeMode.DARK -> "Always dark theme"
                        },
                        selected = themeMode == mode,
                        onClick = { onThemeModeSelected(mode) },
                    )
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 24.dp))
            val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ListItem(
                headlineContent = { Text("Dynamic colors") },
                supportingContent = { Text(if (supported) "Use colors from your wallpaper" else "Available on Android 12 and up") },
                trailingContent = { Switch(checked = dynamicColor && supported, onCheckedChange = null, enabled = supported) },
                modifier = Modifier.toggleable(value = dynamicColor, enabled = supported, role = Role.Switch, onValueChange = onDynamicColorChanged),
            )
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(horizontal = 16.dp)) { Text("Done") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoalBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    dailyGoal: Int,
    onGoalSelected: (Int) -> Unit,
) {
    if (!show) return
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).selectableGroup().padding(bottom = 24.dp)) {
            Text("Daily XP goal", Modifier.padding(24.dp), style = MaterialTheme.typography.headlineSmall)
            listOf(15 to "Light practice", 40 to "Standard daily goal", 60 to "Ambitious pace").forEach { (goal, description) ->
                PickerOption("$goal XP", description, dailyGoal == goal) {
                    onGoalSelected(goal)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun PickerOption(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    )
}

