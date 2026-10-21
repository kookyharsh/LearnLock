package com.learnlock.feature.statistics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnlock.ui.theme.AppMotion

@Composable
fun StatisticsRoute(
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    StatisticsScreen(
        state = state,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    state: StatisticsUiState,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    var selectedDay by remember(state.selectedDay) { mutableStateOf(state.selectedDay) }

    val animatedLevelProgress by animateFloatAsState(
        targetValue = state.currentLevelProgress,
        animationSpec = AppMotion.progress,
        label = "Level Progress",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        TopAppBar(
            title = { Text("Learning Statistics", fontWeight = FontWeight.Bold) },
            windowInsets = WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
        )

        if (state.loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 5.1 Question History Activity Calendar Card
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Question History",
                            style = type.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                        )
                        Text(
                            text = "${state.totalQuestionsAnswered} / ${state.totalQuestionsCount} ANSWERED",
                            style = type.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tap a day to review your answer or try a question you haven't answered yet.",
                        style = type.bodyMedium,
                        color = colors.primary,
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Heatmap Grid with Y-axis day labels and X-axis month headers
                    val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    val horizontalScrollState = rememberScrollState()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        // Left Y-Axis day labels
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 22.dp, end = 8.dp),
                        ) {
                            dayLabels.forEach { label ->
                                Box(
                                    modifier = Modifier.height(28.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Text(
                                        text = label,
                                        style = type.labelSmall,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        // Scrollable Grid of Week Columns
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(horizontalScrollState),
                        ) {
                            // Month Headers Row above columns
                            Box(
                                modifier = Modifier
                                    .height(18.dp)
                                    .fillMaxWidth(),
                            ) {
                                state.monthHeaders.forEach { header ->
                                    val leftOffset = (header.columnIndex * (28 + 4)).dp
                                    Box(
                                        modifier = Modifier.padding(start = leftOffset),
                                    ) {
                                        Text(
                                            text = header.monthName,
                                            style = type.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Grid of week columns
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                state.weekColumns.forEach { week ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        week.forEach { day ->
                                            if (day == null) {
                                                Box(modifier = Modifier.size(28.dp))
                                            } else {
                                                val isSelected = selectedDay?.epochDay == day.epochDay
                                                val cellColor = when (day.status) {
                                                    DayStatus.CORRECT -> Color(0xFF22C55E) // Green
                                                    DayStatus.INCORRECT -> Color(0xFFEF4444) // Red
                                                    DayStatus.UNANSWERED -> colors.surfaceContainerHighest
                                                }

                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(cellColor)
                                                        .then(
                                                            if (isSelected) {
                                                                Modifier.border(2.dp, colors.primary, RoundedCornerShape(6.dp))
                                                            } else Modifier
                                                        )
                                                        .clickable { selectedDay = day },
                                                ) {}
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Legend Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF22C55E))
                            )
                            Text("Correct", style = type.labelSmall, color = colors.onSurfaceVariant)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFEF4444))
                            )
                            Text("Incorrect", style = type.labelSmall, color = colors.onSurfaceVariant)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.surfaceContainerHighest)
                            )
                            Text("Unanswered", style = type.labelSmall, color = colors.onSurfaceVariant)
                        }
                    }

                    // Selected day inspector
                    selectedDay?.let { day ->
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(day.dateFormatted, style = type.labelMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
                                Text(
                                    if (day.totalAnswered == 0) "No questions answered on this day"
                                    else "${day.correctCount} correct · ${day.incorrectCount} incorrect",
                                    style = type.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            if (day.totalAnswered > 0) {
                                Surface(shape = RoundedCornerShape(8.dp), color = colors.primaryContainer) {
                                    Text(
                                        "${day.totalAnswered} Answered",
                                        style = type.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5.2 Learning Statistics Grid
            Text("Core Metrics", style = type.titleMedium, fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "Total Quizzes",
                    value = "${state.totalQuizzesCompleted}",
                    icon = Icons.Default.Quiz,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "Total XP",
                    value = "${state.totalXpEarned}",
                    icon = Icons.Default.ElectricBolt,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "Current Streak",
                    value = "${state.currentStreak} Days",
                    icon = Icons.Default.Whatshot,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "Best Streak",
                    value = "${state.bestStreak} Days",
                    icon = Icons.Default.EmojiEvents,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "Average Accuracy",
                    value = "${state.averageAccuracyPercent}%",
                    icon = Icons.Default.Percent,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "Learning Days",
                    value = "${state.learningDaysCount} Days",
                    icon = Icons.Default.DateRange,
                    modifier = Modifier.weight(1f),
                )
            }

            // 5.3 Level Progression & Achievements
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
                            Text("Learner Level", style = type.bodySmall, color = colors.onSurfaceVariant)
                            Text(
                                text = "Level ${state.levelNumber}: ${state.levelName}",
                                style = type.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary,
                            )
                        }
                        Surface(shape = CircleShape, color = colors.primaryContainer, modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${state.levelNumber}",
                                    style = type.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onPrimaryContainer,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { animatedLevelProgress },
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = colors.primary,
                        trackColor = colors.surfaceContainerHighest,
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${state.totalXpEarned} XP", style = type.labelSmall, color = colors.onSurfaceVariant)
                        Text("Next Level: ${state.xpForNextLevel} XP", style = type.labelSmall, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                }
            }

            // Milestone Badges
            Text("Achievements", style = type.titleMedium, fontWeight = FontWeight.Bold)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.badges.forEach { badge ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (badge.isUnlocked) colors.surfaceContainerHigh else colors.surfaceContainerHighest.copy(alpha = 0.5f),
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (badge.isUnlocked) colors.primaryContainer else colors.surfaceContainerHighest,
                                modifier = Modifier.size(40.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (badge.isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (badge.isUnlocked) colors.primary else colors.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = badge.title,
                                    style = type.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (badge.isUnlocked) colors.onSurface else colors.onSurfaceVariant,
                                )
                                Text(
                                    text = badge.description,
                                    style = type.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }

                            if (badge.isUnlocked) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Unlocked",
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }

            // 5.4 Additional Insights
            Text("Insights", style = type.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InsightRow(
                        label = "Weekly Quizzes",
                        value = "${state.thisWeekCount} this week (vs ${state.lastWeekCount} last week)",
                    )
                    HorizontalDivider()
                    InsightRow(
                        label = "Top Practiced Topic",
                        value = state.mostPracticedTopic,
                    )
                    HorizontalDivider()
                    InsightRow(
                        label = "Most Active Day",
                        value = state.bestDayOfWeek,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = type.bodySmall, color = colors.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = type.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onSurface)
        }
    }
}

@Composable
private fun InsightRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = type.bodyMedium, color = colors.onSurfaceVariant)
        Text(value, style = type.bodyMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
    }
}

