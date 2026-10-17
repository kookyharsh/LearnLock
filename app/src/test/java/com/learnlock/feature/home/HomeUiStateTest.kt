package com.learnlock.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiStateTest {

    @Test
    fun progress_zeroCompleted_returnsZero() {
        val state = HomeUiState(todayCompletedQuizzes = 0, dailyQuizGoal = 5)
        assertEquals(0f, state.progress, 0.001f)
        assertEquals(0, state.progressPercent)
    }

    @Test
    fun progress_partialCompleted_calculatesAccurately() {
        val state = HomeUiState(todayCompletedQuizzes = 3, dailyQuizGoal = 5)
        assertEquals(0.6f, state.progress, 0.001f)
        assertEquals(60, state.progressPercent)
    }

    @Test
    fun progress_goalExceeded_capsAtOneHundredPercent() {
        val state = HomeUiState(todayCompletedQuizzes = 7, dailyQuizGoal = 5)
        assertEquals(1.0f, state.progress, 0.001f)
        assertEquals(100, state.progressPercent)
    }

    @Test
    fun progress_zeroGoal_handlesSafely() {
        val state = HomeUiState(todayCompletedQuizzes = 2, dailyQuizGoal = 0)
        assertEquals(0f, state.progress, 0.001f)
        assertEquals(0, state.progressPercent)
    }

    @Test
    fun isEffectivelyActive_requiresBothEnabledAndOverlayPermission() {
        assertFalse(HomeUiState(isLearnLockEnabled = false, hasOverlayPermission = true).isEffectivelyActive)
        assertFalse(HomeUiState(isLearnLockEnabled = true, hasOverlayPermission = false).isEffectivelyActive)
        assertTrue(HomeUiState(isLearnLockEnabled = true, hasOverlayPermission = true).isEffectivelyActive)
    }
}

