package com.example.data.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveSchedulerTest {

    private val now = 1_700_000_000_000L
    private val day = 24 * 60 * 60 * 1000L

    @Test
    fun firstPass_schedulesOneDayAhead() {
        val state = AdaptiveScheduler.scheduleAnswer(
            repetitions = 0, easeFactor = 2.5, intervalDays = 0,
            nextReviewAt = null, lapses = 0, passed = true, now = now
        )
        assertEquals(1, state.repetitions)
        assertEquals(1, state.intervalDays)
        assertEquals(now + day, state.nextReviewAt)
        assertEquals(AdaptiveScheduler.STATUS_LEARNING, state.status)
    }

    @Test
    fun secondPass_jumpsToThreeDays() {
        val first = AdaptiveScheduler.scheduleAnswer(
            repetitions = 0, easeFactor = 2.5, intervalDays = 0,
            nextReviewAt = null, lapses = 0, passed = true, now = now
        )
        val second = AdaptiveScheduler.scheduleAnswer(
            repetitions = first.repetitions, easeFactor = first.easeFactor,
            intervalDays = first.intervalDays, nextReviewAt = first.nextReviewAt,
            lapses = 0, passed = true, now = now
        )
        assertEquals(2, second.repetitions)
        assertEquals(3, second.intervalDays)
        assertEquals(now + 3 * day, second.nextReviewAt)
    }

    @Test
    fun laterPasses_multiplyByEaseFactor() {
        var reps = 2
        var ease = 2.6
        var interval = 3
        var next: Long? = now + 3 * day
        var lapses = 0
        var lastState: AdaptiveScheduler.ReviewState
        do {
            lastState = AdaptiveScheduler.scheduleAnswer(
                repetitions = reps, easeFactor = ease, intervalDays = interval,
                nextReviewAt = next, lapses = lapses, passed = true, now = now
            )
            reps = lastState.repetitions
            ease = lastState.easeFactor
            interval = lastState.intervalDays
            next = lastState.nextReviewAt
            lapses = lastState.lapses
        } while (reps < 5)

        assertEquals(5, lastState.repetitions)
        assertTrue("interval should have grown past 3 days", lastState.intervalDays > 3)
        assertEquals(now + lastState.intervalDays * day, lastState.nextReviewAt)
    }

    @Test
    fun fail_collapsesIntervalAndIncreasesLapses() {
        val state = AdaptiveScheduler.scheduleAnswer(
            repetitions = 4, easeFactor = 2.6, intervalDays = 30,
            nextReviewAt = now, lapses = 0, passed = false, now = now
        )
        assertEquals(0, state.repetitions)
        assertEquals(1, state.intervalDays)
        assertEquals(now + day, state.nextReviewAt)
        assertEquals(1, state.lapses)
        assertEquals(2.4, state.easeFactor, 0.0001)
        assertEquals(AdaptiveScheduler.STATUS_LEARNING, state.status)
    }

    @Test
    fun easeFactor_clampsToBounds() {
        val low = AdaptiveScheduler.scheduleAnswer(
            repetitions = 0, easeFactor = 1.3, intervalDays = 0,
            nextReviewAt = null, lapses = 0, passed = false, now = now
        )
        assertEquals(1.3, low.easeFactor, 0.0001)

        val high = AdaptiveScheduler.scheduleAnswer(
            repetitions = 1, easeFactor = 2.8, intervalDays = 1,
            nextReviewAt = now, lapses = 0, passed = true, now = now
        )
        assertEquals(2.8, high.easeFactor, 0.0001)
    }

    @Test
    fun mastery_allCorrectIsOne_allWrongIsZero() {
        assertEquals(1.0, AdaptiveScheduler.computeMastery(
            listOf(true, true, true), listOf(now - day, now - 2 * day, now), now
        ), 0.0001)
        assertEquals(0.0, AdaptiveScheduler.computeMastery(
            listOf(false, false, false), listOf(now - day, now - 2 * day, now), now
        ), 0.0001)
    }

    @Test
    fun mastery_recentResultsWeightMoreThanOldOnes() {
        val score = AdaptiveScheduler.computeMastery(
            results = listOf(false, true),
            timestamps = listOf(now - 30 * day, now),
            now = now
        )
        assertTrue("recent correct result should outweigh old wrong one", score > 0.5)
    }

    @Test
    fun mastery_emptyDataIsNeutral() {
        assertEquals(0.5, AdaptiveScheduler.computeMastery(emptyList(), emptyList(), now), 0.0001)
    }

    @Test
    fun resolveDifficulty_strongMasteryRaisesLevel() {
        assertEquals("Hard", AdaptiveScheduler.resolveDifficulty("Medium", 0.9, 0))
    }

    @Test
    fun resolveDifficulty_lowMasteryLowersLevel() {
        assertEquals("Easy", AdaptiveScheduler.resolveDifficulty("Medium", 0.2, 0))
    }

    @Test
    fun resolveDifficulty_lapsesLowersLevel() {
        assertEquals("Easy", AdaptiveScheduler.resolveDifficulty("Medium", 0.6, 2))
    }

    @Test
    fun resolveDifficulty_clampsToBounds() {
        assertEquals("Hard", AdaptiveScheduler.resolveDifficulty("Hard", 0.9, 0))
        assertEquals("Easy", AdaptiveScheduler.resolveDifficulty("Easy", 0.1, 5))
    }

    @Test
    fun resolveDifficulty_unknownBaseFallsBackToEasy() {
        assertEquals("Medium", AdaptiveScheduler.resolveDifficulty("Extreme", 0.5, 0))
    }

    @Test
    fun statusOf_reflectsLearningStage() {
        assertEquals(AdaptiveScheduler.STATUS_NEW, AdaptiveScheduler.statusOf(0, 0, null))
        assertEquals(AdaptiveScheduler.STATUS_LEARNING, AdaptiveScheduler.statusOf(2, 3, now))
        assertEquals(AdaptiveScheduler.STATUS_REVIEW, AdaptiveScheduler.statusOf(3, 7, now))
        assertEquals(AdaptiveScheduler.STATUS_MASTERED, AdaptiveScheduler.statusOf(4, 21, now))
    }
}
