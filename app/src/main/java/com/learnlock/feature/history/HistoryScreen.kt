package com.learnlock.feature.history

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnlock.data.model.QuizSummary
import com.learnlock.ui.theme.success
import com.learnlock.ui.theme.successContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryRoute(
    onOpenQuizDetail: (quizId: Long, isAttempted: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HistoryScreen(
        state = state,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onFilterChanged = viewModel::onFilterChanged,
        onOpenQuizDetail = onOpenQuizDetail,
        onClearAll = viewModel::clearAllHistory,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onSearchQueryChanged: (String) -> Unit,
    onFilterChanged: (QuizFilter) -> Unit,
    onOpenQuizDetail: (quizId: Long, isAttempted: Boolean) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All History?") },
            text = { Text("This will permanently remove all quiz attempts and saved quizzes from your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearAll()
                    },
                ) {
                    Text("Delete All", color = colors.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
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
            title = { Text("Quiz History", fontWeight = FontWeight.Bold) },
            actions = {
                if (state.allQuizzes.isNotEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all history")
                    }
                }
            },
            windowInsets = WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            // 4.2 Functional Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Search by quiz title or topic…") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = colors.onSurfaceVariant)
                },
                trailingIcon = if (state.searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4.3 Filter Chips: All, Attempted, Not Attempted
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuizFilter.entries.forEach { filter ->
                    val label = when (filter) {
                        QuizFilter.ALL -> "All (${state.allQuizzes.size})"
                        QuizFilter.ATTEMPTED -> "Attempted (${state.allQuizzes.count { it.isAttempted }})"
                        QuizFilter.NOT_ATTEMPTED -> "Not Attempted (${state.allQuizzes.count { !it.isAttempted }})"
                    }
                    FilterChip(
                        selected = state.activeFilter == filter,
                        onClick = { onFilterChanged(filter) },
                        label = { Text(label) },
                        shape = RoundedCornerShape(10.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4.1 History List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when {
                    state.loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.primary)
                        }
                    }
                    state.filteredQuizzes.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = colors.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (state.allQuizzes.isEmpty()) {
                                        "No quizzes found.\nGenerate your first quiz from the Home screen!"
                                    } else {
                                        "No quizzes match your filter or search query."
                                    },
                                    style = type.bodyMedium,
                                    color = colors.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(state.filteredQuizzes, key = { "${it.isAttempted}_${it.id}" }) { quiz ->
                                HistoryQuizItem(
                                    quiz = quiz,
                                    onClick = { onOpenQuizDetail(quiz.id, quiz.isAttempted) },
                                )
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryQuizItem(
    quiz: QuizSummary,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
        shape = RoundedCornerShape(14.dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.primaryContainer,
                ) {
                    Text(
                        text = quiz.topic,
                        style = type.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                style = type.titleMedium,
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.success, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Completed · ${quiz.score}/${quiz.maxScore}",
                                style = type.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.surfaceContainerHighest,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Not Attempted",
                                style = type.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
