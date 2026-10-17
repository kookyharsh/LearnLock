package com.learnlock.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class LearningAnalyticsTest {

    private fun dayTimestamp(daysAgo: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }
        return calendar.timeInMillis
    }

    @Test
    fun calculateStreaks_emptyHistory_returnsZero() {
        val streaks = LearningAnalytics.calculateStreaks(emptyList())
        assertEquals(0, streaks.currentStreak)
        assertEquals(0, streaks.bestStreak)
    }

    @Test
    fun calculateStreaks_singleDayActivityToday_returnsStreakOfOne() {
        val today = dayTimestamp(0)
        val streaks = LearningAnalytics.calculateStreaks(listOf(today))
        assertEquals(1, streaks.currentStreak)
        assertEquals(1, streaks.bestStreak)
    }

    @Test
    fun calculateStreaks_multipleQuizzesSameDay_countsAsOne() {
        val today = dayTimestamp(0)
        val todayEarlier = today - 3600000L
        val todayLater = today + 3600000L
        val streaks = LearningAnalytics.calculateStreaks(listOf(todayEarlier, today, todayLater))
        assertEquals(1, streaks.currentStreak)
        assertEquals(1, streaks.bestStreak)
    }

    @Test
    fun calculateStreaks_consecutiveDays_calculatesCorrectStreak() {
        val today = dayTimestamp(0)
        val yesterday = dayTimestamp(1)
        val twoDaysAgo = dayTimestamp(2)
        val streaks = LearningAnalytics.calculateStreaks(listOf(today, yesterday, twoDaysAgo))
        assertEquals(3, streaks.currentStreak)
        assertEquals(3, streaks.bestStreak)
    }

    @Test
    fun calculateStreaks_yesterdayActiveButNotTodayYet_streakMaintained() {
        val yesterday = dayTimestamp(1)
        val twoDaysAgo = dayTimestamp(2)
        val streaks = LearningAnalytics.calculateStreaks(listOf(yesterday, twoDaysAgo))
        // Yesterday was active, so streak is not lost yet today
        assertEquals(2, streaks.currentStreak)
        assertEquals(2, streaks.bestStreak)
    }

    @Test
    fun calculateStreaks_gapBreaksCurrentStreak_retainsBestStreak() {
        val today = dayTimestamp(0)
        // Missed yesterday (day 1)
        val twoDaysAgo = dayTimestamp(2)
        val threeDaysAgo = dayTimestamp(3)
        val fourDaysAgo = dayTimestamp(4)
        val streaks = LearningAnalytics.calculateStreaks(listOf(today, twoDaysAgo, threeDaysAgo, fourDaysAgo))
        assertEquals(1, streaks.currentStreak)
        assertEquals(3, streaks.bestStreak)
    }

    @Test
    fun levelForXp_progression() {
        val (starterName, starterLevel) = LearningAnalytics.levelForXp(0)
        assertEquals("Starter", starterName)
        assertEquals(1, starterLevel)
        assertEquals(100, LearningAnalytics.nextLevelXp(0))
        assertEquals(0, LearningAnalytics.prevLevelXp(0))

        val (learnerName, learnerLevel) = LearningAnalytics.levelForXp(100)
        assertEquals("Learner", learnerName)
        assertEquals(2, learnerLevel)
        assertEquals(300, LearningAnalytics.nextLevelXp(100))
        assertEquals(100, LearningAnalytics.prevLevelXp(100))

        val (masterName, masterLevel) = LearningAnalytics.levelForXp(10000)
        assertEquals("Master", masterName)
        assertEquals(8, masterLevel)
    }
}
