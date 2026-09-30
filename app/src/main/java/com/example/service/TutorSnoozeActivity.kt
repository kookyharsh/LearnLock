package com.example.service

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TutorState
import com.example.data.preferences.AppPreferencesManager
import com.example.data.preferences.ThemeMode
import com.example.ui.theme.UnlockLearnTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TutorSnoozeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                SnoozeContent(
                    onClose = { finish() }
                )
            }
        }
    }
}

private val MINUTES_RANGE = 15f..1440f

@Composable
private fun SnoozeContent(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefsManager = remember { AppPreferencesManager(context) }
    val now = System.currentTimeMillis()
    val enabled = prefsManager.isUnlockServiceEnabled()
    val disabledUntil = prefsManager.getTutorDisabledUntil()
    val isActive = TutorState.isActive(enabled, disabledUntil, now)
    val isPaused = TutorState.isPaused(enabled, disabledUntil, now)

    var minutes by remember { mutableIntStateOf(60) }
    val colors = MaterialTheme.colorScheme

    val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    val pauseUntil = remember(minutes) {
        Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.MINUTE, minutes)
        }.time
    }

    val applyAction: (Boolean, Long) -> Unit = { serviceEnabled, disabledUntilMillis ->
        prefsManager.setUnlockServiceEnabled(serviceEnabled)
        prefsManager.setTutorDisabledUntil(disabledUntilMillis)
        if (serviceEnabled) {
            UnlockOverlayService.start(context)
        } else {
            UnlockOverlayService.stop(context)
        }
        TutorTileService.refresh(context)
        onClose()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tutor Controls",
                color = colors.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = when {
                        isPaused -> "Tutor paused until ${timeFormatter.format(disabledUntil)}"
                        isActive -> "Tutor is ON"
                        else -> "Tutor is OFF"
                    },
                    color = if (isActive) colors.primary else colors.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Keep tutor off for",
                    color = colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = formatDuration(minutes),
                    color = colors.primary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Slider(
                    value = minutes.toFloat(),
                    onValueChange = { minutes = it.toInt().coerceIn(15, 1440) },
                    valueRange = MINUTES_RANGE,
                    steps = 95,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primary,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = colors.surfaceContainerHighest,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "15 min",
                        color = colors.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "24 h",
                        color = colors.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.primaryContainer,
                ) {
                    Text(
                        text = "Tutor returns at ${timeFormatter.format(pauseUntil)}",
                        color = colors.onPrimaryContainer,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }

        Button(
            onClick = {
                val pauseUntilMillis = System.currentTimeMillis() + minutes * 60_000L
                applyAction(true, pauseUntilMillis)
            },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("Pause for ${formatDuration(minutes)}", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = { applyAction(true, 0L) },
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("Turn tutor on now", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = { applyAction(false, 0L) },
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("Turn tutor off completely", fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}
