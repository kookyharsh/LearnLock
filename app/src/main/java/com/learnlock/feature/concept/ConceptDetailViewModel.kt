package com.learnlock.feature.concept

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnlock.data.entity.QuestionHistory
import com.learnlock.data.repository.ConceptRepository
import com.learnlock.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val loaded: Boolean = false,
    val item: QuestionHistory? = null,
)

@HiltViewModel
class ConceptDetailViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val conceptRepository: ConceptRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val historyId: Long = savedStateHandle.get<Long>("historyId") ?: -1L

    val uiState: StateFlow<DetailUiState> = historyRepository.getHistoryById(historyId)
        .map { DetailUiState(loaded = true, item = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun toggleStar(starred: Boolean) {
        val current = uiState.value.item ?: return
        viewModelScope.launch {
            historyRepository.updateStarStatus(current.id, starred)
            conceptRepository.updateStarStatusByTitle(current.conceptTitle, starred)
        }
    }
}
