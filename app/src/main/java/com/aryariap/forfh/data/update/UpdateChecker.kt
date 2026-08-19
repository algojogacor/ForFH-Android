package com.aryariap.forfh.data.update

import com.aryariap.forfh.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
)

data class AppUpdateInfo(
    val latestVersion: String,
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val hasUpdate: Boolean,
    val releaseTitle: String?,
    val releaseNotes: String?,
    val downloadUrl: String,
    val publishedAt: String?,
)

object UpdateChecker {

    private const val GITHUB_RELEASE_API =
        "https://api.github.com/repos/algojogacor/ForFH-Android/releases/latest"

    // Default download destination on the companion web portal / releases
    const val DOWNLOAD_PAGE_URL = "https://usual-olwen-algojogacorbgt-a2be655b.koyeb.app/unduh"

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder().build()

    suspend fun checkLatestRelease(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_RELEASE_API)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ForFH-Android-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val release = json.decodeFromString<GitHubReleaseDto>(body)
                val isNewer = isNewerVersion(release.tagName, BuildConfig.VERSION_NAME)

                AppUpdateInfo(
                    latestVersion = release.tagName.removePrefix("v"),
                    currentVersion = BuildConfig.VERSION_NAME,
                    hasUpdate = isNewer,
                    releaseTitle = release.name ?: release.tagName,
                    releaseNotes = release.body,
                    downloadUrl = DOWNLOAD_PAGE_URL,
                    publishedAt = release.publishedAt,
                )
            }
        } catch (_: Throwable) {
            null
        }
    }

    internal fun isNewerVersion(remoteTag: String, localVersion: String): Boolean {
        val remoteClean = remoteTag.trim().removePrefix("v")
        val localClean = localVersion.trim().removePrefix("v")

        val remoteParts = remoteClean.split(".").mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }
        val localParts = localClean.split(".").mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
