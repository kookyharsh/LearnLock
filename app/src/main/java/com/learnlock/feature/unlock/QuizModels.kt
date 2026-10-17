package com.learnlock.feature.unlock

import android.os.Bundle
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshots.SnapshotStateMap

data class QuizQuestion(
    val questionText: String,
    val questionType: String,
    val optionsList: List<String>,
    val codeSnippetPrefix: String?,
    val correctAnswer: String,
    val explanation: String,
    val acceptedAnswers: List<String> = emptyList(),
    val hint: String? = null,
)

data class QuizResult(
    val passed: Boolean,
    val correctCount: Int,
    val total: Int,
    val masteryBefore: Double?,
    val masteryAfter: Double?,
    val nextReviewDays: Int?,
    val srsStatus: String?,
    val pendingCount: Int = 0,
)

internal val SelectedOptionsSaver: Saver<SnapshotStateMap<Int, Int>, IntArray> = Saver(
    save = { state: SnapshotStateMap<Int, Int> ->
        state.entries.flatMap { (key, value) -> listOf(key, value) }.toIntArray()
    },
    restore = { saved: IntArray ->
        mutableStateMapOf<Int, Int>().apply {
            saved.asSequence().chunked(2).forEach { pair ->
                if (pair.size == 2) put(pair[0], pair[1])
            }
        }
    },
)

internal val CodeAnswersSaver: Saver<SnapshotStateMap<Int, String>, Bundle> = Saver(
    save = { state: SnapshotStateMap<Int, String> ->
        Bundle().apply {
            for ((key, value) in state) {
                putString(key.toString(), value)
            }
        }
    },
    restore = { saved: Bundle ->
        mutableStateMapOf<Int, String>().apply {
            saved.keySet().forEach { key ->
                key.toIntOrNull()?.let { put(it, saved.getString(key) ?: "") }
            }
        }
    },
)

internal val QuizResultStateSaver: Saver<MutableState<QuizResult?>, Bundle> = Saver(
    save = { state: MutableState<QuizResult?> ->
        Bundle().apply {
            state.value?.let { r ->
                putBoolean("hasResult", true)
                putBoolean("passed", r.passed)
                putInt("correct", r.correctCount)
                putInt("total", r.total)
                putInt("pending", r.pendingCount)
                r.masteryBefore?.let { putDouble("masteryBefore", it) }
                r.masteryAfter?.let { putDouble("masteryAfter", it) }
                r.nextReviewDays?.let { putInt("reviewDays", it) }
                r.srsStatus?.let { putString("srs", it) }
            }
        }
    },
    restore = { saved: Bundle ->
        mutableStateOf(
            if (saved.getBoolean("hasResult", false)) {
                QuizResult(
                    passed = saved.getBoolean("passed"),
                    correctCount = saved.getInt("correct"),
                    total = saved.getInt("total"),
                    pendingCount = saved.getInt("pending"),
                    masteryBefore = if (saved.containsKey("masteryBefore")) saved.getDouble("masteryBefore") else null,
                    masteryAfter = if (saved.containsKey("masteryAfter")) saved.getDouble("masteryAfter") else null,
                    nextReviewDays = if (saved.containsKey("reviewDays")) saved.getInt("reviewDays") else null,
                    srsStatus = saved.getString("srs"),
                )
            } else {
                null
            },
        )
    },
)
