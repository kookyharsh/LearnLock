package com.learnlock.data.model

data class QuizSummary(
    val id: Long,
    val title: String,
    val topic: String,
    val questionCount: Int,
    val isAttempted: Boolean,
    val score: Int? = null,
    val maxScore: Int? = null,
    val date: Long,
    val difficulty: String = "Medium",
    val questionsJson: String? = null,
    val conceptSummary: String? = null,
    val historySessionId: Long? = null,
    val conceptId: Long? = null,
    val xpEarned: Int? = null,
    val isStarred: Boolean = false,
) {
    val scorePercentage: Int?
        get() = if (maxScore != null && maxScore > 0 && score != null) {
            ((score.toFloat() / maxScore) * 100).toInt()
        } else null
}

