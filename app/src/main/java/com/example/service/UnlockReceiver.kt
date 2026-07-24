package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
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
                pregenerateConceptsIfNeeded(context, prefsManager)
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
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("TRIGGERED_BY_UNLOCK", true)
        }

        // Attempt 1: Direct activity start
        try {
            context.startActivity(quizIntent)
        } catch (e: Exception) {
            Log.e("UnlockReceiver", "Direct startActivity failed: ${e.message}")
        }

        // Attempt 2: High priority notification with fullScreenIntent
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "unlock_quiz_alert_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Unlock Learning Quiz",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Pops up CS concepts automatically when unlocking phone"
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                2002,
                quizIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Unlock & Learn CS")
                .setContentText("Tap to solve your 30s CS micro-quiz!")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(2002, notification)
        } catch (e: Exception) {
            Log.e("UnlockReceiver", "Notification popup trigger error: ${e.message}")
        }
    }

    companion object {
        fun shouldTriggerLearning(prefsManager: AppPreferencesManager): Boolean {
            if (!prefsManager.isLearningWindowEnabled()) return true

            try {
                val startStr = prefsManager.getLearningWindowStart()
                val endStr = prefsManager.getLearningWindowEnd()

                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val nowStr = sdf.format(Calendar.getInstance().time)

                val nowTime = sdf.parse(nowStr)
                val startTime = sdf.parse(startStr)
                val endTime = sdf.parse(endStr)

                if (nowTime != null && startTime != null && endTime != null) {
                    val inWindow = if (startTime.before(endTime)) {
                        nowTime in startTime..endTime
                    } else {
                        nowTime >= startTime || nowTime <= endTime
                    }
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
                    var unusedCount = db.conceptDao().getUnusedCountDirect()



                    val apiKey = prefsManager.getApiKey()
                    if (unusedCount < 3 && apiKey.isNotBlank()) {
                        Log.d("UnlockReceiver", "Queue low ($unusedCount unused). Generating concepts via AI...")
                        val generator = GeminiConceptGenerator(context, prefsManager)
                        val newConcepts = generator.generateBatchConcepts(
                            topics = prefsManager.getSelectedTopics(),
                            count = 3
                        )
                        if (newConcepts.isNotEmpty()) {
                            db.conceptDao().insertConcepts(newConcepts)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("UnlockReceiver", "Pre-generation error: ${e.message}")
                }
            }
        }
    }
}
