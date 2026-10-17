package com.learnlock.feature.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnlock.data.model.QuizSummary
import com.learnlock.data.remote.ai.ConceptGenerationState
import com.learnlock.feature.unlock.UnlockQuizActivity
import com.learnlock.ui.theme.AppMotion
import com.learnlock.ui.theme.success
import com.learnlock.ui.theme.successContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeRoute(
    onNavigateToHistory: () -> Unit,
    onOpenQuizDetail: (quizId: Long, isAttempted: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    HomeScreen(
        state = state,
        onToggleLearnLock = viewModel::setLearnLockEnabled,
        onGenerateQuiz = viewModel::generateQuiz,
        onDismissGenerationMessage = viewModel::clearGenerationResult,
        onNavigateToHistory = onNavigateToHistory,
        onOpenQuizDetail = onOpenQuizDetail,
        onStartQuiz = { quizId ->
            val intent = Intent(context, UnlockQuizActivity::class.java).apply {
                if (quizId != null) {
                    putExtra(UnlockQuizActivity.EXTRA_CONCEPT_ID, quizId)
                }
            }
            context.startActivity(intent)
        },
        onGrantOverlayPermission = {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri(),
                )
                context.startActivity(intent)
            } catch (_: Exception) {}
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onToggleLearnLock: (Boolean) -> Unit = {},
    onGenerateQuiz: () -> Unit = {},
    onDismissGenerationMessage: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onOpenQuizDetail: (quizId: Long, isAttempted: Boolean) -> Unit = { _, _ -> },
    onStartQuiz: (quizId: Long?) -> Unit = {},
    onGrantOverlayPermission: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = AppMotion.progress,
        label = "Daily Progress",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("LearnLock", fontWeight = FontWeight.Bold, style = type.titleLarge)
                }
            },
            actions = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.primaryContainer,
                    modifier = Modifier.padding(end = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "${state.totalXp} XP",
                            style = type.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimaryContainer,
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 3.1 LearnLock Enable/Disable Control Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isEffectivelyActive) colors.surfaceContainerHighest else colors.surfaceContainerHigh,
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = state.isLearnLockEnabled,
                                role = Role.Switch,
                                onValueChange = onToggleLearnLock,
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (state.isEffectivelyActive) colors.primary else colors.surfaceContainerHighest,
                                modifier = Modifier.size(42.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (state.isEffectivelyActive) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = if (state.isEffectivelyActive) colors.onPrimary else colors.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "LearnLock",
                                    style = type.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface,
                                )
                                Text(
                                    text = if (state.isEffectivelyActive) "Status: Enabled" else "Status: Disabled",
                                    style = type.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (state.isEffectivelyActive) colors.primary else colors.onSurfaceVariant,
                                )
                            }
                        }

                        Switch(
                            checked = state.isLearnLockEnabled,
                            onCheckedChange = null,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (state.isLearnLockEnabled) {
                            "LearnLock automatically triggers micro-quizzes when unlocking your phone to reinforce your learning."
                        } else {
                            "LearnLock quiz triggers are paused. You can still practice quizzes manually anytime."
                        },
                        style = type.bodySmall,
                        color = colors.onSurfaceVariant,
                    )

                    // Overlay permission warning if enabled but permission missing
                    if (state.isLearnLockEnabled && !state.hasOverlayPermission) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.errorContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = colors.error,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = "Permission Required",
                                        style = type.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onErrorContainer,
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "'Display over other apps' is needed to present quizzes on unlock.",
                                    style = type.bodySmall,
                                    color = colors.onErrorContainer,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onGrantOverlayPermission,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.error,
                                        contentColor = colors.onError,
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Grant Permission", style = type.labelLarge)
                                }
                            }
                        }
                    }
                }
            }

            // 3.2 Generate Quiz Button
            val isGenerating = state.generationState is ConceptGenerationState.Running
            Button(
                onClick = onGenerateQuiz,
                enabled = !isGenerating,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = colors.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Generating Quiz…", style = type.labelLarge, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Generate Quiz", style = type.labelLarge, fontWeight = FontWeight.Bold)
                }
            }

            // Generation feedback banner
            when (val gen = state.generationState) {
                is ConceptGenerationState.Success -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.successContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.success, modifier = Modifier.size(18.dp))
                                Text("${gen.count} quizzes generated & ready!", style = type.bodyMedium, color = colors.onSurface)
                            }
                            IconButton(onClick = onDismissGenerationMessage, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                is ConceptGenerationState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.error, modifier = Modifier.size(18.dp))
                                Text(gen.message, style = type.bodySmall, color = colors.onErrorContainer)
                            }
                            IconButton(onClick = onDismissGenerationMessage, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                else -> {}
            }

            // 3.3 Today's Progress & 3.4 Streak Display
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Today's Progress",
                                style = type.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                            )
                            Text(
                                text = "${state.todayCompletedQuizzes} of ${state.dailyQuizGoal} quizzes completed",
                                style = type.bodyMedium,
                                color = colors.onSurfaceVariant,
                            )
                        }

                        // Streak chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (state.currentStreak > 0) colors.errorContainer else colors.surfaceContainerHighest,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = "Streak",
                                    tint = if (state.currentStreak > 0) colors.error else colors.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "${state.currentStreak} day${if (state.currentStreak == 1) "" else "s"}",
                                    style = type.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.currentStreak > 0) colors.onErrorContainer else colors.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = colors.primary,
                        trackColor = colors.surfaceContainerHighest,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = state.motivationalMessage,
                            style = type.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${state.progressPercent}%",
                            style = type.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                        )
                    }
                }
            }

            // 3.5 Recent Quizzes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent Quizzes",
                    style = type.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
                TextButton(onClick = onNavigateToHistory) {
                    Text("View All", style = type.labelLarge, color = colors.primary)
                }
            }

            if (state.recentQuizzes.isEmpty()) {
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No quizzes yet",
                            style = type.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Generate Quiz' above to create your first micro-quiz!",
                            style = type.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            } else {
                state.recentQuizzes.forEach { quiz ->
                    RecentQuizCard(
                        quiz = quiz,
                        onClick = { onOpenQuizDetail(quiz.id, quiz.isAttempted) },
                        onAttempt = { onStartQuiz(quiz.conceptId ?: quiz.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentQuizCard(
    quiz: QuizSummary,
    onClick: () -> Unit,
    onAttempt: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val sdf = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.secondaryContainer,
                ) {
                    Text(
                        text = quiz.topic,
                        style = type.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }

                Text(
                    text = sdf.format(Date(quiz.date)),
                    style = type.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = quiz.title,
                style = type.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${quiz.questionCount} Questions · ${quiz.difficulty}",
                    style = type.bodySmall,
                    color = colors.onSurfaceVariant,
                )

                if (quiz.isAttempted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.successContainer,
                    ) {
                        Text(
                            text = "Score: ${quiz.score}/${quiz.maxScore}",
                            style = type.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                } else {
                    Button(
                        onClick = onAttempt,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Attempt", style = type.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
