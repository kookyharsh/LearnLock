package com.learnlock.feature.home

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnlock.data.entity.ConceptItem
import com.learnlock.data.entity.QuestionHistory
import com.learnlock.data.model.QuizSummary
import com.learnlock.data.preferences.AppPreferencesManager
import com.learnlock.data.remote.ai.ConceptGenerationCoordinator
import com.learnlock.data.remote.ai.ConceptGenerationState
import com.learnlock.data.repository.ConceptRepository
import com.learnlock.data.repository.HistoryRepository
import com.learnlock.data.repository.QuizMapping
import com.learnlock.domain.LearningAnalytics
import com.learnlock.feature.tutor.TutorTileService
import com.learnlock.feature.unlock.UnlockOverlayService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLearnLockEnabled: Boolean = false,
    val hasOverlayPermission: Boolean = true,
    val todayCompletedQuizzes: Int = 0,
    val dailyQuizGoal: Int = 5,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val motivationalMessage: String = "Start your first quiz today!",
    val recentQuizzes: List<QuizSummary> = emptyList(),
    val totalXp: Int = 0,
    val levelName: String = "Starter",
    val levelNumber: Int = 1,
    val generationState: ConceptGenerationState = ConceptGenerationState.Idle,
) {
    val progress: Float
        get() = if (dailyQuizGoal <= 0) 0f else (todayCompletedQuizzes.toFloat() / dailyQuizGoal).coerceIn(0f, 1f)

    val progressPercent: Int
        get() = (progress * 100).toInt()

    val isEffectivelyActive: Boolean
        get() = isLearnLockEnabled && hasOverlayPermission
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val historyRepository: HistoryRepository,
    private val conceptRepository: ConceptRepository,
    private val preferences: AppPreferencesManager,
) : ViewModel() {

    private val refreshVersion = MutableStateFlow(0)

    fun refresh() {
        refreshVersion.update { it + 1 }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        historyRepository.getAllHistory(),
        conceptRepository.getUnusedConcepts(),
        ConceptGenerationCoordinator.state,
        refreshVersion,
    ) { allHistory, unusedConcepts, genState, _ ->
        computeState(allHistory, unusedConcepts, genState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun computeState(
        history: List<QuestionHistory>,
        unusedConcepts: List<ConceptItem>,
        genState: ConceptGenerationState,
    ): HomeUiState {
        val hasOverlay = Settings.canDrawOverlays(context)
        val isServiceEnabled = preferences.isUnlockServiceEnabled()
        val dailyGoal = preferences.getDailyQuizGoal()

        // Completed quizzes: SESSION_SUMMARY entries
        val sessions = history.filter { it.questionType == "SESSION_SUMMARY" }

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayCompletedCount = sessions.count { it.answeredAt >= todayStart }

        // Streak calculation
        val completionDates = sessions.map { it.answeredAt }
        val streakInfo = LearningAnalytics.calculateStreaks(completionDates)

        // Motivation message
        val motivation = when {
            todayCompletedCount == 0 -> "Complete your first quiz to keep your streak going!"
            todayCompletedCount < dailyGoal -> {
                val remaining = dailyGoal - todayCompletedCount
                if (remaining == 1) "1 more quiz to reach your daily goal!" else "$remaining more quizzes to reach your daily goal!"
            }
            todayCompletedCount == dailyGoal -> "Daily goal reached! Great job!"
            else -> "Goal achieved! ${todayCompletedCount - dailyGoal} extra completed today!"
        }

        // Recent Quizzes: merge recent unattempted concepts and recent completed sessions
        val completedSummaries = sessions.map { QuizMapping.fromSessionSummary(it) }
        val unattemptedSummaries = unusedConcepts.map { QuizMapping.fromConcept(it) }

        val combinedQuizzes = (completedSummaries + unattemptedSummaries)
            .sortedByDescending { it.date }
            .take(3)

        // XP Calculation from correct answers
        val graded = history.filter { it.questionType != "SESSION_SUMMARY" && it.status != "GRADING_PENDING" }
        val totalXp = graded.count { it.isCorrect } * 10
        val (lvlName, lvlNum) = LearningAnalytics.levelForXp(totalXp)

        return HomeUiState(
            isLearnLockEnabled = isServiceEnabled,
            hasOverlayPermission = hasOverlay,
            todayCompletedQuizzes = todayCompletedCount,
            dailyQuizGoal = dailyGoal,
            currentStreak = streakInfo.currentStreak,
            bestStreak = streakInfo.bestStreak,
            motivationalMessage = motivation,
            recentQuizzes = combinedQuizzes,
            totalXp = totalXp,
            levelName = lvlName,
            levelNumber = lvlNum,
            generationState = genState,
        )
    }

    fun setLearnLockEnabled(enabled: Boolean) {
        preferences.setUnlockServiceEnabled(enabled)
        if (enabled) {
            UnlockOverlayService.start(context)
        } else {
            UnlockOverlayService.stop(context)
        }
        TutorTileService.refresh(context)
        refresh()
    }

    fun generateQuiz() {
        ConceptGenerationCoordinator.start(context)
    }

    fun clearGenerationResult() {
        ConceptGenerationCoordinator.clearResult()
    }
}
