package com.example.ui.tour

enum class CoachmarkStep(val title: String, val description: String) {
    LEARN_TAB(
        "Learn Tab",
        "This is where your active concept and quiz will appear whenever you unlock your device."
    ),
    HISTORY_TAB(
        "History & Progress",
        "Track your progress and review all previously generated concepts and quiz questions here."
    ),
    SETTINGS_TAB(
        "Credentials & Topics",
        "Add your Gemini API key and specify your own custom learning topics (subjects) here."
    )
}
