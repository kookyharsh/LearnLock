package com.learnlock.data.repository

import com.learnlock.data.remote.update.GitHubAsset
import com.learnlock.data.remote.update.GitHubRelease
import com.learnlock.data.remote.update.GitHubUpdateService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRepositoryTest {

    private val fakeService = object : GitHubUpdateService {
        override suspend fun getLatestRelease(owner: String, repo: String): GitHubRelease {
            return GitHubRelease(
                tagName = "v1.2.0",
                name = "Release 1.2.0",
                body = "Bug fixes and improvements",
                htmlUrl = "https://github.com/Harsh/learnlock/releases/tag/v1.2.0",
                assets = listOf(
                    GitHubAsset(
                        name = "learnlock-v1.2.0.apk",
                        size = 15_000_000L,
                        downloadUrl = "https://github.com/Harsh/learnlock/releases/download/v1.2.0/learnlock.apk",
                    ),
                ),
            )
        }
    }

    private val repository = UpdateRepository(fakeService)

    @Test
    fun versionComparison_identifiesNewerVersions() {
        assertTrue(repository.isNewerVersion("1.2.0", "1.1.9"))
        assertTrue(repository.isNewerVersion("v2.0.0", "1.9.9"))
        assertFalse(repository.isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(repository.isNewerVersion("1.0.0", "1.1.0"))
    }

    @Test
    fun checkForUpdate_detectsNewRelease() = runTest {
        val result = repository.checkForUpdate(currentVersion = "1.0.0")
        assertTrue(result.isSuccess)
        val info = result.getOrThrow()
        assertTrue(info.isUpdateAvailable)
        assertEquals("1.2.0", info.latestVersion)
        assertEquals("https://github.com/Harsh/learnlock/releases/download/v1.2.0/learnlock.apk", info.apkUrl)
    }

    @Test
    fun checkForUpdate_upToDate() = runTest {
        val result = repository.checkForUpdate(currentVersion = "1.2.0")
        assertTrue(result.isSuccess)
        val info = result.getOrThrow()
        assertFalse(info.isUpdateAvailable)
    }
}
