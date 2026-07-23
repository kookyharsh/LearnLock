package com.example.service

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.preferences.AppPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class DriveSyncManager(
    private val context: Context,
    private val prefsManager: AppPreferencesManager
) {
    private val dbName = "unlock_learn_db"

    suspend fun exportDatabaseToUri(targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(dbName)
            if (!dbFile.exists()) return@withContext false

            // Checkpoint WAL first
            val db = AppDatabase.getDatabase(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                FileInputStream(dbFile).use { input ->
                    input.copyTo(output)
                }
            }
            prefsManager.setLastSyncTime(System.currentTimeMillis())
            true
        } catch (e: Exception) {
            Log.e("DriveSyncManager", "Export error: ${e.message}", e)
            false
        }
    }

    suspend fun importDatabaseFromUri(sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(dbName)
            
            // Close database instance
            val db = AppDatabase.getDatabase(context)
            db.close()

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
            prefsManager.setLastSyncTime(System.currentTimeMillis())
            true
        } catch (e: Exception) {
            Log.e("DriveSyncManager", "Import error: ${e.message}", e)
            false
        }
    }

    suspend fun performCloudSync(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Simulate Google Drive cloud synchronization
            val db = AppDatabase.getDatabase(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
            
            val syncDir = File(context.filesDir, "google_drive_sync")
            if (!syncDir.exists()) syncDir.mkdirs()

            val cloudBackupFile = File(syncDir, "unlock_learn_backup.db")
            val dbFile = context.getDatabasePath(dbName)

            if (dbFile.exists()) {
                FileInputStream(dbFile).use { input ->
                    FileOutputStream(cloudBackupFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            prefsManager.setLastSyncTime(System.currentTimeMillis())
            true
        } catch (e: Exception) {
            Log.e("DriveSyncManager", "Sync error: ${e.message}", e)
            false
        }
    }
}
