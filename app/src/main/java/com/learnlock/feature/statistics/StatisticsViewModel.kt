package com.learnlock.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnlock.data.entity.QuestionHistory
import com.learnlock.data.repository.HistoryRepository
import com.learnlock.domain.LearningAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CalendarDayItem(
    val epochDay: Long,
    val dayOfMonth: Int,
    val monthName: String,
    val dateFormatted: String,
    val completedQuizzes: Int,
    val earnedXp: Int,
    val intensity: Int, // 0 to 4
)

data class MilestoneBadge(
    val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
)

data class StatisticsUiState(
    val loading: Boolean = true,
    val calendarDays: List<CalendarDayItem> = emptyList(),
    val selectedDay: CalendarDayItem? = null,
    val totalQuizzesCompleted: Int = 0,
    val totalXpEarned: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val averageAccuracyPercent: Int = 0,
    val totalQuestionsAnswered: Int = 0,
    val correctAnswersCount: Int = 0,
    val learningDaysCount: Int = 0,
    val levelName: String = "Starter",
    val levelNumber: Int = 1,
    val currentLevelProgress: Float = 0f,
    val xpForNextLevel: Int = 100,
    val xpInCurrentLevel: Int = 0,
    val badges: List<MilestoneBadge> = emptyList(),
    val thisWeekCount: Int = 0,
    val lastWeekCount: Int = 0,
    val mostPracticedTopic: String = "None",
    val bestDayOfWeek: String = "None",
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    val uiState: StateFlow<StatisticsUiState> = historyRepository.getAllHistory()
        .map { history -> computeState(history) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    private fun computeState(history: List<QuestionHistory>): StatisticsUiState {
        val sessions = history.filter { it.questionType == "SESSION_SUMMARY" }
        val gradedQuestions = history.filter {
            it.questionType != "SESSION_SUMMARY" && it.status != "GRADING_PENDING"
        }

        val totalQuizzes = sessions.size
        val correctQuestions = gradedQuestions.count { it.isCorrect }
        val totalQuestions = gradedQuestions.size
        val totalXp = correctQuestions * 10

        val accuracy = if (totalQuestions > 0) {
            ((correctQuestions.toFloat() / totalQuestions) * 100).toInt()
        } else 0

        // Streak & active dates
        val timestamps = sessions.map { it.answeredAt }
        val streakInfo = LearningAnalytics.calculateStreaks(timestamps)

        val uniqueDayEpochs = timestamps.map { LearningAnalytics.toLocalDayEpoch(it) }.distinct()
        val learningDays = uniqueDayEpochs.size

        // Calendar grid: last 28 days (4 weeks) leading up to today
        val now = System.currentTimeMillis()
        val dayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val dayList = mutableListOf<CalendarDayItem>()

        val oneDayMillis = 24L * 60 * 60 * 1000
        val todayEpoch = LearningAnalytics.toLocalDayEpoch(now)

        for (i in 27 downTo 0) {
            val targetEpoch = todayEpoch - (i * oneDayMillis)
            val cal = Calendar.getInstance().apply { timeInMillis = targetEpoch }
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val monthName = monthFormat.format(cal.time)
            val dateFormatted = dayFormat.format(cal.time)

            val sessionsOnDay = sessions.filter {
                LearningAnalytics.toLocalDayEpoch(it.answeredAt) == targetEpoch
            }
            val quizzesCount = sessionsOnDay.size

            val questionsOnDay = gradedQuestions.filter {
                LearningAnalytics.toLocalDayEpoch(it.answeredAt) == targetEpoch
            }
            val xpOnDay = questionsOnDay.count { it.isCorrect } * 10

            val intensity = when {
                quizzesCount == 0 -> 0
                quizzesCount == 1 -> 1
                quizzesCount == 2 -> 2
                quizzesCount in 3..4 -> 3
                else -> 4
            }

            dayList.add(
                CalendarDayItem(
                    epochDay = targetEpoch,
                    dayOfMonth = dayOfMonth,
                    monthName = monthName,
                    dateFormatted = dateFormatted,
                    completedQuizzes = quizzesCount,
                    earnedXp = xpOnDay,
                    intensity = intensity,
                )
            )
        }

        // Level & Progress
        val (lvlName, lvlNum) = LearningAnalytics.levelForXp(totalXp)
        val prevLvlXp = LearningAnalytics.prevLevelXp(totalXp)
        val nextLvlXp = LearningAnalytics.nextLevelXp(totalXp)
        val xpInLevel = totalXp - prevLvlXp
        val range = (nextLvlXp - prevLvlXp).coerceAtLeast(1)
        val levelProgress = (xpInLevel.toFloat() / range).coerceIn(0f, 1f)

        // Milestone achievements
        val hasPerfectQuiz = sessions.any { session ->
            val answers = session.userAnswer.split(" | ")
            answers.size >= 3 && session.isCorrect
        }

        val badges = listOf(
            MilestoneBadge("first_quiz", "First Step", "Complete your first quiz", totalQuizzes >= 1),
            MilestoneBadge("streak_3", "Streak Builder", "Maintain a 3-day streak", streakInfo.bestStreak >= 3),
            MilestoneBadge("streak_7", "Weekly Master", "Maintain a 7-day streak", streakInfo.bestStreak >= 7),
            MilestoneBadge("quizzes_10", "Quiz Novice", "Complete 10 quizzes", totalQuizzes >= 10),
            MilestoneBadge("questions_50", "Half-Century", "Answer 50 questions", totalQuestions >= 50),
            MilestoneBadge("perfect_score", "Flawless", "Score 100% on a full quiz", hasPerfectQuiz),
        )

        // Additional Insights:
        val sevenDaysMillis = 7L * oneDayMillis
        val thisWeekCount = sessions.count { it.answeredAt >= (now - sevenDaysMillis) }
        val lastWeekCount = sessions.count { it.answeredAt in (now - 2 * sevenDaysMillis) until (now - sevenDaysMillis) }

        val topicCounts = sessions.groupBy { it.topic }.mapValues { it.value.size }
        val topTopic = topicCounts.maxByOrNull { it.value }?.key ?: "None"

        val dayOfWeekCounts = sessions.groupBy {
            Calendar.getInstance().apply { timeInMillis = it.answeredAt }.get(Calendar.DAY_OF_WEEK)
        }.mapValues { it.value.size }

        val bestDayNum = dayOfWeekCounts.maxByOrNull { it.value }?.key
        val bestDayName = when (bestDayNum) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> "None"
        }

        return StatisticsUiState(
            loading = false,
            calendarDays = dayList,
            selectedDay = dayList.lastOrNull(),
            totalQuizzesCompleted = totalQuizzes,
            totalXpEarned = totalXp,
            currentStreak = streakInfo.currentStreak,
            bestStreak = streakInfo.bestStreak,
            averageAccuracyPercent = accuracy,
            totalQuestionsAnswered = totalQuestions,
            correctAnswersCount = correctQuestions,
            learningDaysCount = learningDays,
            levelName = lvlName,
            levelNumber = lvlNum,
            currentLevelProgress = levelProgress,
            xpForNextLevel = nextLvlXp,
            xpInCurrentLevel = xpInLevel,
            badges = badges,
            thisWeekCount = thisWeekCount,
            lastWeekCount = lastWeekCount,
            mostPracticedTopic = topTopic,
            bestDayOfWeek = bestDayName,
        )
    }
}

