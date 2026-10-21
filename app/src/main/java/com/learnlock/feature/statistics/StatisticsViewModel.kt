package com.learnlock.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnlock.data.entity.ConceptItem
import com.learnlock.data.entity.QuestionHistory
import com.learnlock.data.repository.ConceptRepository
import com.learnlock.data.repository.HistoryRepository
import com.learnlock.domain.LearningAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class DayStatus {
    CORRECT,
    INCORRECT,
    UNANSWERED,
}

data class CalendarDayItem(
    val epochDay: Long,
    val dayOfMonth: Int,
    val dayOfWeek: Int, // 1 = Sun, 2 = Mon, ..., 7 = Sat
    val monthName: String,
    val dateFormatted: String,
    val completedQuizzes: Int,
    val earnedXp: Int,
    val intensity: Int, // 0 to 4
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val totalAnswered: Int = 0,
    val status: DayStatus = DayStatus.UNANSWERED,
    val isFuture: Boolean = false,
)

data class MonthHeaderItem(
    val monthName: String,
    val columnIndex: Int,
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
    val weekColumns: List<List<CalendarDayItem?>> = emptyList(),
    val monthHeaders: List<MonthHeaderItem> = emptyList(),
    val selectedDay: CalendarDayItem? = null,
    val totalQuizzesCompleted: Int = 0,
    val totalXpEarned: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val averageAccuracyPercent: Int = 0,
    val totalQuestionsAnswered: Int = 0,
    val totalQuestionsCount: Int = 179,
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
    private val conceptRepository: ConceptRepository,
) : ViewModel() {

    val uiState: StateFlow<StatisticsUiState> = combine(
        historyRepository.getAllHistory(),
        conceptRepository.getAllConcepts(),
    ) { history, concepts ->
        computeState(history, concepts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    private fun computeState(
        history: List<QuestionHistory>,
        concepts: List<ConceptItem>,
    ): StatisticsUiState {
        val sessions = history.filter { it.questionType == "SESSION_SUMMARY" }
        val gradedQuestions = history.filter {
            it.questionType != "SESSION_SUMMARY" && it.status != "GRADING_PENDING"
        }

        val totalQuizzes = sessions.size
        val correctQuestions = gradedQuestions.count { it.isCorrect }
        val totalQuestions = gradedQuestions.size
        val totalXp = correctQuestions * 10
        val totalBankCount = concepts.size.coerceAtLeast(179)

        val accuracy = if (totalQuestions > 0) {
            ((correctQuestions.toFloat() / totalQuestions) * 100).toInt()
        } else 0

        // Streak & active dates
        val timestamps = sessions.map { it.answeredAt }
        val streakInfo = LearningAnalytics.calculateStreaks(timestamps)

        val uniqueDayEpochs = timestamps.map { LearningAnalytics.toLocalDayEpoch(it) }.distinct()
        val learningDays = uniqueDayEpochs.size

        // Multi-week calendar grid (10 trailing weeks ending with current week)
        val now = System.currentTimeMillis()
        val dayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val oneDayMillis = 24L * 60 * 60 * 1000
        val todayEpoch = LearningAnalytics.toLocalDayEpoch(now)

        val calToday = Calendar.getInstance().apply { timeInMillis = now }
        val dayOfWeekToday = calToday.get(Calendar.DAY_OF_WEEK) // SUNDAY=1 ... SATURDAY=7
        val daysSinceSunday = dayOfWeekToday - Calendar.SUNDAY
        val currentWeekSundayEpoch = todayEpoch - (daysSinceSunday * oneDayMillis)

        val numWeeks = 10
        val startSundayEpoch = currentWeekSundayEpoch - ((numWeeks - 1) * 7 * oneDayMillis)

        val weekColumns = mutableListOf<List<CalendarDayItem?>>()
        val allCalendarDays = mutableListOf<CalendarDayItem>()
        val monthHeaders = mutableListOf<MonthHeaderItem>()
        var lastMonthName = ""

        for (col in 0 until numWeeks) {
            val weekDays = mutableListOf<CalendarDayItem?>()
            val colStartEpoch = startSundayEpoch + (col * 7 * oneDayMillis)
            val calCol = Calendar.getInstance().apply { timeInMillis = colStartEpoch }
            val colMonthName = monthFormat.format(calCol.time)

            if (colMonthName != lastMonthName) {
                monthHeaders.add(MonthHeaderItem(colMonthName, col))
                lastMonthName = colMonthName
            }

            for (row in 0..6) {
                val targetEpoch = colStartEpoch + (row * oneDayMillis)
                val cal = Calendar.getInstance().apply { timeInMillis = targetEpoch }
                val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                val monthName = monthFormat.format(cal.time)
                val dateFormatted = dayFormat.format(cal.time)
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

                if (targetEpoch > todayEpoch) {
                    weekDays.add(null)
                } else {
                    val sessionsOnDay = sessions.filter {
                        LearningAnalytics.toLocalDayEpoch(it.answeredAt) == targetEpoch
                    }
                    val questionsOnDay = gradedQuestions.filter {
                        LearningAnalytics.toLocalDayEpoch(it.answeredAt) == targetEpoch
                    }
                    val correctCount = questionsOnDay.count { it.isCorrect }
                    val incorrectCount = questionsOnDay.count { !it.isCorrect }
                    val totalAnsweredOnDay = questionsOnDay.size
                    val xpOnDay = correctCount * 10

                    val status = when {
                        totalAnsweredOnDay == 0 -> DayStatus.UNANSWERED
                        correctCount > 0 -> DayStatus.CORRECT
                        else -> DayStatus.INCORRECT
                    }

                    val intensity = when {
                        sessionsOnDay.isEmpty() -> 0
                        sessionsOnDay.size == 1 -> 1
                        sessionsOnDay.size == 2 -> 2
                        sessionsOnDay.size in 3..4 -> 3
                        else -> 4
                    }

                    val item = CalendarDayItem(
                        epochDay = targetEpoch,
                        dayOfMonth = dayOfMonth,
                        dayOfWeek = dayOfWeek,
                        monthName = monthName,
                        dateFormatted = dateFormatted,
                        completedQuizzes = sessionsOnDay.size,
                        earnedXp = xpOnDay,
                        intensity = intensity,
                        correctCount = correctCount,
                        incorrectCount = incorrectCount,
                        totalAnswered = totalAnsweredOnDay,
                        status = status,
                    )
                    weekDays.add(item)
                    allCalendarDays.add(item)
                }
            }
            weekColumns.add(weekDays)
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
            calendarDays = allCalendarDays,
            weekColumns = weekColumns,
            monthHeaders = monthHeaders,
            selectedDay = allCalendarDays.lastOrNull(),
            totalQuizzesCompleted = totalQuizzes,
            totalXpEarned = totalXp,
            currentStreak = streakInfo.currentStreak,
            bestStreak = streakInfo.bestStreak,
            averageAccuracyPercent = accuracy,
            totalQuestionsAnswered = totalQuestions,
            totalQuestionsCount = totalBankCount,
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
