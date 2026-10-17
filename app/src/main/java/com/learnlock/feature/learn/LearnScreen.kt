package com.learnlock.feature.learn

import com.learnlock.ui.components.GenerationProgress

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnlock.data.entity.QuestionHistory
import com.learnlock.data.remote.ai.ConceptGenerationState
import com.learnlock.ui.theme.star
import kotlinx.coroutines.launch

@Composable
fun LearnRoute(
    snackbarHostState: SnackbarHostState,
    onOpenDetail: (historyId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LearnViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LearnScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onOpenDetail = onOpenDetail,
        onToggleService = viewModel::setServiceEnabled,
        onGenerate = viewModel::generateConcepts,
        onToggleStar = viewModel::toggleStar,
        onGrantPermission = {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri(),
                )
                context.startActivity(intent)
            } catch (_: Exception) {
                scope.launch { snackbarHostState.showSnackbar("Unable to open overlay settings") }
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    state: LearnUiState,
    snackbarHostState: SnackbarHostState,
    onOpenDetail: (historyId: Long) -> Unit,
    onToggleService: (Boolean) -> Unit,
    onGenerate: () -> Unit,
    onToggleStar: (QuestionHistory) -> Unit,
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val recentHistory = state.recentHistory
    val generationState = state.generation

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        TopAppBar(
            title = { Text("Learn") },
            windowInsets = WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Primary task region: service toggle + manual generation.
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = state.serviceEnabled,
                                role = Role.Switch,
                                onValueChange = onToggleService,
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Unlock & Learn",
                                style = type.labelSmall,
                                color = colors.primary,
                            )
                            Text(
                                text = "Phone Unlock Tutor",
                                style = type.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                            )
                        }
                        Switch(
                            checked = state.serviceEnabled,
                            onCheckedChange = null,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (state.serviceEnabled) {
                            "Active: shows a learning concept card automatically when unlocking your device."
                        } else {
                            "Paused: enable to receive micro-quizzes on screen unlock."
                        },
                        style = type.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (generationState is ConceptGenerationState.Running) {
                        GenerationProgress(generationState.message)
                    }
                    Button(
                        onClick = onGenerate,
                        enabled = generationState !is ConceptGenerationState.Running,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        when (generationState) {
                            is ConceptGenerationState.Running -> { Text("Generating concepts…") }
                            is ConceptGenerationState.Success -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${generationState.count} concepts added", fontWeight = FontWeight.Bold)
                            }
                            is ConceptGenerationState.Error -> {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry generation", fontWeight = FontWeight.Bold)
                            }
                            ConceptGenerationState.Idle -> {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate new concepts", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (generationState is ConceptGenerationState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = generationState.message,
                            style = type.bodySmall,
                            color = colors.error,
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Column {
                            Text(
                                text = if (state.scheduleEnabled) {
                                    "Scheduled learning window"
                                } else {
                                    "Learning schedule off"
                                },
                                style = type.labelMedium,
                                color = colors.onSurfaceVariant,
                            )
                            Text(
                                text = if (state.scheduleEnabled) {
                                    "${state.windowStart} – ${state.windowEnd}"
                                } else {
                                    "Learn anytime"
                                },
                                style = type.titleSmall,
                                color = colors.onSurface,
                            )
                        }
                    }
                }
            }

            if (state.serviceEnabled && !state.hasOverlayPermission) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = colors.onErrorContainer,
                            )
                            Text(
                                text = "Permission Required",
                                style = type.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onErrorContainer,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "'Display over other apps' permission is required to show quiz concepts automatically when unlocking your device.",
                            style = type.bodyMedium,
                            color = colors.onErrorContainer,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onGrantPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.error,
                                contentColor = colors.onError,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Grant Permission in Settings", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recently viewed",
                    style = type.titleMedium,
                    color = colors.onSurface,
                )
                if (recentHistory != null) {
                    Text(
                        text = "${recentHistory.size} viewed",
                        style = type.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            when (recentHistory) {
                null -> {
                    // Loading: placeholders matching the final card geometry.
                    repeat(3) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth(0.4f)
                                        .height(14.dp)
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .background(colors.surfaceContainerHighest),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(14.dp)
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .background(colors.surfaceContainerHighest),
                                )
                            }
                        }
                    }
                }

                else -> if (recentHistory.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(36.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No concepts viewed yet",
                                style = type.titleSmall,
                                color = colors.onSurface,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "As you unlock your device and practice quizzes, your viewed concepts will appear here for quick review.",
                                style = type.bodyMedium,
                                color = colors.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    recentHistory.forEach { historyItem ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                            onClick = { onOpenDetail(historyItem.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = colors.primaryContainer,
                                    ) {
                                        Text(
                                            text = historyItem.topic,
                                            style = type.labelMedium,
                                            color = colors.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        )
                                    }

                                    IconButton(onClick = { onToggleStar(historyItem) }) {
                                        Icon(
                                            imageVector = if (historyItem.isStarred) {
                                                Icons.Default.Star
                                            } else {
                                                Icons.Outlined.StarBorder
                                            },
                                            contentDescription = if (historyItem.isStarred) {
                                                "Remove from favorites"
                                            } else {
                                                "Add to favorites"
                                            },
                                            tint = if (historyItem.isStarred) {
                                                colors.star
                                            } else {
                                                colors.onSurfaceVariant
                                            },
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = historyItem.conceptTitle,
                                    style = type.titleMedium,
                                    color = colors.onSurface,
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = historyItem.questionText,
                                    style = type.bodyMedium,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 2,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


