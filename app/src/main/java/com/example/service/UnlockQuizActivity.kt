package com.example.service

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        setContent {
            UnlockLearnTheme {
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
