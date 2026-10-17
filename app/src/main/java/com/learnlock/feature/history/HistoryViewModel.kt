package com.learnlock.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnlock.data.entity.ConceptItem
import com.learnlock.data.entity.QuestionHistory
import com.learnlock.data.model.QuizSummary
import com.learnlock.data.repository.ConceptRepository
import com.learnlock.data.repository.HistoryRepository
import com.learnlock.data.repository.QuizMapping
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class QuizFilter { ALL, ATTEMPTED, NOT_ATTEMPTED }

data class HistoryUiState(
    val loading: Boolean = true,
    val searchQuery: String = "",
    val activeFilter: QuizFilter = QuizFilter.ALL,
    val allQuizzes: List<QuizSummary> = emptyList(),
    val filteredQuizzes: List<QuizSummary> = emptyList(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val conceptRepository: ConceptRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val activeFilter = MutableStateFlow(QuizFilter.ALL)

    val uiState: StateFlow<HistoryUiState> = combine(
        historyRepository.getAllHistory(),
        conceptRepository.getUnusedConcepts(),
        searchQuery,
        activeFilter,
    ) { allHistory, unusedConcepts, query, filter ->
        val sessions = allHistory.filter { it.questionType == "SESSION_SUMMARY" }
        val completed = sessions.map { QuizMapping.fromSessionSummary(it) }
        val unattempted = unusedConcepts.map { QuizMapping.fromConcept(it) }

        val allQuizzes = (completed + unattempted).sortedByDescending { it.date }

        val filtered = allQuizzes.filter { quiz ->
            val matchesFilter = when (filter) {
                QuizFilter.ALL -> true
                QuizFilter.ATTEMPTED -> quiz.isAttempted
                QuizFilter.NOT_ATTEMPTED -> !quiz.isAttempted
            }
            val matchesQuery = query.isBlank() ||
                quiz.title.contains(query, ignoreCase = true) ||
                quiz.topic.contains(query, ignoreCase = true)

            matchesFilter && matchesQuery
        }

        HistoryUiState(
            loading = false,
            searchQuery = query,
            activeFilter = filter,
            allQuizzes = allQuizzes,
            filteredQuizzes = filtered,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onFilterChanged(filter: QuizFilter) {
        activeFilter.value = filter
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearAllHistory()
            conceptRepository.clearAllConcepts()
        }
    }
}
