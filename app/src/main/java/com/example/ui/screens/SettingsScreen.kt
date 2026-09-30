package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.preferences.AppPreferencesManager
import com.example.data.preferences.ThemeMode
import com.example.service.ConceptGenerator
import com.example.service.TutorTileService
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    onStartTour: (() -> Unit)? = null,
    onThemeModeChange: (ThemeMode) -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefsManager = remember { AppPreferencesManager(context) }

    var apiKeyInput by rememberSaveable { mutableStateOf(prefsManager.getApiKey()) }
    var customModelInput by rememberSaveable { mutableStateOf(prefsManager.getCustomModel()) }
    var isApiKeyVisible by rememberSaveable { mutableStateOf(value = false) }

    var isTestingKey by rememberSaveable { mutableStateOf(false) }
    var keyTestError by rememberSaveable { mutableStateOf<String?>(null) }

    var windowEnabled by rememberSaveable { mutableStateOf(prefsManager.isLearningWindowEnabled()) }
    var startTimeInput by rememberSaveable { mutableStateOf(prefsManager.getLearningWindowStart()) }
    var endTimeInput by rememberSaveable { mutableStateOf(prefsManager.getLearningWindowEnd()) }
    var skipDuringActive by rememberSaveable { mutableStateOf(prefsManager.isSkipDuringActiveWindow()) }

    // Backed by prefs on every change, so a plain remember re-reading prefs is
    // equivalent after recreation.
    var selectedTopics by remember { mutableStateOf(prefsManager.getSelectedTopics()) }
    var newTopicInput by rememberSaveable { mutableStateOf("") }

    var questionsCountInput by rememberSaveable { mutableIntStateOf(prefsManager.getQuestionsPerQuiz()) }
    var difficultyInput by rememberSaveable { mutableStateOf(prefsManager.getDifficultyLevel()) }
    var themeMode by rememberSaveable { mutableStateOf(prefsManager.getThemeMode()) }

    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var conceptCount by remember { mutableStateOf<Int?>(null) }
    var historyCount by remember { mutableStateOf<Int?>(null) }

    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    LaunchedEffect(Unit) {
        try {
            val db = com.example.data.AppDatabase.getDatabase(context)
            conceptCount = db.conceptDao().getConceptCount()
            historyCount = db.historyDao().getHistoryCount()
        } catch (_: Exception) {
            // Counts are informational; the screen works without them.
        }
    }

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
                val db = com.example.data.AppDatabase.getDatabase(context)
                db.conceptDao().clearAllConcepts()
                db.historyDao().clearAllHistory()
                conceptCount = 0
                historyCount = 0
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        TopAppBar(
            title = { Text("Settings") },
            actions = {
                if (onStartTour != null) {
                    IconButton(onClick = onStartTour) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = {
                                themeMode = mode
                                prefsManager.setThemeMode(mode)
                                onThemeModeChange(mode)
                            },
                            label = {
                                Text(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> "System"
                                        ThemeMode.LIGHT -> "Light"
                                        ThemeMode.DARK -> "Dark"
                                    },
                                )
                            },
                            leadingIcon = if (themeMode == mode) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            } else null,
                            modifier = Modifier.weight(1f),
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
                            prefsManager.setApiKey(it)
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
                            prefsManager.setCustomModel(it)
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
                                    val generator = ConceptGenerator(prefsManager)
                                    val (ok, message) = generator.testApiKeyConnection()
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
                                prefsManager.setLearningWindowEnabled(it)
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
                            prefsManager.setLearningWindowStart(it)
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
                            prefsManager.setLearningWindowEnd(it)
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
                                prefsManager.setSkipDuringActiveWindow(it)
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
                                prefsManager.setSelectedTopics(updated)
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
                    "Science", "Mathematics", "Computer Science", "Geography", "Literature"
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
                                prefsManager.setSelectedTopics(updated)
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
                                    prefsManager.setSelectedTopics(updated)
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
                        prefsManager.setQuestionsPerQuiz(questionsCountInput)
                        coroutineScope.launch {
                            try {
                                com.example.data.AppDatabase.getDatabase(context)
                                    .conceptDao().clearUnusedConcepts()
                            } catch (_: Exception) {
                                // Cleanup is best-effort.
                            }
                        }
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
                                prefsManager.setDifficultyLevel(level)
                                coroutineScope.launch {
                                    try {
                                        com.example.data.AppDatabase.getDatabase(context)
                                            .conceptDao().clearUnusedConcepts()
                                    } catch (_: Exception) {
                                        // Cleanup is best-effort.
                                    }
                                }
                            },
                            label = { Text(level) },
                            modifier = Modifier.weight(1f),
                        )
                    }
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
                    val counts = buildString {
                        append(conceptCount?.let { "$it concepts" } ?: "…")
                        historyCount?.let { append(" · $it quiz answers") }
                    }
                    Text(
                        text = "Currently stored: $counts",
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
                cal.get(java.util.Calendar.HOUR_OF_DAY) to cal.get(java.util.Calendar.MINUTE)
            } catch (_: Exception) {
                defaultHour to defaultMinute
            }

            android.app.TimePickerDialog(
                context,
                { _, hour, minute ->
                    val cal = java.util.Calendar.getInstance()
                    cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
                    cal.set(java.util.Calendar.MINUTE, minute)
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
