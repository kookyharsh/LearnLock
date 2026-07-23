package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.preferences.AppPreferencesManager
import com.example.data.entity.ConceptItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("UnlockReceiver", "Received action: $action")

        val prefsManager = AppPreferencesManager(context)
        if (!prefsManager.isUnlockServiceEnabled()) return

        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                startOverlayServiceIfNeeded(context)
                pregenerateConceptsIfNeeded(context, prefsManager)
            }

            Intent.ACTION_SCREEN_OFF -> {
                // Ensure concepts are generated and ready BEFORE phone locks!
                pregenerateConceptsIfNeeded(context, prefsManager)
            }

            Intent.ACTION_USER_PRESENT -> {
                // Phone unlocked! Check if learning condition applies
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
        } catch (e: Exception) {
            Log.e("UnlockReceiver", "Failed to start overlay service: ${e.message}")
        }
    }

    private fun launchUnlockQuizActivity(context: Context) {
        try {
            val quizIntent = Intent(context, UnlockQuizActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("TRIGGERED_BY_UNLOCK", true)
            }
            context.startActivity(quizIntent)
        } catch (e: Exception) {
            Log.e("UnlockReceiver", "Could not start UnlockQuizActivity: ${e.message}")
        }
    }

    companion object {
        fun shouldTriggerLearning(prefsManager: AppPreferencesManager): Boolean {
            if (!prefsManager.isLearningWindowEnabled()) return true

            try {
                val startStr = prefsManager.getLearningWindowStart() // e.g. "09:00"
                val endStr = prefsManager.getLearningWindowEnd() // e.g. "21:00"

                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val nowStr = sdf.format(Calendar.getInstance().time)

                val nowTime = sdf.parse(nowStr)
                val startTime = sdf.parse(startStr)
                val endTime = sdf.parse(endStr)

                if (nowTime != null && startTime != null && endTime != null) {
                    val inWindow = if (startTime.before(endTime)) {
                        nowTime in startTime..endTime
                    } else {
                        // Spans midnight
                        nowTime >= startTime || nowTime <= endTime
                    }

                    // User constraint: "I can choose to not learn a concept after 9am to 9pm"
                    // If skip option is enabled and we are in active window, return true unless configured otherwise
                    return inWindow
                }
            } catch (e: Exception) {
                Log.e("UnlockReceiver", "Time parsing error: ${e.message}")
            }
            return true
        }

        fun pregenerateConceptsIfNeeded(context: Context, prefsManager: AppPreferencesManager) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val unusedCount = db.conceptDao().getUnusedCountDirect()
                    if (unusedCount < 3) {
                        Log.d("UnlockReceiver", "Queue low ($unusedCount unused). Pre-generating concepts now...")
                        val generator = GeminiConceptGenerator(context, prefsManager)
                        val newConcepts = generator.generateBatchConcepts(
                            topics = prefsManager.getSelectedTopics(),
                            count = 3
                        )
                        db.conceptDao().insertConcepts(newConcepts)
                    }
                } catch (e: Exception) {
                    Log.e("UnlockReceiver", "Pre-generation error: ${e.message}")
                }
            }
        }
    }
}
