package com.learnlock.data.remote.update

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface GitHubUpdateService {

    @Headers("User-Agent: LearnLockApp")
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): GitHubRelease
}
