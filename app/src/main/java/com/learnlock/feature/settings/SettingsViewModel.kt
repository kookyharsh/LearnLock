package com.learnlock.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnlock.data.preferences.AppPreferencesManager
import com.learnlock.data.preferences.ThemeMode
import com.learnlock.data.repository.ConceptRepository
import com.learnlock.data.repository.HistoryRepository
import com.learnlock.data.remote.ai.ConceptGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsInitial(
    val apiKey: String,
    val customModel: String,
    val learningWindowEnabled: Boolean,
    val learningWindowStart: String,
    val learningWindowEnd: String,
    val skipDuringActiveWindow: Boolean,
    val selectedTopics: Set<String>,
    val questionsPerQuiz: Int,
    val difficultyLevel: String,
    val questionTypes: Set<String>,
    val dailyXpGoal: Int,
    val themeMode: ThemeMode,
)

data class SettingsDataCounts(
    val conceptCount: Int? = null,
    val historyCount: Int? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferencesManager,
    private val conceptRepository: ConceptRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    val initial = SettingsInitial(
        apiKey = prefs.getApiKey(),
        customModel = prefs.getCustomModel(),
        learningWindowEnabled = prefs.isLearningWindowEnabled(),
        learningWindowStart = prefs.getLearningWindowStart(),
        learningWindowEnd = prefs.getLearningWindowEnd(),
        skipDuringActiveWindow = prefs.isSkipDuringActiveWindow(),
        selectedTopics = prefs.getSelectedTopics(),
        questionsPerQuiz = prefs.getQuestionsPerQuiz(),
        difficultyLevel = prefs.getDifficultyLevel(),
        questionTypes = prefs.getQuestionTypes(),
        dailyXpGoal = prefs.getDailyXpGoal(),
        themeMode = prefs.getThemeMode(),
    )

    private val _counts = MutableStateFlow(SettingsDataCounts())
    val counts: StateFlow<SettingsDataCounts> = _counts.asStateFlow()

    init {
        loadCounts()
    }

    private fun loadCounts() {
        viewModelScope.launch {
            try {
                _counts.value = SettingsDataCounts(
                    conceptCount = conceptRepository.getConceptCount(),
                    historyCount = historyRepository.getHistoryCount(),
                )
            } catch (_: Exception) {
                // Counts are informational; the screen works without them.
            }
        }
    }

    fun setApiKey(value: String) = prefs.setApiKey(value)

    fun setCustomModel(value: String) = prefs.setCustomModel(value)

    fun setThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode)

    fun isDynamicColor(): Boolean = prefs.isDynamicColor()

    fun setDynamicColor(enabled: Boolean) = prefs.setDynamicColor(enabled)

    fun setLearningWindowEnabled(enabled: Boolean) = prefs.setLearningWindowEnabled(enabled)

    fun setLearningWindowStart(value: String) = prefs.setLearningWindowStart(value)

    fun setLearningWindowEnd(value: String) = prefs.setLearningWindowEnd(value)

    fun setSkipDuringActiveWindow(skip: Boolean) = prefs.setSkipDuringActiveWindow(skip)

    fun setSelectedTopics(topics: Set<String>) = prefs.setSelectedTopics(topics)

    fun setDailyXpGoal(goal: Int) = prefs.setDailyXpGoal(goal)

    fun setDifficultyLevel(level: String) {
        prefs.setDifficultyLevel(level)
        clearUnusedConcepts()
    }

    fun setQuestionsPerQuiz(count: Int) {
        prefs.setQuestionsPerQuiz(count)
        clearUnusedConcepts()
    }

    fun setQuestionTypes(types: Set<String>) = prefs.setQuestionTypes(types)

    private fun clearUnusedConcepts() {
        viewModelScope.launch {
            try {
                conceptRepository.clearUnusedConcepts()
            } catch (_: Exception) {
                // Cleanup is best-effort.
            }
        }
    }

    suspend fun testApiKeyConnection(): Pair<Boolean, String> =
        ConceptGenerator(prefs).testApiKeyConnection()

    suspend fun deleteAllData() {
        conceptRepository.clearAllConcepts()
        historyRepository.clearAllHistory()
        _counts.value = SettingsDataCounts(conceptCount = 0, historyCount = 0)
    }
}
