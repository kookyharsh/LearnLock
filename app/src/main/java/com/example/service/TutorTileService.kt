package com.example.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.R
import com.example.data.TutorState
import com.example.data.preferences.AppPreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            tile.subtitle = getString(R.string.tile_subtitle_on)
        } else if (TutorState.isPaused(enabled, disabledUntil, now)) {
            tile.state = Tile.STATE_INACTIVE
            val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(disabledUntil))
            tile.subtitle = getString(R.string.tile_subtitle_paused_until, time)
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.subtitle = getString(R.string.tile_subtitle_off)
        }
        tile.updateTile()
    }

    companion object {
        fun refresh(context: Context) {
            try {
                val component = ComponentName(context, TutorTileService::class.java)
                TileService.requestListeningState(context, component)
            } catch (_: Exception) {
            }
        }

        fun startIfEnabled(context: Context) {
            if (AppPreferencesManager(context).isUnlockServiceEnabled()) {
                refresh(context)
            }
        }
    }
}
