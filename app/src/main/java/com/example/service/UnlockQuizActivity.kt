package com.example.service

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import com.example.data.preferences.AppPreferencesManager
import com.example.data.preferences.ThemeMode
import com.example.ui.quiz.UnlockQuizScreen
import com.example.ui.theme.UnlockLearnTheme

class UnlockQuizActivity : ComponentActivity() {

    companion object {
        const val EXTRA_RETRY_HISTORY_ID = "retry_history_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure screen turns on and activity shows over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as? android.app.KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        setContent {
            val themeMode = remember { AppPreferencesManager(this).getThemeMode() }
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            SideEffect {
                val style = if (darkTheme) {
                    androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    androidx.activity.SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    )
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            UnlockLearnTheme(themeMode = themeMode) {
                val retryHistoryId = intent.getLongExtra(EXTRA_RETRY_HISTORY_ID, -1L)
                    .takeIf { it != -1L }
                UnlockQuizScreen(
                    onDismiss = { finish() },
                    retryHistoryId = retryHistoryId,
                )
            }
        }
    }
}
