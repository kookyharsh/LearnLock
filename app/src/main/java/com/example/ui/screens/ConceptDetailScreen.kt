package com.example.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.QuestionHistory
import com.example.ui.components.MarkdownView
import com.example.ui.quiz.QuizQuestion
import com.example.ui.quiz.isValidSnippet
import com.example.ui.quiz.parseQuestionsList
import com.example.ui.theme.GoldStar
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConceptDetailScreen(
    item: QuestionHistory,
    onBack: () -> Unit,
    onStarToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keyed on the item so navigating from one concept to another never
    // shows a stale star toggle.
    var isStarred by remember(item.id, item.isStarred) { mutableStateOf(item.isStarred) }

    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    val questionsList: List<QuizQuestion> = remember(item) {
        parseQuestionsList(
            questionsJson = item.questionsJson,
            fallbackQuestionText = item.questionText,
            fallbackQuestionType = item.questionType,
            fallbackOptionsJson = item.optionsJson,
            fallbackCodePrefix = item.codeSnippetPrefix,
            fallbackCorrectAnswer = item.correctAnswer,
            fallbackExplanation = item.explanation,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        TopAppBar(
            title = { Text(item.topic, maxLines = 1) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        isStarred = !isStarred
                        onStarToggled(isStarred)
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
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.primary,
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = colors.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Text(
                    text = "AI concept summary",
                    style = type.labelSmall,
                    color = colors.primary,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = item.conceptTitle,
                style = type.headlineSmall,
                color = colors.onSurface,
                modifier = Modifier.semantics { heading() },
            )

            Spacer(modifier = Modifier.height(14.dp))

            MarkdownView(markdownText = item.conceptSummary ?: item.explanation)

            if (item.codeSnippetPrefix.isValidSnippet()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = colors.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = item.codeSnippetPrefix!!.replace("\\n", "\n"),
                        style = type.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = colors.primary,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = colors.primaryContainer,
                ) {
                    Text(
                        text = "Knowledge check",
                        style = type.labelSmall,
                        color = colors.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Text(
                    text = "• ${questionsList.size} questions",
                    style = type.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (questionsList.isEmpty()) {
                Text(
                    text = "No questions were recorded for this concept yet.",
                    style = type.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }

            questionsList.forEachIndexed { idx, q ->
                val userSegment = userAnswerForQuestion(item.userAnswer, idx, questionsList.size)
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Q${idx + 1}. ${q.questionText}",
                            style = type.titleSmall,
                            color = colors.onSurface,
                        )

                        if (q.optionsList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            q.optionsList.forEachIndexed { optIdx, optText ->
                                val isCorrect = checkIsCorrect(optIdx, optText, q.correctAnswer)
                                val isUserSel = checkIsUserSelected(optIdx, optText, userSegment)
                                OptionRow(
                                    optIdx = optIdx,
                                    optText = optText,
                                    isCorrect = isCorrect,
                                    isUserSelected = isUserSel,
                                )
                                if (optIdx < q.optionsList.lastIndex) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        if (q.explanation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = colors.surfaceContainerHighest,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Explanation",
                                        style = type.labelSmall,
                                        color = colors.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = q.explanation,
                                        style = type.bodySmall,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text("Return to app", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OptionRow(
    optIdx: Int,
    optText: String,
    isCorrect: Boolean,
    isUserSelected: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    // State is tonal background + icon + label: no decorative border needed,
    // and plain options need no container at all.
    val container = when {
        isCorrect -> SuccessGreen.copy(alpha = 0.12f)
        isUserSelected -> colors.primaryContainer.copy(alpha = 0.4f)
        else -> null
    }
    val letterLabel = when (optIdx) {
        0 -> "A"
        1 -> "B"
        2 -> "C"
        3 -> "D"
        else -> (optIdx + 1).toString()
    }

    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = when {
                    isCorrect -> SuccessGreen
                    isUserSelected -> colors.primary
                    else -> colors.surfaceContainerHighest
                },
                modifier = Modifier.size(24.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = letterLabel,
                        style = type.labelSmall,
                        color = when {
                            isCorrect -> colors.onPrimary
                            isUserSelected -> colors.onPrimary
                            else -> colors.onSurfaceVariant
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = optText,
                style = type.bodyMedium,
                fontWeight = if (isCorrect || isUserSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCorrect || isUserSelected) colors.onSurface else colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (isCorrect) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Correct answer",
                    tint = SuccessGreen,
                    modifier = Modifier.size(18.dp),
                )
            } else if (isUserSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Your answer",
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }

    if (container != null) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = container,
            modifier = Modifier.fillMaxWidth(),
            content = content,
        )
    } else {
        Box(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

/**
 * Multi-question attempts store answers joined as "Q1: x | Q2: y"; resolve the
 * segment for one question so badges never compare against the joined string.
 */
private fun userAnswerForQuestion(userAnswer: String, index: Int, total: Int): String {
    if (total <= 1) return userAnswer
    val parts = userAnswer.split(" | ")
    if (parts.size == total) {
        return Regex("^Q${index + 1}:\\s*").replace(parts[index], "")
    }
    return userAnswer
}

private fun checkIsCorrect(optIndex: Int, optionText: String, correctAnswer: String): Boolean {
    val trimmed = correctAnswer.trim()
    val letter = when (optIndex) { 0 -> "A"; 1 -> "B"; 2 -> "C"; 3 -> "D"; else -> "" }
    return trimmed.equals(letter, ignoreCase = true) ||
        trimmed == optIndex.toString() ||
        trimmed.equals(optionText.trim(), ignoreCase = true)
}

private fun checkIsUserSelected(optIndex: Int, optionText: String, userAnswer: String?): Boolean {
    if (userAnswer.isNullOrBlank()) return false
    val trimmed = userAnswer.trim()
    val letter = when (optIndex) { 0 -> "A"; 1 -> "B"; 2 -> "C"; 3 -> "D"; else -> "" }
    return trimmed.equals(letter, ignoreCase = true) ||
        trimmed == optIndex.toString() ||
        trimmed.equals(optionText.trim(), ignoreCase = true)
}
