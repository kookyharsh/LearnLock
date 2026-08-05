package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorStateTest {

    private val now = 1_700_000_000_000L
    private val hour = 60 * 60 * 1000L

    @Test
    fun active_whenEnabledAndNeverPaused() {
        assertTrue(TutorState.isActive(serviceEnabled = true, disabledUntil = 0L, now = now))
    }

    @Test
    fun inactive_whenServiceDisabled() {
        assertFalse(TutorState.isActive(serviceEnabled = false, disabledUntil = 0L, now = now))
        assertFalse(TutorState.isActive(serviceEnabled = false, disabledUntil = now + 2 * hour, now = now))
    }

    @Test
    fun inactive_duringPause() {
        assertFalse(TutorState.isActive(serviceEnabled = true, disabledUntil = now + 2 * hour, now = now))
    }

    @Test
    fun active_afterPauseElapses() {
        assertTrue(TutorState.isActive(serviceEnabled = true, disabledUntil = now + 2 * hour, now = now + 3 * hour))
    }

    @Test
    fun active_whenPauseBoundaryReached() {
        assertTrue(TutorState.isActive(serviceEnabled = true, disabledUntil = now + 2 * hour, now = now + 2 * hour))
    }

    @Test
    fun paused_onlyDuringActivePause() {
        assertTrue(TutorState.isPaused(serviceEnabled = true, disabledUntil = now + 2 * hour, now = now))
        assertFalse(TutorState.isPaused(serviceEnabled = true, disabledUntil = 0L, now = now))
        assertFalse(TutorState.isPaused(serviceEnabled = false, disabledUntil = now + 2 * hour, now = now))
        assertFalse(TutorState.isPaused(serviceEnabled = true, disabledUntil = now + 2 * hour, now = now + 3 * hour))
    }
    @Test
    fun remainingMillis_clampsToZero() {
        assertEquals(2 * hour, TutorState.remainingMillis(now + 2 * hour, now))
        assertEquals(0L, TutorState.remainingMillis(now - 1, now))
    }
}
