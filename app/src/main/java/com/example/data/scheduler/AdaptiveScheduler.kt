package com.example.data.scheduler

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Adaptive spaced-repetition engine (SM-2 inspired) plus mastery scoring and
 * dynamic difficulty resolution. Pure Kotlin so it is unit-testable on the JVM.
 */
object AdaptiveScheduler {

    const val MIN_EASE_FACTOR = 1.3
    const val MAX_EASE_FACTOR = 2.8
    const val DEFAULT_EASE_FACTOR = 2.5

    val DIFFICULTY_LEVELS = listOf("Easy", "Medium", "Hard")

    const val STATUS_NEW = "NEW"
    const val STATUS_LEARNING = "LEARNING"
    const val STATUS_REVIEW = "REVIEW"
    const val STATUS_MASTERED = "MASTERED"

    private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

    /** One step below the base difficulty when the user is struggling. */
    private const val STRUGGLE_MASTERY_THRESHOLD = 0.4
    /** One step above the base difficulty when the user is clearly strong. */
    private const val STRONG_MASTERY_THRESHOLD = 0.8
    private const val LAPSE_TOLERANCE = 2

    data class ReviewState(
        val repetitions: Int,
        val easeFactor: Double,
        val intervalDays: Int,
        val nextReviewAt: Long?,
        val lapses: Int,
        val status: String
    )

    /**
     * Applies the SM-2 rules for a single review outcome.
     * Pass: grows the interval and ease. Fail: collapses back to a 1-day interval.
     */
    fun scheduleAnswer(
        repetitions: Int,
        easeFactor: Double,
        intervalDays: Int,
        nextReviewAt: Long?,
        lapses: Int,
        passed: Boolean,
        now: Long
    ): ReviewState {
        return if (passed) {
            val newRepetitions = repetitions + 1
            val newInterval = when {
                newRepetitions <= 1 -> 1
                newRepetitions == 2 -> 3
                else -> max(1, (intervalDays * easeFactor).roundToInt())
            }
            val newEase = clampEase(easeFactor + 0.1)
            val newNextReviewAt = now + newInterval * DAY_MILLIS
            ReviewState(
                repetitions = newRepetitions,
                easeFactor = newEase,
                intervalDays = newInterval,
                nextReviewAt = newNextReviewAt,
                lapses = lapses,
                status = statusOf(newRepetitions, newInterval, newNextReviewAt)
            )
        } else {
            val newLapses = lapses + 1
            val newEase = clampEase(easeFactor - 0.2)
            ReviewState(
                repetitions = 0,
                easeFactor = newEase,
                intervalDays = 1,
                nextReviewAt = now + DAY_MILLIS,
                lapses = newLapses,
                status = STATUS_LEARNING
            )
        }
    }

    /**
     * Recency-weighted accuracy in [0, 1]. Recent results count roughly double
     * those from a week ago (exponential decay with a 7-day half-life).
     * Returns a neutral 0.5 when there is no data.
     */
    fun computeMastery(results: List<Boolean>, timestamps: List<Long>, now: Long): Double {
        require(results.size == timestamps.size) { "results and timestamps must match" }
        if (results.isEmpty()) return 0.5

        val halfLifeDays = 7.0
        val lambda = 0.6931471805599453 / (halfLifeDays * DAY_MILLIS)
        var weightedSum = 0.0
        var weightSum = 0.0
        for (i in results.indices) {
            val weight = exp(-lambda * (now - timestamps[i]).coerceAtLeast(0))
            weightedSum += if (results[i]) weight else 0.0
            weightSum += weight
        }
        return if (weightSum == 0.0) 0.5 else (weightedSum / weightSum).coerceIn(0.0, 1.0)
    }

    /**
     * Resolves the difficulty for the next batch of generated content based on
     * the user's baseline preference, per-topic mastery, and lapse history.
     */
    fun resolveDifficulty(baseSetting: String, mastery: Double, lapses: Int): String {
        val baseIdx = DIFFICULTY_LEVELS.indexOf(baseSetting)
        if (baseIdx == -1) return DIFFICULTY_LEVELS[1]
        var idx = baseIdx
        if (lapses >= LAPSE_TOLERANCE || mastery < STRUGGLE_MASTERY_THRESHOLD) {
            idx = max(0, idx - 1)
        } else if (mastery >= STRONG_MASTERY_THRESHOLD) {
            idx = min(DIFFICULTY_LEVELS.lastIndex, idx + 1)
        }
        return DIFFICULTY_LEVELS[idx]
    }

    fun statusOf(repetitions: Int, intervalDays: Int, nextReviewAt: Long?): String {
        if (nextReviewAt == null) return STATUS_NEW
        if (repetitions < 3) return STATUS_LEARNING
        if (intervalDays <= 7) return STATUS_REVIEW
        return STATUS_MASTERED
    }

    fun clampEase(easeFactor: Double): Double {
        return easeFactor.coerceIn(MIN_EASE_FACTOR, MAX_EASE_FACTOR)
    }
}
