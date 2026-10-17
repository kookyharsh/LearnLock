package com.learnlock.feature.settings

import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.learnlock.feature.unlock.UnlockOverlayService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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

    var isLearnLockEnabled by rememberSaveable { mutableStateOf(initial.isLearnLockEnabled) }
    var apiKeyInput by rememberSaveable { mutableStateOf(initial.apiKey) }
    var customModelInput by rememberSaveable { mutableStateOf(initial.customModel) }
    var isApiKeyVisible by rememberSaveable { mutableStateOf(false) }

    var isTestingKey by rememberSaveable { mutableStateOf(false) }
    var keyTestError by rememberSaveable { mutableStateOf<String?>(null) }

    var windowEnabled by rememberSaveable { mutableStateOf(initial.learningWindowEnabled) }
    var startTimeInput by rememberSaveable { mutableStateOf(initial.learningWindowStart) }
    var endTimeInput by rememberSaveable { mutableStateOf(initial.learningWindowEnd) }

    var selectedTopics by remember { mutableStateOf(initial.selectedTopics) }
    var newTopicInput by rememberSaveable { mutableStateOf("") }

    var dailyQuizGoal by rememberSaveable { mutableIntStateOf(initial.dailyQuizGoal) }
    var questionsCountInput by rememberSaveable { mutableIntStateOf(initial.questionsPerQuiz) }
    var difficultyInput by rememberSaveable { mutableStateOf(initial.difficultyLevel) }
    var questionTypes by remember { mutableStateOf(initial.questionTypes) }
    var themeMode by rememberSaveable { mutableStateOf(initial.themeMode) }
    var dynamicColor by rememberSaveable { mutableStateOf(viewModel.isDynamicColor()) }

    var showAppearanceSheet by rememberSaveable { mutableStateOf(false) }
    var showGoalSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

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
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete all learning data?") },
            text = {
                Text("This permanently deletes every saved quiz and quiz answer from your device. Your API key and preferences will be preserved.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        coroutineScope.launch {
                            try {
                                viewModel.deleteAllData()
                                snackbarHostState.showSnackbar("All learning data deleted")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Failed to delete data: ${e.message}")
                            }
                        }
                    },
                ) {
                    Text("Delete", color = colors.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        TopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.Bold) },
            windowInsets = WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Group 1: LearnLock Settings
            SettingsGroup(title = "LearnLock Service") {
                // Shared LearnLock toggle
                SettingSwitchItem(
                    icon = Icons.Default.Lock,
                    title = "Enable LearnLock",
                    description = if (isLearnLockEnabled) "Quizzes trigger when unlocking your phone" else "Automatic lock-screen triggering is paused",
                    checked = isLearnLockEnabled,
                    onCheckedChange = { checked ->
                        isLearnLockEnabled = checked
                        viewModel.setLearnLockEnabled(checked)
                        if (checked) {
                            UnlockOverlayService.start(context)
                        } else {
                            UnlockOverlayService.stop(context)
                        }
                        TutorTileService.refresh(context)
                    },
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Permission Status
                SettingActionItem(
                    icon = if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                    iconTint = if (hasOverlayPermission) colors.primary else colors.error,
                    title = "Display over other apps",
                    description = if (hasOverlayPermission) "Permission granted ✓" else "Permission required to show quizzes on lock-screen",
                    onClick = {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:${context.packageName}".toUri(),
                            )
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Unable to open overlay settings") }
                        }
                    },
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Learning window schedule toggle
                SettingSwitchItem(
                    icon = Icons.Default.Schedule,
                    title = "Schedule learning window",
                    description = if (windowEnabled) "Quizzes only trigger between $startTimeInput and $endTimeInput" else "Quizzes trigger anytime during unlock",
                    checked = windowEnabled,
                    onCheckedChange = {
                        windowEnabled = it
                        viewModel.setLearningWindowEnabled(it)
                    },
                )

                if (windowEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TimePickerField(
                            label = "Start time",
                            value = startTimeInput,
                            onPicked = {
                                startTimeInput = it
                                viewModel.setLearningWindowStart(it)
                            },
                            modifier = Modifier.weight(1f),
                        )
                        TimePickerField(
                            label = "End time",
                            value = endTimeInput,
                            onPicked = {
                                endTimeInput = it
                                viewModel.setLearningWindowEnd(it)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Group 2: Quiz Preferences
            SettingsGroup(title = "Quiz Preferences") {
                SettingActionItem(
                    icon = Icons.Default.Quiz,
                    title = "Daily Quiz Goal",
                    description = "$dailyQuizGoal quizzes per day",
                    onClick = { showGoalSheet = true },
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Question count per quiz slider
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Questions per quiz", style = type.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("$questionsCountInput questions", style = type.labelMedium, color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = questionsCountInput.toFloat(),
                        onValueChange = { questionsCountInput = it.toInt().coerceIn(1, 5) },
                        onValueChangeFinished = { viewModel.setQuestionsPerQuiz(questionsCountInput) },
                        valueRange = 1f..5f,
                        steps = 3,
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Difficulty chips
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Default difficulty", style = type.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("Easy", "Medium", "Hard").forEach { level ->
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
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Question types
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Question Types", style = type.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Select which formats to include in generated quizzes", style = type.bodySmall, color = colors.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    val typeMap = listOf(
                        "MCQ" to "Multiple Choice",
                        "TRUE_FALSE" to "True / False",
                        "MULTI_SELECT" to "Multiple Select",
                        "FILL_BLANK" to "Fill in Blank",
                        "SHORT_ANSWER" to "Short Answer",
                        "DRAG_BLANK" to "Drag and Drop",
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        typeMap.forEach { (key, label) ->
                            val isSelected = key in questionTypes
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected && questionTypes.size > 1) {
                                        questionTypes -= key
                                        viewModel.setQuestionTypes(questionTypes)
                                    } else if (!isSelected) {
                                        questionTypes += key
                                        viewModel.setQuestionTypes(questionTypes)
                                    }
                                },
                                label = { Text(label) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null,
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Learning topics
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Learning Topics", style = type.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = newTopicInput,
                            onValueChange = { newTopicInput = it },
                            placeholder = { Text("Add custom topic") },
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
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                },
                            )
                        }
                    }
                }
            }

            // Group 3: LLM API Credentials
            SettingsGroup(title = "AI Configuration") {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            viewModel.setApiKey(it)
                            keyTestError = null
                        },
                        label = { Text("API Key / Token") },
                        placeholder = { Text("sk-or-v1-... or AIzaSy...") },
                        supportingText = { Text(keyTestError ?: "Stored encrypted locally on this device.") },
                        isError = keyTestError != null,
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = customModelInput,
                        onValueChange = {
                            customModelInput = it
                            viewModel.setCustomModel(it)
                        },
                        label = { Text("Model Name (Optional)") },
                        placeholder = { Text("Default: gemini-1.5-flash or gpt-4o-mini") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isTestingKey = true
                                keyTestError = null
                                try {
                                    val (ok, msg) = viewModel.testApiKeyConnection()
                                    if (ok) {
                                        snackbarHostState.showSnackbar(msg)
                                    } else {
                                        keyTestError = msg
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
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing…")
                        } else {
                            Icon(Icons.Default.NetworkCheck, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test API Connection", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Group 4: Appearance
            SettingsGroup(title = "Appearance") {
                SettingActionItem(
                    icon = Icons.Default.Palette,
                    title = "Theme & Colors",
                    description = when (themeMode) {
                        ThemeMode.SYSTEM -> "System Default"
                        ThemeMode.LIGHT -> "Light Theme"
                        ThemeMode.DARK -> "Dark Theme"
                    } + if (dynamicColor) " · Dynamic" else "",
                    onClick = { showAppearanceSheet = true },
                )
            }

            // Group 5: Quick Settings & Notifications
            SettingsGroup(title = "Quick Settings") {
                SettingActionItem(
                    icon = Icons.Default.AddCircle,
                    title = "Quick Settings Tutor Tile",
                    description = "Toggle LearnLock directly from notification shade",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val statusBar = context.getSystemService(android.app.StatusBarManager::class.java)
                            statusBar?.requestAddTileService(
                                ComponentName(context, TutorTileService::class.java),
                                context.getString(R.string.tile_label),
                                android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_tile_tutor),
                                context.mainExecutor,
                            ) { }
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Swipe down your notification shade and edit tiles to add LearnLock.")
                            }
                        }
                    },
                )
            }

            // Group 6: Data & Storage
            SettingsGroup(title = "Data & Storage") {
                val storedText = buildString {
                    append(counts.conceptCount?.let { "$it concepts" } ?: "…")
                    counts.historyCount?.let { append(" · $it quiz attempts") }
                }
                SettingActionItem(
                    icon = Icons.Default.Storage,
                    title = "Stored Data",
                    description = "Locally stored: $storedText",
                    onClick = null,
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingActionItem(
                    icon = Icons.Default.DeleteForever,
                    iconTint = colors.error,
                    title = "Delete All Learning Data",
                    description = "Permanently removes all saved quizzes and history",
                    onClick = { showDeleteConfirm = true },
                )
            }

            // Group 7: About
            SettingsGroup(title = "About") {
                SettingActionItem(
                    icon = Icons.Default.Info,
                    title = "LearnLock",
                    description = "Version 2.0",
                    onClick = null,
                )
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

    DailyQuizGoalBottomSheet(
        show = showGoalSheet,
        onDismiss = { showGoalSheet = false },
        currentGoal = dailyQuizGoal,
        onGoalSelected = { goal ->
            dailyQuizGoal = goal
            viewModel.setDailyQuizGoal(goal)
        },
    )
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = type.labelLarge,
            fontWeight = FontWeight.Bold,
            color = colors.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingSwitchItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f),
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
            Column {
                Text(title, style = type.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                Text(description, style = type.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingActionItem(
    icon: ImageVector,
    title: String,
    description: String,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = type.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            Text(description, style = type.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun TimePickerField(
    label: String,
    value: String,
    onPicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier.clickable {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                context,
                { _, h, m ->
                    cal.set(Calendar.HOUR_OF_DAY, h)
                    cal.set(Calendar.MINUTE, m)
                    val formatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
                    onPicked(formatted)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                false,
            ).show()
        },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            trailingIcon = { Icon(Icons.Default.Schedule, null) },
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Text("Theme & Colors", modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Column(modifier = Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    ListItem(
                        headlineContent = {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> "System Default"
                                    ThemeMode.LIGHT -> "Light Theme"
                                    ThemeMode.DARK -> "Dark Theme"
                                }
                            )
                        },
                        leadingContent = {
                            RadioButton(selected = themeMode == mode, onClick = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = themeMode == mode, role = Role.RadioButton) {
                                onThemeModeSelected(mode)
                            },
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ListItem(
                headlineContent = { Text("Dynamic colors") },
                supportingContent = { Text(if (supported) "Palette derived from device wallpaper" else "Available on Android 12+") },
                trailingContent = { Switch(checked = dynamicColor && supported, onCheckedChange = null, enabled = supported) },
                modifier = Modifier.toggleable(value = dynamicColor, enabled = supported, role = Role.Switch, onValueChange = onDynamicColorChanged),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DailyQuizGoalBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    currentGoal: Int,
    onGoalSelected: (Int) -> Unit,
) {
    if (!show) return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .selectableGroup()
                .padding(bottom = 24.dp),
        ) {
            Text("Daily Quiz Goal", modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            listOf(
                3 to "Light Practice (3 quizzes)",
                5 to "Recommended Daily Pace (5 quizzes)",
                8 to "Dedicated Learner (8 quizzes)",
                10 to "Intensive Challenge (10 quizzes)",
            ).forEach { (goal, desc) ->
                ListItem(
                    headlineContent = { Text("$goal Quizzes") },
                    supportingContent = { Text(desc) },
                    leadingContent = { RadioButton(selected = currentGoal == goal, onClick = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = currentGoal == goal, role = Role.RadioButton) {
                            onGoalSelected(goal)
                            onDismiss()
                        },
                )
            }
        }
    }
}
