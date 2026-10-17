package com.learnlock.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnlock.data.entity.QuestionHistory
import com.learnlock.data.repository.ConceptRepository
import com.learnlock.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryEntry(
    val item: QuestionHistory,
    val retryItem: QuestionHistory?,
    val attemptCount: Int,
)

enum class DayBucket { EMPTY, PENDING, ALL_CORRECT, ALL_WRONG, MIXED }

data class DayStat(val daysAgo: Int, val count: Int, val bucket: DayBucket)

data class TopicStat(val topic: String, val accuracyPercent: Int, val count: Int)

data class HistoryStats(
    val correct: Int = 0,
    val retry: Int = 0,
    val pending: Int = 0,
    val days: List<DayStat> = emptyList(),
    val topics: List<TopicStat> = emptyList(),
)

data class HistoryUiState(
    val loading: Boolean = true,
    val allHistory: List<QuestionHistory> = emptyList(),
    val groupedEntries: List<HistoryEntry> = emptyList(),
    val topics: List<String> = listOf("All"),
    val stats: HistoryStats = HistoryStats(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val conceptRepository: ConceptRepository,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = historyRepository.getAllHistory()
        .map { computeState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clearAllHistory() }
    }

    fun toggleStar(entry: HistoryEntry) {
        val item = entry.item
        val newStar = !item.isStarred
        viewModelScope.launch {
            historyRepository.updateStarStatusForConcept(item.topic, item.conceptTitle, newStar)
            conceptRepository.updateStarStatusForConcept(item.topic, item.conceptTitle, newStar)
        }
    }

    private fun computeState(history: List<QuestionHistory>): HistoryUiState {
        val grouped = history
            .groupBy { "${it.topic.trim().lowercase()}\u0000${it.conceptTitle.trim().lowercase()}" }
            .values
            .map { attempts ->
                val latest = attempts.maxBy { it.answeredAt }
                val retry = attempts
                    .filter { it.status == "RETRY_PENDING" }
                    .maxByOrNull { it.answeredAt }
                val pendingReview = attempts
                    .filter { it.status == "GRADING_PENDING" }
                    .maxByOrNull { it.answeredAt }
                HistoryEntry(
                    item = latest.copy(
                        status = when {
                            retry != null -> "RETRY_PENDING"
                            pendingReview != null -> "GRADING_PENDING"
                            else -> "PASSED"
                        },
                        userAnswer = pendingReview?.userAnswer ?: latest.userAnswer,
                        isStarred = attempts.any { it.isStarred },
                    ),
                    retryItem = retry,
                    attemptCount = attempts.size,
                )
            }
            .sortedByDescending { it.item.answeredAt }

        val gradedItems = history.filter {
            it.questionType != "SESSION_SUMMARY" && it.status != "GRADING_PENDING"
        }
        val topicStats = gradedItems.groupBy { it.topic }
            .mapValues { (_, attempts) -> attempts.count { it.isCorrect } to attempts.size }
            .entries.sortedByDescending { it.value.second }
            .take(5)
            .map { (topic, scores) ->
                TopicStat(
                    topic = topic,
                    accuracyPercent = if (scores.second == 0) 0 else scores.first * 100 / scores.second,
                    count = scores.second,
                )
            }

        val days = (13 downTo 0).map { daysAgo ->
            val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val start = day.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = start + 24L * 60 * 60 * 1000
            val dayItems = history.filter {
                it.answeredAt in start until end && it.questionType != "SESSION_SUMMARY"
            }
            val bucket = when {
                dayItems.isEmpty() -> DayBucket.EMPTY
                dayItems.any { it.status == "GRADING_PENDING" } -> DayBucket.PENDING
                dayItems.all { it.isCorrect } -> DayBucket.ALL_CORRECT
                dayItems.all { !it.isCorrect } -> DayBucket.ALL_WRONG
                else -> DayBucket.MIXED
            }
            DayStat(daysAgo, dayItems.size, bucket)
        }

        return HistoryUiState(
            loading = false,
            allHistory = history,
            groupedEntries = grouped,
            topics = listOf("All") + history.map { it.topic }.distinct(),
            stats = HistoryStats(
                correct = gradedItems.count { it.isCorrect },
                retry = gradedItems.count { !it.isCorrect },
                pending = history.count { it.status == "GRADING_PENDING" },
                days = days,
                topics = topicStats,
            ),
        )
    }
}
