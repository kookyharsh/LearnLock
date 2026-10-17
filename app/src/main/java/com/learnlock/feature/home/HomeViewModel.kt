package com.learnlock.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnlock.data.entity.QuestionHistory
import com.learnlock.data.preferences.AppPreferencesManager
import com.learnlock.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val todayXp: Int = 0,
    val lifetimeXp: Int = 0,
    val combo: Int = 0,
    val level: String = "Starter",
    val goal: Int = 40,
    val gradedCount: Int = 0,
    val retryCount: Int = 0,
    val gradingCount: Int = 0,
    val tutorEnabled: Boolean = false,
) {
    val progress: Float
        get() = if (goal <= 0) 0f else (todayXp.toFloat() / goal).coerceIn(0f, 1f)

    val progressPercent: Int
        get() = (progress * 100).toInt()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    historyRepository: HistoryRepository,
    private val preferences: AppPreferencesManager,
) : ViewModel() {
    private val refreshVersion = MutableStateFlow(0)

    fun refreshPreferences() { refreshVersion.update { it + 1 } }

    val uiState: StateFlow<HomeUiState> = historyRepository.getAllHistory()
        .combine(refreshVersion) { history, _ -> computeState(history) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun computeState(history: List<QuestionHistory>): HomeUiState {
        val graded = history.filter { it.questionType != "SESSION_SUMMARY" }
        val ordered = graded.sortedBy { it.answeredAt }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = calendar.timeInMillis

        var combo = 0
        var lifetimeXp = 0
        var todayXp = 0
        var currentSession = ""
        ordered.forEach { attempt ->
            val sessionKey = "${attempt.topic}:${attempt.conceptTitle}:${attempt.answeredAt / 300_000L}"
            if (sessionKey != currentSession) {
                combo = 0
                currentSession = sessionKey
            }
            if (attempt.isCorrect) combo++ else combo = 0
            if (attempt.isCorrect) {
                val multiplier = when {
                    combo >= 6 -> 1.5
                    combo == 5 -> 1.4
                    combo == 4 -> 1.3
                    combo == 3 -> 1.2
                    combo == 2 -> 1.1
                    else -> 1.0
                }
                val points = (10 * multiplier).toInt()
                lifetimeXp += points
                if (attempt.answeredAt >= todayStart) todayXp += points
            }
        }

        return HomeUiState(
            todayXp = todayXp,
            lifetimeXp = lifetimeXp,
            combo = combo,
            level = levelFor(lifetimeXp),
            goal = preferences.getDailyXpGoal(),
            gradedCount = graded.size,
            retryCount = history.count { it.status == "RETRY_PENDING" },
            gradingCount = history.count { it.status == "GRADING_PENDING" },
            tutorEnabled = preferences.isUnlockServiceEnabled(),
        )
    }

    private fun levelFor(lifetimeXp: Int): String = when {
        lifetimeXp >= 10000 -> "Master"
        lifetimeXp >= 6000 -> "Expert"
        lifetimeXp >= 3000 -> "Advanced"
        lifetimeXp >= 1500 -> "Skilled"
        lifetimeXp >= 750 -> "Practitioner"
        lifetimeXp >= 300 -> "Explorer"
        lifetimeXp >= 100 -> "Learner"
        else -> "Starter"
    }
}
