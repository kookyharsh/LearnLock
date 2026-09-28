package com.example.ui.screens

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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.example.data.AppDatabase
import com.example.data.entity.QuestionHistory
import com.example.service.UnlockQuizActivity
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldStar
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    onOpenDetail: (historyId: Long) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    var selectedTopicFilter by rememberSaveable { mutableStateOf("All") }
    var selectedStatusFilter by rememberSaveable { mutableStateOf("All") }
    var showClearConfirm by rememberSaveable { mutableStateOf(false) }

    // Load the full history: filters and the destructive clear action must see
    // the same data the user sees, not an arbitrary 10-item window.
    val allHistory by db.historyDao().getAllHistory().collectAsState(initial = null)

    val filteredHistory = remember(allHistory, selectedTopicFilter, selectedStatusFilter) {
        (allHistory ?: emptyList()).filter { item ->
            val matchesTopic =
                selectedTopicFilter == "All" || item.topic.equals(selectedTopicFilter, ignoreCase = true)
            val matchesStatus = when (selectedStatusFilter) {
                "PASSED" -> item.status == "PASSED"
                "RETRY_PENDING" -> item.status == "RETRY_PENDING"
                "STARRED" -> item.isStarred
                else -> true
            }
            matchesTopic && matchesStatus
        }
    }

    val topicsList = remember(allHistory) {
        listOf("All") + (allHistory ?: emptyList()).map { it.topic }.distinct()
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
                        coroutineScope.launch {
                            db.historyDao().clearAllHistory()
                            snackbarHostState.showSnackbar("History cleared")
                        }
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
            title = { Text("History") },
            actions = {
                if (!allHistory.isNullOrEmpty()) {
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
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(topicsList, key = { it }) { topic ->
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
                val statusOptions = listOf("All", "PASSED", "RETRY_PENDING", "STARRED")
                items(statusOptions, key = { it }) { status ->
                    val displayLabel = when (status) {
                        "PASSED" -> "Passed"
                        "RETRY_PENDING" -> "Retry pending"
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
                    allHistory == null -> {
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
                                text = if (allHistory!!.isEmpty()) {
                                    "No question attempts recorded yet.\nUnlock your phone to start learning!"
                                } else {
                                    "No history matches your selected filters."
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
                            onItemClick = { historyItem -> onOpenDetail(historyItem.id) },
                            onRetryItem = {
                                val intent = Intent(context, UnlockQuizActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                        ) { historyItem ->
                            val newStar = !historyItem.isStarred
                            coroutineScope.launch {
                                db.historyDao().updateStarStatus(historyItem.id, newStar)
                                db.conceptDao().updateStarStatusByTitle(historyItem.conceptTitle, newStar)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryList(
    historyList: List<QuestionHistory>,
    onItemClick: (QuestionHistory) -> Unit,
    onRetryItem: (QuestionHistory) -> Unit,
    onToggleStar: (QuestionHistory) -> Unit,
) {
    val sdf = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(historyList, key = { it.id }) { item ->
            val statusColor = if (item.isCorrect) SuccessGreen else ErrorRed
            val answerSegments = remember(item.id, item.userAnswer) {
                splitAnswerSegments(item.userAnswer)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onItemClick(item) },
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Status is icon + text, never color alone.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = if (item.isCorrect) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.Warning
                                },
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (item.isCorrect) "Passed" else "Retry pending",
                                style = type.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onToggleStar(item) }) {
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
                                    tint = if (item.isStarred) GoldStar else colors.onSurfaceVariant,
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = colors.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
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
                            // For single-question attempts the expected answer is
                            // unambiguous; for multi-question quizzes the correct
                            // answers live on the detail screen per question.
                            if (!item.isCorrect && answerSegments.size == 1) {
                                Text(
                                    text = "Expected: ${formatAnswerText(item.correctAnswer, item.optionsJson)}",
                                    style = type.bodyMedium,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (item.explanation.isNotBlank() && answerSegments.size == 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.explanation,
                            style = type.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }

                    if (!item.isCorrect) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { onRetryItem(item) },
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

/** Splits a joined multi-question answer ("Q1: x | Q2: y") into segments. */
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
