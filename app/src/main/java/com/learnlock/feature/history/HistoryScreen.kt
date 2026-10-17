package com.learnlock.feature.history

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnlock.feature.unlock.UnlockQuizActivity

import com.learnlock.ui.theme.star
import com.learnlock.ui.theme.success
import com.learnlock.ui.theme.warning
import com.learnlock.ui.theme.warningContainer
import com.learnlock.ui.theme.onWarningContainer
import com.learnlock.ui.theme.successContainer
import com.learnlock.ui.theme.onSuccessContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun HistoryRoute(
    snackbarHostState: SnackbarHostState,
    onOpenDetail: (historyId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    HistoryScreen(
        state = state,
        onOpenDetail = onOpenDetail,
        onClearHistory = {
            viewModel.clearHistory()
            scope.launch { snackbarHostState.showSnackbar("History cleared") }
        },
        onToggleStar = viewModel::toggleStar,
        onRetry = { entry ->
            val retryItem = entry.retryItem ?: return@HistoryScreen
            val intent = Intent(context, UnlockQuizActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(UnlockQuizActivity.EXTRA_RETRY_HISTORY_ID, retryItem.id)
            }
            context.startActivity(intent)
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onOpenDetail: (historyId: Long) -> Unit,
    onClearHistory: () -> Unit,
    onToggleStar: (HistoryEntry) -> Unit,
    onRetry: (HistoryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTopicFilter by rememberSaveable { mutableStateOf("All") }
    var selectedStatusFilter by rememberSaveable { mutableStateOf("All") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showClearConfirm by rememberSaveable { mutableStateOf(value = false) }

    val groupedHistory = state.groupedEntries
    val filteredHistory = remember(
        groupedHistory,
        selectedTopicFilter,
        selectedStatusFilter,
        searchQuery,
    ) {
        val normalizedQuery = searchQuery.trim()
        groupedHistory.filter { entry ->
            val item = entry.item
            val matchesTopic =
                (selectedTopicFilter == "All") || item.topic.equals(selectedTopicFilter, ignoreCase = true)
            val matchesStatus = when (selectedStatusFilter) {
                "PASSED" -> item.status == "PASSED"
                "RETRY_PENDING" -> item.status == "RETRY_PENDING"
                "GRADING_PENDING" -> item.status == "GRADING_PENDING"
                "STARRED" -> item.isStarred
                else -> true
            }
            val matchesSearch = normalizedQuery.isEmpty() ||
                item.conceptTitle.contains(normalizedQuery, ignoreCase = true) ||
                item.topic.contains(normalizedQuery, ignoreCase = true)
            matchesTopic && matchesStatus && matchesSearch
        }
    }

    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Delete all history?") },
            text = {
                Text("This permanently removes all recorded attempts. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        onClearHistory()
                    },
                ) {
                    Text("Delete", color = colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
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
            title = { Text("Stats & history") },
            actions = {
                if (state.allHistory.isNotEmpty()) {
                    IconButton(onClick = { showClearConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Delete all history",
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Question history", style = type.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${state.stats.correct} correct · ${state.stats.retry} to retry · ${state.stats.pending} pending",
                        style = type.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        items(state.stats.days, key = { it.daysAgo }) { dayStat ->
                            val cellColor = when (dayStat.bucket) {
                                DayBucket.EMPTY -> colors.surfaceContainerHighest
                                DayBucket.PENDING -> colors.warningContainer
                                DayBucket.ALL_CORRECT -> colors.successContainer
                                DayBucket.ALL_WRONG -> colors.errorContainer
                                DayBucket.MIXED -> colors.secondaryContainer
                            }
                            Surface(
                                color = cellColor,
                                contentColor = when (dayStat.bucket) {
                                    DayBucket.PENDING -> colors.onWarningContainer
                                    DayBucket.ALL_CORRECT -> colors.onSuccessContainer
                                    DayBucket.ALL_WRONG -> colors.onErrorContainer
                                    DayBucket.MIXED -> colors.onSecondaryContainer
                                    DayBucket.EMPTY -> colors.onSurfaceVariant
                                },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.size(30.dp).semantics {
                                    contentDescription = "${dayStat.count} question attempts " +
                                        if (dayStat.daysAgo == 0) "today" else "${dayStat.daysAgo} days ago"
                                },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (dayStat.count > 0) {
                                        Text(dayStat.count.coerceAtMost(9).toString(), style = type.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                    Text("Recent 14 days", style = type.labelSmall, color = colors.onSurfaceVariant)
                }
            }
            if (state.stats.topics.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Category performance", style = type.titleSmall, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(state.stats.topics) { topicStat ->
                        Surface(color = colors.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(topicStat.topic, style = type.labelMedium, color = colors.onSurface)
                                Text(
                                    "${topicStat.accuracyPercent}% · ${topicStat.count} answers",
                                    style = type.labelSmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search history") },
                placeholder = { Text("Concept or subject") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(state.topics, key = { it }) { topic ->
                    FilterChip(
                        selected = selectedTopicFilter == topic,
                        onClick = { selectedTopicFilter = topic },
                        label = { Text(topic) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val statusOptions = listOf("All", "PASSED", "RETRY_PENDING", "GRADING_PENDING", "STARRED")
                items(statusOptions, key = { it }) { status ->
                    val displayLabel = when (status) {
                        "PASSED" -> "Passed"
                        "RETRY_PENDING" -> "Retry pending"
                        "GRADING_PENDING" -> "Review pending"
                        "STARRED" -> "Starred"
                        else -> "All"
                    }
                    FilterChip(
                        selected = selectedStatusFilter == status,
                        onClick = { selectedStatusFilter = status },
                        label = { Text(displayLabel) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when {
                    state.loading -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(3) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = colors.surfaceContainerHigh,
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Spacer(
                                            modifier = Modifier
                                                .fillMaxWidth(0.5f)
                                                .height(14.dp)
                                                .clip(MaterialTheme.shapes.extraSmall)
                                                .background(colors.surfaceContainerHighest),
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Spacer(
                                            modifier = Modifier
                                                .fillMaxWidth(0.8f)
                                                .height(14.dp)
                                                .clip(MaterialTheme.shapes.extraSmall)
                                                .background(colors.surfaceContainerHighest),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    filteredHistory.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (state.allHistory.isEmpty()) {
                                    "No question attempts recorded yet.\nUnlock your phone to start learning!"
                                } else {
                                    if (searchQuery.isBlank()) {
                                        "No history matches your selected filters."
                                    } else {
                                        "No concepts match “${searchQuery.trim()}”."
                                    }
                                },
                                style = type.bodyMedium,
                                color = colors.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    else -> {
                        HistoryList(
                            historyList = filteredHistory,
                            onItemClick = { entry -> onOpenDetail(entry.item.id) },
                            onRetryItem = onRetry,
                            onToggleStar = onToggleStar,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryList(
    historyList: List<HistoryEntry>,
    onItemClick: (HistoryEntry) -> Unit,
    onRetryItem: (HistoryEntry) -> Unit,
    onToggleStar: (HistoryEntry) -> Unit,
) {
    val sdf = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            historyList,
            key = { "${it.item.topic}\u0000${it.item.conceptTitle}" },
        ) { entry ->
            val item = entry.item
            val isRetryPending = entry.retryItem != null
            val isReviewPending = item.status == "GRADING_PENDING"
            val statusColor = when {
                isRetryPending -> colors.error
                isReviewPending -> colors.warning
                else -> colors.success
            }
            val answerSegments = remember(item.id, item.userAnswer) {
                splitAnswerSegments(item.userAnswer)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                onClick = { onItemClick(entry) },
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = if (isRetryPending) Icons.Default.Warning else if (isReviewPending) {
                                    Icons.Default.Refresh
                                } else {
                                    Icons.Default.CheckCircle
                                },
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = when {
                                    isRetryPending -> "Retry pending"
                                    isReviewPending -> "Review pending"
                                    else -> "Passed"
                                },
                                style = type.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onToggleStar(entry) }) {
                                Icon(
                                    imageVector = if (item.isStarred) {
                                        Icons.Default.Star
                                    } else {
                                        Icons.Outlined.StarBorder
                                    },
                                    contentDescription = if (item.isStarred) {
                                        "Remove from favorites"
                                    } else {
                                        "Add to favorites"
                                    },
                                    tint = if (item.isStarred) colors.star else colors.onSurfaceVariant,
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = sdf.format(Date(item.answeredAt)),
                                style = type.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = item.conceptTitle,
                        style = type.titleSmall,
                        color = colors.onSurface,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.questionText,
                        style = type.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )

                    if (entry.attemptCount > 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${entry.attemptCount} attempts",
                            style = type.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (answerSegments.size > 1) "Your answers:" else "Your answer:",
                        style = type.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    answerSegments.forEach { segment ->
                        Text(
                            text = segment,
                            style = type.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                        )
                    }
                    if ((!item.isCorrect) && answerSegments.size == 1) {
                        Text(
                            text = "Expected: ${formatAnswerText(item.correctAnswer, item.optionsJson)}",
                            style = type.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }

                    if (item.explanation.isNotBlank() && answerSegments.size == 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.explanation,
                            style = type.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }

                    if (isRetryPending) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { onRetryItem(entry) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry question", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private val answerSegmentPattern = Regex("^Q\\d+:\\s*")

private fun splitAnswerSegments(userAnswer: String): List<String> {
    val parts = userAnswer.split(" | ")
    if (parts.size > 1 && parts.all { answerSegmentPattern.containsMatchIn(it) }) {
        return parts.map { answerSegmentPattern.replace(it, "") }
    }
    return listOf(userAnswer)
}

private fun formatAnswerText(answerRaw: String, optionsJson: String?): String {
    if (answerRaw.isBlank()) return answerRaw
    if (!optionsJson.isNullOrBlank()) {
        try {
            val array = org.json.JSONArray(optionsJson)
            val optionsList = mutableListOf<String>()
            for (i in 0 until array.length()) {
                optionsList.add(array.getString(i))
            }
            val idx = answerRaw.trim().toIntOrNull()
            if (idx != null && idx in optionsList.indices) {
                return optionsList[idx]
            }
            val letterIdx = when (answerRaw.trim().uppercase()) {
                "A" -> 0
                "B" -> 1
                "C" -> 2
                "D" -> 3
                else -> -1
            }
            if (letterIdx != -1 && letterIdx in optionsList.indices) {
                return optionsList[letterIdx]
            }
        } catch (_: Exception) {
            // fallback
        }
    }
    return answerRaw
}


