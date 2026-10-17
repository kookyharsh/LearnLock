package com.learnlock.feature.tutor

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.learnlock.R
import com.learnlock.domain.TutorState
import com.learnlock.data.preferences.AppPreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TutorTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val prefsManager = AppPreferencesManager(this)
        val now = System.currentTimeMillis()
        val active = TutorState.isActive(
            serviceEnabled = prefsManager.isUnlockServiceEnabled(),
            disabledUntil = prefsManager.getTutorDisabledUntil(),
            now = now
        )
        if (active) {
            prefsManager.setUnlockServiceEnabled(false)
            prefsManager.setTutorDisabledUntil(0L)
        } else {
            prefsManager.setTutorDisabledUntil(0L)
            prefsManager.setUnlockServiceEnabled(true)
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val prefsManager = AppPreferencesManager(this)
        val now = System.currentTimeMillis()
        val enabled = prefsManager.isUnlockServiceEnabled()
        val disabledUntil = prefsManager.getTutorDisabledUntil()

        if (TutorState.isActive(enabled, disabledUntil, now)) {
            tile.state = Tile.STATE_ACTIVE
            setTileSubtitle(tile, getString(R.string.tile_subtitle_on))
        } else if (TutorState.isPaused(enabled, disabledUntil, now)) {
            tile.state = Tile.STATE_INACTIVE
            val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(disabledUntil))
            setTileSubtitle(tile, getString(R.string.tile_subtitle_paused_until, time))
        } else {
            tile.state = Tile.STATE_INACTIVE
            setTileSubtitle(tile, getString(R.string.tile_subtitle_off))
        }
        tile.updateTile()
    }

    private fun setTileSubtitle(tile: Tile, subtitle: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = subtitle
        }
    }

    companion object {
        fun refresh(context: Context) {
            try {
                val component = ComponentName(context, TutorTileService::class.java)
                requestListeningState(context, component)
            } catch (_: Exception) {
            }
        }

        @Suppress("unused")
        fun startIfEnabled(context: Context) {
            if (AppPreferencesManager(context).isUnlockServiceEnabled()) {
                refresh(context)
            }
        }
    }
}
