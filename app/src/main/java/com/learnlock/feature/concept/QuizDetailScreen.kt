package com.learnlock.feature.concept

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnlock.data.model.QuizSummary
import com.learnlock.feature.unlock.UnlockQuizActivity
import com.learnlock.feature.unlock.isValidSnippet
import com.learnlock.ui.components.MarkdownView
import com.learnlock.ui.theme.star
import com.learnlock.ui.theme.success
import com.learnlock.ui.theme.successContainer
import com.learnlock.ui.theme.warning
import com.learnlock.ui.theme.warningContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun QuizDetailRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (!state.loaded) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val quiz = state.quizSummary
    if (quiz == null) {
        LaunchedEffect(Unit) { onBack() }
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    QuizDetailScreen(
        state = state,
        onBack = onBack,
        onToggleStar = viewModel::toggleStar,
        onAttemptQuiz = {
            val intent = Intent(context, UnlockQuizActivity::class.java).apply {
                if (quiz.conceptId != null) {
                    putExtra(UnlockQuizActivity.EXTRA_CONCEPT_ID, quiz.conceptId)
                }
            }
            context.startActivity(intent)
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDetailScreen(
    state: QuizDetailUiState,
    onBack: () -> Unit,
    onToggleStar: (Boolean) -> Unit,
    onAttemptQuiz: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val quiz = state.quizSummary ?: return
    val sdf = remember { SimpleDateFormat("MMMM dd, yyyy · HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        TopAppBar(
            title = { Text(quiz.topic, style = type.titleMedium, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { onToggleStar(!quiz.isStarred) }) {
                    Icon(
                        imageVector = if (quiz.isStarred) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Star",
                        tint = if (quiz.isStarred) colors.star else colors.onSurfaceVariant,
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (quiz.isAttempted) {
                // Attempted Quiz Result Hero Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.primaryContainer,
                            ) {
                                Text(
                                    text = quiz.topic,
                                    style = type.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                            Text(
                                text = sdf.format(Date(quiz.date)),
                                style = type.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = quiz.title,
                            style = type.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(14.dp))

                        // Score metrics grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${quiz.score}/${quiz.maxScore}",
                                    style = type.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary,
                                )
                                Text("Score", style = type.labelMedium, color = colors.onSurfaceVariant)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${quiz.scorePercentage ?: 0}%",
                                    style = type.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface,
                                )
                                Text("Accuracy", style = type.labelMedium, color = colors.onSurfaceVariant)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "+${state.earnedXp} XP",
                                    style = type.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary,
                                )
                                Text("XP Earned", style = type.labelMedium, color = colors.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Answer breakdown pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.successContainer,
                                modifier = Modifier.weight(1f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Default.Check, null, tint = colors.success, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${state.totalCorrect} Correct", style = type.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.errorContainer,
                                modifier = Modifier.weight(1f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Default.Close, null, tint = colors.error, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${state.totalIncorrect} Incorrect", style = type.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (state.totalPending > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.warningContainer,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Default.HourglassTop, null, tint = colors.warning, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${state.totalPending} Pending", style = type.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Questions Review Section
                Text("Questions & Submitted Answers", style = type.titleMedium, fontWeight = FontWeight.Bold)

                state.questionReviews.forEach { review ->
                    QuestionReviewCard(review = review)
                }

            } else {
                // Unattempted Quiz Overview
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
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
                                    style = type.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surfaceContainerHighest,
                            ) {
                                Text(
                                    text = "Not Attempted",
                                    style = type.labelMedium,
                                    color = colors.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = quiz.title,
                            style = type.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Created ${sdf.format(Date(quiz.date))} · ${quiz.questionCount} Questions · ${quiz.difficulty} Difficulty",
                            style = type.bodySmall,
                            color = colors.onSurfaceVariant,
                        )

                        if (!quiz.conceptSummary.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Summary", style = type.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            MarkdownView(markdownText = quiz.conceptSummary)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onAttemptQuiz,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Attempt Quiz", style = type.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionReviewCard(review: QuestionReviewItem) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Question ${review.index}",
                    style = type.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                )

                // Status chip
                when {
                    review.isPendingEvaluation -> {
                        Surface(shape = RoundedCornerShape(6.dp), color = colors.warningContainer) {
                            Text(
                                text = "Pending Evaluation",
                                style = type.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    review.isCorrect -> {
                        Surface(shape = RoundedCornerShape(6.dp), color = colors.successContainer) {
                            Text(
                                text = "Correct ✓",
                                style = type.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.success,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    else -> {
                        Surface(shape = RoundedCornerShape(6.dp), color = colors.errorContainer) {
                            Text(
                                text = "Incorrect ✗",
                                style = type.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = review.questionText,
                style = type.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
            )

            if (review.codeSnippetPrefix.isValidSnippet()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = review.codeSnippetPrefix!!.replace("\\n", "\n"),
                        style = type.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = colors.primary,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            // User Answer
            Text("Your Submitted Answer:", style = type.labelSmall, color = colors.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = review.userAnswer.ifBlank { "No answer provided" },
                style = type.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    review.isPendingEvaluation -> colors.warning
                    review.isCorrect -> colors.success
                    else -> colors.error
                },
            )

            // Expected Answer if incorrect
            if (!review.isCorrect && !review.isPendingEvaluation && review.correctAnswer.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Correct Answer:", style = type.labelSmall, color = colors.onSurfaceVariant)
                Spacer(modifier = Modifier.height(2.dp))
                val formattedExpected = formatCorrectAnswer(review.correctAnswer, review.optionsList)
                Text(
                    text = formattedExpected,
                    style = type.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )
            }

            if (review.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceContainerHighest.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Explanation", style = type.labelSmall, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(review.explanation, style = type.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun formatCorrectAnswer(correctAnswer: String, options: List<String>): String {
    val idx = correctAnswer.toIntOrNull()
    if (idx != null && idx in options.indices) {
        return options[idx]
    }
    val letterIdx = when (correctAnswer.trim().uppercase()) {
        "A" -> 0; "B" -> 1; "C" -> 2; "D" -> 3; else -> -1
    }
    if (letterIdx != -1 && letterIdx in options.indices) {
        return options[letterIdx]
    }
    return correctAnswer
}

