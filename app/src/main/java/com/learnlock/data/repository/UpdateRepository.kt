package com.learnlock.data.repository

import com.learnlock.BuildConfig
import com.learnlock.data.remote.update.GitHubUpdateService
import com.learnlock.data.remote.update.UpdateInfo
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class UpdateRepository @Inject constructor(
    private val updateService: GitHubUpdateService,
) {

    suspend fun checkForUpdate(
        owner: String = "kookyharsh",
        repo: String = "LearnLock",
        currentVersion: String = BuildConfig.VERSION_NAME,
    ): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val release = updateService.getLatestRelease(owner, repo)
            val latestVersion = release.tagName.removePrefix("v").trim()
            val cleanCurrent = currentVersion.removePrefix("v").trim()

            val isAvailable = isNewerVersion(latestVersion, cleanCurrent)
            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                ?: release.assets.firstOrNull()

            Result.success(
                UpdateInfo(
                    isUpdateAvailable = isAvailable,
                    currentVersion = cleanCurrent,
                    latestVersion = latestVersion,
                    releaseName = release.name,
                    releaseNotes = release.body,
                    apkUrl = apkAsset?.downloadUrl,
                    apkSize = apkAsset?.size ?: 0L,
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isNewerVersion(remoteVersion: String, localVersion: String): Boolean {
        val remoteParts = remoteVersion.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val localParts = localVersion.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }

        val maxParts = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxParts) {
            val remotePart = remoteParts.getOrElse(i) { 0 }
            val localPart = localParts.getOrElse(i) { 0 }
            if (remotePart > localPart) return true
            if (remotePart < localPart) return false
        }
        return false
    }
}
