package com.example

import android.app.Application
import android.content.Intent
import android.os.Build
import com.example.data.preferences.AppPreferencesManager
import com.example.service.UnlockOverlayService

class UnlockLearnApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        val prefsManager = AppPreferencesManager(this)
        if (prefsManager.isUnlockServiceEnabled()) {
            try {
                val serviceIntent = Intent(this, UnlockOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } catch (e: Exception) {
                // Ignore service start exception if restricted by OS background limits on boot
            }
        }
    }
}
