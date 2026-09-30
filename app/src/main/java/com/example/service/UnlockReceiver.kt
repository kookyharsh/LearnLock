package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.preferences.AppPreferencesManager
import com.example.data.entity.ConceptItem
import com.example.data.TutorState
import com.example.data.resolveWeakestTopic
import com.example.data.scheduler.AdaptiveScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("UnlockReceiver", "Received action: $action")

        val prefsManager = AppPreferencesManager(context)
        if (!TutorState.isActive(
                serviceEnabled = prefsManager.isUnlockServiceEnabled(),
                disabledUntil = prefsManager.getTutorDisabledUntil(),
                now = System.currentTimeMillis()
            )
        ) return

        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                startOverlayServiceIfNeeded(context)
                val pendingResult = goAsync()
                pregenerateConceptsIfNeeded(context, prefsManager) { pendingResult.finish() }
            }

            Intent.ACTION_SCREEN_OFF -> {
                val pendingResult = goAsync()
                pregenerateConceptsIfNeeded(context, prefsManager) { pendingResult.finish() }
            }

            Intent.ACTION_USER_PRESENT -> {
                if (shouldTriggerLearning(prefsManager)) {
                    launchUnlockQuizActivity(context)
                }
            }
        }
    }

    private fun startOverlayServiceIfNeeded(context: Context) {
        try {
            val serviceIntent = Intent(context, UnlockOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (t: Throwable) {
            Log.e("UnlockReceiver", "Failed to start overlay service: ${t.message}")
        }
    }

    private fun launchUnlockQuizActivity(context: Context) {
        val quizIntent = Intent(context, UnlockQuizActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra("TRIGGERED_BY_UNLOCK", true)
        }

        try {
            context.startActivity(quizIntent)
            Log.d("UnlockReceiver", "Successfully launched UnlockQuizActivity directly")
        } catch (e: Exception) {
            Log.e("UnlockReceiver", "Failed to launch UnlockQuizActivity: ${e.message}", e)
        }
    }

    companion object {
        private fun parseMinutesOfDay(timeStr: String): Int? {
            return try {
                val trimmed = timeStr.trim()
                val is12Hour = trimmed.contains("AM", ignoreCase = true) || trimmed.contains("PM", ignoreCase = true)
                val formatStr = if (is12Hour) "hh:mm a" else "HH:mm"
                val sdf = SimpleDateFormat(formatStr, Locale.US)
                val date = sdf.parse(trimmed) ?: return null
                val cal = Calendar.getInstance().apply { time = date }
                cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            } catch (_: Exception) {
                null
            }
        }

        fun shouldTriggerLearning(prefsManager: AppPreferencesManager): Boolean {
            if (!prefsManager.isLearningWindowEnabled()) return true

            try {
                val startStr = prefsManager.getLearningWindowStart()
                val endStr = prefsManager.getLearningWindowEnd()

                val startMinutes = parseMinutesOfDay(startStr)
                val endMinutes = parseMinutesOfDay(endStr)

                val cal = Calendar.getInstance()
                val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

                if (startMinutes != null && endMinutes != null) {
                    val inWindow = if (startMinutes <= endMinutes) {
                        nowMinutes in startMinutes..endMinutes
                    } else {
                        nowMinutes >= startMinutes || nowMinutes <= endMinutes
                    }
                    return inWindow
                }
            } catch (e: Exception) {
                Log.e("UnlockReceiver", "Time parsing error: ${e.message}")
            }
            return true
        }

        private val generationInFlight = AtomicBoolean(false)

        fun pregenerateConceptsIfNeeded(
            context: Context,
            prefsManager: AppPreferencesManager,
            onComplete: (() -> Unit)? = null,
        ) {
            if (!generationInFlight.compareAndSet(false, true)) {
                onComplete?.invoke()
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val now = System.currentTimeMillis()
                    val unusedCount = db.conceptDao().getUnusedCountDirect()
                    val dueCount = db.conceptDao().getDueReviewsCount(now)

                    val apiKey = prefsManager.getApiKey()
                    if ((unusedCount < 3 || dueCount < 2) && apiKey.isNotBlank()) {
                        Log.d("UnlockReceiver", "Queue low (unused=$unusedCount, due=$dueCount). Generating concepts via AI...")
                        val selectedTopics = prefsManager.getSelectedTopics()
                        val generator = ConceptGenerator(prefsManager)

                        // Target the weakest topic with mastery-resolved difficulty
                        val weakestTopic = resolveWeakestTopic(db, selectedTopics.toList())
                        val targetTopics = if (weakestTopic != null) setOf(weakestTopic) else selectedTopics
                        if (targetTopics.isEmpty()) {
                            Log.w("UnlockReceiver", "No topics available for pre-generation.")
                            return@launch
                        }

                        val topicAccuracy = db.historyDao().getTopicAccuracy()
                            .associateBy { it.topic }
                        val accuracy = topicAccuracy[weakestTopic]
                        val mastery = if (accuracy == null || accuracy.total == 0) 0.5
                        else accuracy.correct.toDouble() / accuracy.total
                        val retryCount = weakestTopic?.let {
                            db.historyDao().getPendingRetryCountForTopic(it)
                        } ?: 0
                        val difficulty = AdaptiveScheduler.resolveDifficulty(
                            baseSetting = prefsManager.getDifficultyLevel(),
                            mastery = mastery,
                            lapses = retryCount
                        )
                        val focusAreas = db.conceptDao().getWeakConcepts(5)
                            .map { it.conceptTitle }

                        val newConcepts = generator.generateBatchConcepts(
                            topics = targetTopics,
                            count = 3,
                            difficultyOverride = difficulty,
                            focusAreas = focusAreas
                        )
                        if (newConcepts.isNotEmpty()) {
                            db.conceptDao().insertConcepts(newConcepts)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("UnlockReceiver", "Pre-generation error: ${e.message}")
                } finally {
                    generationInFlight.set(false)
                    onComplete?.invoke()
                }
            }
        }
    }
}
