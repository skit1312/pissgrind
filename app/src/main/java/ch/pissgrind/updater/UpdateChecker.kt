package ch.pissgrind.updater

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null,
    val name: String? = null,
    val prerelease: Boolean = false,
    val draft: Boolean = false,
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
    private const val TAG = "UpdateChecker"
    private const val RELEASES_URL =
        "https://api.github.com/repos/skit1312/pissgrind/releases?per_page=30"
    private const val CACHE_TTL_MS = 60 * 60 * 1000L      // 1 Stunde

    private val client = HttpClient(OkHttp) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 15_000
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    private val mutex = Mutex()
    private var cachedReleases: List<Pair<List<Int>, GitHubRelease>>? = null
    private var cachedAt = 0L

    private suspend fun fetchReleases(force: Boolean = false): List<Pair<List<Int>, GitHubRelease>>? =
        mutex.withLock {
            val now = System.currentTimeMillis()
            if (!force && cachedReleases != null && now - cachedAt < CACHE_TTL_MS) {
                return@withLock cachedReleases
            }
            try {
                val response = client.get(RELEASES_URL) {
                    header("Accept", "application/vnd.github+json")
                }
                if (response.status != HttpStatusCode.OK) {
                    Log.w(
                        TAG,
                        "HTTP ${response.status.value}, rate limit remaining: " +
                                "${response.headers["X-RateLimit-Remaining"]}"
                    )
                    return@withLock cachedReleases
                }
                val releases: List<GitHubRelease> = response.body()
                cachedReleases = releases
                    .filter { !it.draft && !it.prerelease }
                    .mapNotNull { r -> parseVersion(r.tagName)?.let { it to r } }
                    .sortedWith { a, b -> compareVersions(b.first, a.first) }
                cachedAt = now
                cachedReleases
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed", e)
                cachedReleases
            }
        }

    suspend fun checkForUpdate(currentVersion: String, force: Boolean = false): UpdateInfo {
        val fail = UpdateInfo(false, currentVersion, null, null, success = false)
        val releases = fetchReleases(force) ?: return fail
        val current = parseVersion(currentVersion) ?: return fail
        val (latestV, latest) = releases.firstOrNull()
            ?: return UpdateInfo(false, currentVersion, null, null)

        return UpdateInfo(
            hasUpdate = compareVersions(latestV, current) > 0,
            latestVersion = latestV.joinToString("."),
            changelog = latest.body,
            downloadUrl = latest.assets
                .firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                ?.browserDownloadUrl
        )
    }

    suspend fun getChangelogSinceVersion(currentVersion: String): String {
        val releases = fetchReleases() ?: return ""
        val current = parseVersion(currentVersion) ?: return ""
        return releases
            .filter { compareVersions(it.first, current) > 0 }
            .joinToString("\n\n") { (_, r) -> "## ${r.name ?: r.tagName}\n${r.body ?: ""}" }
    }

    private fun parseVersion(raw: String): List<Int>? =
        Regex("""\d+(\.\d+)*""").find(raw)?.value
            ?.split(".")
            ?.map { it.toIntOrNull() ?: 0 }

    private fun compareVersions(a: List<Int>, b: List<Int>): Int {
        for (i in 0 until maxOf(a.size, b.size)) {
            val d = a.getOrElse(i) { 0 }.compareTo(b.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return 0
    }
}
