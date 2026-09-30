package com.example.service

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.preferences.AppPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ConceptGenerationState {
    data object Idle : ConceptGenerationState
    data class Running(val message: String) : ConceptGenerationState
    data class Success(val count: Int) : ConceptGenerationState
    data class Error(val message: String) : ConceptGenerationState
}

/**
 * Application-scoped generation keeps running when the user changes tabs and
 * exposes a single source of truth so repeat taps cannot create duplicate jobs.
 */
object ConceptGenerationCoordinator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<ConceptGenerationState>(ConceptGenerationState.Idle)
    val state: StateFlow<ConceptGenerationState> = _state.asStateFlow()

    fun start(context: Context) {
        if (_state.value is ConceptGenerationState.Running) return

        val appContext = context.applicationContext
        val preferences = AppPreferencesManager(appContext)
        val apiKey = preferences.getApiKey().trim()
        if (apiKey.isBlank()) {
            _state.value = ConceptGenerationState.Error("Add an API key in the 'API & Setup' tab first.")
            return
        }

        _state.value = ConceptGenerationState.Running("Connecting to AI provider…")
        scope.launch {
            try {
                val topics = preferences.getSelectedTopics()
                val topicName = topics.toList().randomOrNull() ?: "General Knowledge"
                _state.value = ConceptGenerationState.Running("Creating $topicName concepts…")
                val concepts = ConceptGenerator(preferences).generateBatchConcepts(
                    topics = topics,
                    count = 3,
                )
                if (concepts.isEmpty()) {
                    _state.value = ConceptGenerationState.Error(
                        "AI provider returned 0 concepts. Verify your API key in 'API & Setup'.",
                    )
                    return@launch
                }

                _state.value = ConceptGenerationState.Running("Saving ${concepts.size} concepts…")
                AppDatabase.getDatabase(appContext).conceptDao().insertConcepts(concepts)
                _state.value = ConceptGenerationState.Success(concepts.size)
            } catch (error: Exception) {
                _state.value = ConceptGenerationState.Error(
                    error.message?.takeIf { it.isNotBlank() }
                        ?: "Generation failed. Check your API key in 'API & Setup' and network connection.",
                )
            }
        }
    }

    fun clearResult() {
        if (_state.value !is ConceptGenerationState.Running) {
            _state.value = ConceptGenerationState.Idle
        }
    }
}
