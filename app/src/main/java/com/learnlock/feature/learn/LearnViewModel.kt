package com.learnlock.feature.learn

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnlock.data.entity.QuestionHistory
import com.learnlock.data.preferences.AppPreferencesManager
import com.learnlock.data.repository.ConceptRepository
import com.learnlock.data.repository.HistoryRepository
import com.learnlock.data.remote.ai.ConceptGenerationCoordinator
import com.learnlock.data.remote.ai.ConceptGenerationState
import com.learnlock.feature.tutor.TutorTileService
import com.learnlock.feature.unlock.UnlockOverlayService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LearnUiState(
    val serviceEnabled: Boolean = false,
    val scheduleEnabled: Boolean = false,
    val windowStart: String = "09:00 AM",
    val windowEnd: String = "09:00 PM",
    val hasOverlayPermission: Boolean = true,
    val recentHistory: List<QuestionHistory>? = null,
    val generation: ConceptGenerationState = ConceptGenerationState.Idle,
)

@HiltViewModel
class LearnViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val preferences: AppPreferencesManager,
    private val historyRepository: HistoryRepository,
    private val conceptRepository: ConceptRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(currentState())
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            historyRepository.getAllHistory().collect { history ->
                val distinct = history
                    .distinctBy { "${it.topic.trim().lowercase()}\u0000${it.conceptTitle.trim().lowercase()}" }
                    .take(10)
                _uiState.update { it.copy(recentHistory = distinct) }
            }
        }
        viewModelScope.launch {
            ConceptGenerationCoordinator.state.collect { generation ->
                _uiState.update { it.copy(generation = generation) }
            }
        }
    }

    fun refresh() {
        _uiState.value = currentState().copy(
            recentHistory = _uiState.value.recentHistory,
            generation = _uiState.value.generation,
        )
        if (preferences.isUnlockServiceEnabled()) {
            UnlockOverlayService.start(appContext)
        }
    }

    fun setServiceEnabled(enabled: Boolean) {
        preferences.setUnlockServiceEnabled(enabled)
        _uiState.update { it.copy(serviceEnabled = enabled) }
        if (enabled) {
            UnlockOverlayService.start(appContext)
        } else {
            UnlockOverlayService.stop(appContext)
        }
        TutorTileService.refresh(appContext)
    }

    fun generateConcepts() {
        ConceptGenerationCoordinator.start(appContext)
    }

    fun toggleStar(item: QuestionHistory) {
        val newStar = !item.isStarred
        viewModelScope.launch {
            historyRepository.updateStarStatus(item.id, newStar)
            conceptRepository.updateStarStatusByTitle(item.conceptTitle, newStar)
        }
    }

    private fun currentState(): LearnUiState = LearnUiState(
        serviceEnabled = preferences.isUnlockServiceEnabled(),
        scheduleEnabled = preferences.isLearningWindowEnabled(),
        windowStart = formatTime(preferences.getLearningWindowStart()),
        windowEnd = formatTime(preferences.getLearningWindowEnd()),
        hasOverlayPermission = Settings.canDrawOverlays(appContext),
        recentHistory = null,
        generation = ConceptGenerationCoordinator.state.value,
    )

    private fun formatTime(time: String): String = try {
        if (time.contains("AM") || time.contains("PM")) {
            time
        } else {
            val sdf24 = SimpleDateFormat("HH:mm", Locale.getDefault())
            val sdf12 = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = sdf24.parse(time)
            if (date != null) sdf12.format(date) else time
        }
    } catch (_: Exception) {
        time
    }
}
