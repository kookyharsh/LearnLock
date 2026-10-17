package com.learnlock.ui.tour

enum class CoachmarkStep(val title: String, val description: String) {
    HOME_TAB(
        "Home Dashboard",
        "Control LearnLock, review daily progress, view your streak, and access your recent quizzes."
    ),
    HISTORY_TAB(
        "Quiz Library",
        "Browse, filter, and search all your generated and completed quizzes."
    ),
    STATS_TAB(
        "Statistics & Streaks",
        "View your activity heatmap, mastery progression, and milestone achievements."
    ),
    SETTINGS_TAB(
        "Settings & Setup",
        "Configure your daily quiz goal, question types, and AI credentials."
    );

    companion object {
        val LEARN_TAB = HOME_TAB
    }
}
