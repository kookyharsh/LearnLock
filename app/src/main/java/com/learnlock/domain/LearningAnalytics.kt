package com.learnlock.domain

import java.util.Calendar

data class StreakInfo(
    val currentStreak: Int,
    val bestStreak: Int,
    val isActiveToday: Boolean,
)

object LearningAnalytics {

    /**
     * Calculates consecutive day streaks based on epoch millisecond completion timestamps.
     * Consecutive days are evaluated at calendar-day boundaries in local timezone.
     */
    fun calculateStreaks(completionTimestamps: List<Long>, now: Long = System.currentTimeMillis()): StreakInfo {
        if (completionTimestamps.isEmpty()) {
            return StreakInfo(currentStreak = 0, bestStreak = 0, isActiveToday = false)
        }

        // Distinct calendar day epoch keys (normalized to midnight) in descending order
        val uniqueDays = completionTimestamps
            .map { toLocalDayEpoch(it) }
            .distinct()
            .sortedDescending()

        val todayEpoch = toLocalDayEpoch(now)
        val oneDayMillis = 24L * 60 * 60 * 1000

        val isActiveToday = uniqueDays.contains(todayEpoch)
        val yesterdayEpoch = todayEpoch - oneDayMillis

        // Calculate current streak
        var currentStreak = 0
        if (isActiveToday) {
            currentStreak = 1
            var expectedDay = todayEpoch - oneDayMillis
            for (day in uniqueDays.filter { it < todayEpoch }) {
                if (day == expectedDay) {
                    currentStreak++
                    expectedDay -= oneDayMillis
                } else if (day < expectedDay) {
                    break
                }
            }
        } else if (uniqueDays.contains(yesterdayEpoch)) {
            // Completed yesterday, current day still ongoing: preserve streak
            currentStreak = 1
            var expectedDay = yesterdayEpoch - oneDayMillis
            for (day in uniqueDays.filter { it < yesterdayEpoch }) {
                if (day == expectedDay) {
                    currentStreak++
                    expectedDay -= oneDayMillis
                } else if (day < expectedDay) {
                    break
                }
            }
        } else {
            currentStreak = 0
        }

        // Calculate best historical streak
        val sortedAscending = uniqueDays.sorted()
        var bestStreak = if (sortedAscending.isNotEmpty()) 1 else 0
        var currentRun = 1

        for (i in 1 until sortedAscending.size) {
            val diff = sortedAscending[i] - sortedAscending[i - 1]
            // Allowance for DST shifts between 23h and 25h:
            if (diff in (23L * 3600 * 1000)..(25L * 3600 * 1000)) {
                currentRun++
                if (currentRun > bestStreak) {
                    bestStreak = currentRun
                }
            } else {
                currentRun = 1
            }
        }
        if (currentStreak > bestStreak) {
            bestStreak = currentStreak
        }

        return StreakInfo(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            isActiveToday = isActiveToday,
        )
    }

    /**
     * Normalizes a timestamp to local calendar day midnight (00:00:00.000).
     */
    fun toLocalDayEpoch(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Determines user level based on total earned XP.
     */
    fun levelForXp(xp: Int): Pair<String, Int> = when {
        xp >= 10000 -> "Master" to 8
        xp >= 6000 -> "Expert" to 7
        xp >= 3000 -> "Advanced" to 6
        xp >= 1500 -> "Skilled" to 5
        xp >= 750 -> "Practitioner" to 4
        xp >= 300 -> "Explorer" to 3
        xp >= 100 -> "Learner" to 2
        else -> "Starter" to 1
    }

    fun nextLevelXp(xp: Int): Int = when {
        xp >= 10000 -> 10000
        xp >= 6000 -> 10000
        xp >= 3000 -> 6000
        xp >= 1500 -> 3000
        xp >= 750 -> 1500
        xp >= 300 -> 750
        xp >= 100 -> 300
        else -> 100
    }

    fun prevLevelXp(xp: Int): Int = when {
        xp >= 10000 -> 10000
        xp >= 6000 -> 6000
        xp >= 3000 -> 3000
        xp >= 1500 -> 1500
        xp >= 750 -> 750
        xp >= 300 -> 300
        xp >= 100 -> 100
        else -> 0
    }
}

