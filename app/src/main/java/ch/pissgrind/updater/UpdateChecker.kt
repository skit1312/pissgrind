package ch.pissgrind.updater

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null,
    val name: String? = null,
    val assets: List<GitHubAsset> = emptyList()
)

@Serializable
data class GitHubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = ""
)

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val changelog: String?,
    val downloadUrl: String?,
    val success: Boolean = true
)

object UpdateChecker {
    private const val LATEST_URL = "https://api.github.com/repos/skit1312/pissgrind/releases/latest"
    private const val ALL_RELEASES_URL = "https://api.github.com/repos/skit1312/pissgrind/releases"

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun checkForUpdate(currentVersion: String): UpdateInfo {
        return try {
            val latest: GitHubRelease = client.get(LATEST_URL) {
                header("Accept", "application/vnd.github+json")
            }.body()

            val latestVersion = latest.tagName.removePrefix("v")
            val hasUpdate = isNewer(latestVersion, currentVersion)

            val downloadUrl = latest.assets.firstOrNull { it.name.endsWith(".apk") }?.browserDownloadUrl

            UpdateInfo(
                hasUpdate = hasUpdate,
                latestVersion = latestVersion,
                changelog = latest.body,
                downloadUrl = downloadUrl
            )
        } catch (e: Exception) {
            android.util.Log.e("UpdateChecker", "Update check failed", e)
            UpdateInfo(false, currentVersion, null, null, success = false)
        }
    }

    suspend fun getChangelogSinceVersion(currentVersion: String): String {
        return try {
            val releases: List<GitHubRelease> = client.get(ALL_RELEASES_URL) {
                header("Accept", "application/vnd.github+json")
            }.body()
            releases
                .filter { isNewer(it.tagName.removePrefix("v"), currentVersion) }
                .joinToString("\n\n") { "## ${it.name ?: it.tagName}\n${it.body ?: ""}" }
        } catch (e: Exception) {
            ""
        }
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv > cv) return true
            if (lv < cv) return false
        }
        return false
    }
}

