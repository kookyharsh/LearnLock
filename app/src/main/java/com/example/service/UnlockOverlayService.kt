package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.preferences.AppPreferencesManager

class UnlockOverlayService : Service() {

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundNotification() {
        val channelId = "unlock_learn_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Unlock Learning Service",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Monitors phone unlocks to show educational CS concepts"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Unlock & Learn CS Active")
            .setContentText("Ready with pre-generated CS concepts on unlock")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(
                        1001,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (t: Throwable) {
                    android.util.Log.w("UnlockOverlayService", "specialUse FGS type rejected, falling back to standard startForeground", t)
                    startForeground(1001, notification)
                }
            } else {
                startForeground(1001, notification)
            }
        } catch (t: Throwable) {
            android.util.Log.e("UnlockOverlayService", "Failed to start foreground notification", t)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Pre-generate concepts in background when service starts
        val prefsManager = AppPreferencesManager(applicationContext)
        if (com.example.data.TutorState.isActive(
                serviceEnabled = prefsManager.isUnlockServiceEnabled(),
                disabledUntil = prefsManager.getTutorDisabledUntil(),
                now = System.currentTimeMillis()
            )
        ) {
            UnlockReceiver.pregenerateConceptsIfNeeded(applicationContext, prefsManager)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(context: Context) {
            val serviceIntent = Intent(context, UnlockOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UnlockOverlayService::class.java))
        }
    }
}
