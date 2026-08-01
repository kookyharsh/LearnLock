package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferencesManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "secure_app_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback if Keystore initialization encounters issue in container environment
            context.getSharedPreferences("app_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    private val _apiKeyFlow = MutableStateFlow(getApiKey())
    val apiKeyFlow: StateFlow<String> = _apiKeyFlow.asStateFlow()

    fun getApiKey(): String {
        val customKey = prefs.getString(KEY_API_KEY, "") ?: ""
        if (customKey.isNotBlank()) return customKey
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
        _apiKeyFlow.value = key.trim()
    }

    fun isLearningWindowEnabled(): Boolean {
        return prefs.getBoolean(KEY_WINDOW_ENABLED, false)
    }

    fun setLearningWindowEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WINDOW_ENABLED, enabled).apply()
    }

    fun getLearningWindowStart(): String {
        return prefs.getString(KEY_WINDOW_START, "09:00 AM") ?: "09:00 AM"
    }

    fun setLearningWindowStart(time: String) {
        prefs.edit().putString(KEY_WINDOW_START, time).apply()
    }

    fun getLearningWindowEnd(): String {
        return prefs.getString(KEY_WINDOW_END, "09:00 PM") ?: "09:00 PM"
    }

    fun setLearningWindowEnd(time: String) {
        prefs.edit().putString(KEY_WINDOW_END, time).apply()
    }

    fun isSkipDuringActiveWindow(): Boolean {
        return prefs.getBoolean(KEY_SKIP_DURING_ACTIVE, true)
    }

    fun setSkipDuringActiveWindow(skip: Boolean) {
        prefs.edit().putBoolean(KEY_SKIP_DURING_ACTIVE, skip).apply()
    }

    fun getSelectedTopics(): Set<String> {
        return prefs.getStringSet(KEY_TOPICS, DEFAULT_TOPICS) ?: DEFAULT_TOPICS
    }

    fun setSelectedTopics(topics: Set<String>) {
        prefs.edit().putStringSet(KEY_TOPICS, topics).apply()
    }

    fun isUnlockServiceEnabled(): Boolean {
        return prefs.getBoolean(KEY_SERVICE_ENABLED, false)
    }

    fun setUnlockServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
    }

    fun isDriveSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_DRIVE_SYNC, true)
    }

    fun setDriveSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DRIVE_SYNC, enabled).apply()
    }

    fun getLastSyncTime(): Long {
        return prefs.getLong(KEY_LAST_SYNC, 0L)
    }

    fun setLastSyncTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC, time).apply()
    }

    fun getGoogleUserEmail(): String? {
        return prefs.getString(KEY_GOOGLE_USER_EMAIL, null)
    }

    fun setGoogleUserEmail(email: String) {
        prefs.edit().putString(KEY_GOOGLE_USER_EMAIL, email).apply()
    }

    fun isTourCompleted(): Boolean {
        return prefs.getBoolean(KEY_TOUR_COMPLETED, false)
    }

    fun setTourCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_TOUR_COMPLETED, completed).apply()
    }

    fun getCustomModel(): String {
        return prefs.getString(KEY_CUSTOM_MODEL, "") ?: ""
    }

    fun setCustomModel(model: String) {
        prefs.edit().putString(KEY_CUSTOM_MODEL, model.trim()).apply()
    }

    fun getQuestionsPerQuiz(): Int {
        return prefs.getInt(KEY_QUESTIONS_PER_QUIZ, 3).coerceIn(1, 5)
    }

    fun setQuestionsPerQuiz(count: Int) {
        prefs.edit().putInt(KEY_QUESTIONS_PER_QUIZ, count.coerceIn(1, 5)).apply()
    }

    fun getDifficultyLevel(): String {
        return prefs.getString(KEY_DIFFICULTY_LEVEL, "Medium") ?: "Medium"
    }

    fun setDifficultyLevel(level: String) {
        prefs.edit().putString(KEY_DIFFICULTY_LEVEL, level.trim()).apply()
    }

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_CUSTOM_MODEL = "custom_model"
        private const val KEY_WINDOW_ENABLED = "window_enabled"
        private const val KEY_WINDOW_START = "window_start"
        private const val KEY_WINDOW_END = "window_end"
        private const val KEY_SKIP_DURING_ACTIVE = "skip_during_active"
        private const val KEY_TOPICS = "selected_topics"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_DRIVE_SYNC = "drive_sync_enabled"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val KEY_GOOGLE_USER_EMAIL = "google_user_email"
        private const val KEY_TOUR_COMPLETED = "tour_completed"
        private const val KEY_QUESTIONS_PER_QUIZ = "questions_per_quiz"
        private const val KEY_DIFFICULTY_LEVEL = "difficulty_level"

        val DEFAULT_TOPICS = emptySet<String>()
    }
}
