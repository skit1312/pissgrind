package ch.pissgrind.data.recommendation

import android.util.Log
import ch.pissgrind.data.datasource.navidrome.NavidromeDataSource
import ch.pissgrind.data.repository.GenreRepository
import ch.pissgrind.managers.NavidromeManager
import ch.pissgrind.managers.settings.ExcludedAlbumsManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex

@Singleton
class RecommendationRepository @Inject constructor(
    private val cache: RecommendationCache,
    private val trainingDataManager: TrainingDataManager,
    private val genreRepository: GenreRepository,
    private val algorithmLogger: AlgorithmLogger,
    private val excludedAlbumsManager: ExcludedAlbumsManager
) {
    private val TAG = "RecommendationRepo"
    private val engine = RecommendationEngine(genreRepository)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    // In memory cache
    private var cachedProfile: GenreProfile? = null
    private var cachedRecommendations: List<ScoredAlbum>? = null
    private var cachedAllGenres: Set<String>? = null
    private var frequentAlbumsCache: List<AlbumGenreData> = emptyList()
    private var recentAlbumsCache: List<AlbumGenreData> = emptyList()
    private var playedIdsCache: Set<String> = emptySet()

    private var lastRefreshTime: Long = 0
    private val cacheValidityMs = 16 * 60 * 60 * 1000L
    private val refreshMutex = Mutex()
    private val maxDailyRefreshes = 6
    private val refreshCooldownMs = 30 * 60 * 1000L
    private var dailyRefreshCount = 0
    private var lastDailyReset = 0L
    private var lastManualRefreshTime = 0L

    /**
     * Manual refresh triggered by user. Enforces daily limit + cooldown.
     * Returns: Pair(recommendations, cooldownMs) where cooldownMs is:
     *   0 = success, >0 = remaining cooldown, -1 = daily limit reached
     */
    suspend fun manualRefresh(): Pair<List<ScoredAlbum>, Long> {
        val now = System.currentTimeMillis()

        // Reset daily counter at midnight
        val today = now / (24 * 60 * 60 * 1000L)
        if (today != lastDailyReset / (24 * 60 * 60 * 1000L)) {
            dailyRefreshCount = 0
            lastDailyReset = now
        }

        // Check daily limit
        if (dailyRefreshCount >= maxDailyRefreshes) {
            return Pair(cachedRecommendations ?: emptyList(), -1L)
        }

        // Check cooldown
        val timeSinceLastRefresh = now - lastManualRefreshTime
        if (timeSinceLastRefresh < refreshCooldownMs) {
            val remaining = refreshCooldownMs - timeSinceLastRefresh
            return Pair(cachedRecommendations ?: emptyList(), remaining)
        }

        // Do refresh
        lastManualRefreshTime = now
        dailyRefreshCount++

        // Invalidate cache to force fresh fetch
        cachedRecommendations = null
        lastRefreshTime = 0

        val result = refreshRecommendations()
        return Pair(result, 0L)
    }

    fun getRemainingRefreshes(): Int {
        val now = System.currentTimeMillis()
        val today = now / (24 * 60 * 60 * 1000L)
        if (today != lastDailyReset / (24 * 60 * 60 * 1000L)) {
            return maxDailyRefreshes
        }
        return (maxDailyRefreshes - dailyRefreshCount).coerceAtLeast(0)
    }

    fun getCooldownRemaining(): Long {
        val now = System.currentTimeMillis()
        val elapsed = now - lastManualRefreshTime
        return if (elapsed >= refreshCooldownMs) 0L else refreshCooldownMs - elapsed
    }

    /**
     * Get recommendations (uses memory cache -> disk cache -> fresh fetch).
     * Applies time-based rotation for variety.
     */
    suspend fun getRecommendations(forceRefresh: Boolean = false): List<ScoredAlbum> {
        // 1. Memory cache (valid)
        if (!forceRefresh && cachedRecommendations != null &&
            System.currentTimeMillis() - lastRefreshTime < cacheValidityMs
        ) {
            return applyRotation(cachedRecommendations!!)
        }

        // 2. Disk cache (valid)
        if (!forceRefresh) {
            val diskData = cache.load()
            if (diskData != null && System.currentTimeMillis() - diskData.lastRefreshTime < cacheValidityMs) {
                Log.d(TAG, "Loaded from disk cache (valid)")
                cachedProfile = diskData.profile.toDomain()
                cachedRecommendations = diskData.recommendations.map { it.toDomain() }
                cachedAllGenres = diskData.allGenres
                lastRefreshTime = diskData.lastRefreshTime
                return applyRotation(cachedRecommendations!!)
            }
        }

        // 3. Fresh fetch
        val freshResult = refreshRecommendations()

        // 4. If refresh failed, fall back to stale disk cache
        if (freshResult.isEmpty()) {
            val staleData = cache.load()
            if (staleData != null && staleData.recommendations.isNotEmpty()) {
                Log.d(TAG, "Server fetch failed, using stale disk cache")
                cachedProfile = staleData.profile.toDomain()
                cachedRecommendations = staleData.recommendations.map { it.toDomain() }
                cachedAllGenres = staleData.allGenres
                lastRefreshTime = staleData.lastRefreshTime
                return applyRotation(cachedRecommendations!!)
            }
        }

        return applyRotation(freshResult)
    }

    /**
     * Get the user's genre profile.
     */
    suspend fun getGenreProfile(forceRefresh: Boolean = false): GenreProfile {
        if (!forceRefresh && cachedProfile != null) return cachedProfile!!

        if (!forceRefresh) {
            val diskData = cache.load()
            if (diskData != null) {
                cachedProfile = diskData.profile.toDomain()
                return cachedProfile!!
            }
        }

        refreshRecommendations()
        return cachedProfile ?: GenreProfile(emptyMap())
    }

    /**
     * Get all genres available in the library.
     */
    suspend fun getAllGenres(): Set<String> {
        if (cachedAllGenres != null) return cachedAllGenres!!

        val diskData = cache.load()
        if (diskData != null) {
            cachedAllGenres = diskData.allGenres
            return cachedAllGenres!!
        }

        refreshRecommendations()
        return cachedAllGenres ?: emptySet()
    }

    /**
     * Submit training feedback and invalidate cache.
     */
    suspend fun submitFeedback(albumId: String, genres: List<String>, liked: Boolean, artistId: String = "", artistName: String = "") {
        trainingDataManager.addFeedback(albumId, genres, liked, artistId, artistName)
        cachedRecommendations = null
        lastRefreshTime = 0
    }

    /**
     * Rotates Recommendations
     * Changes every [6] hours.
     */
    private fun applyRotation(recommendations: List<ScoredAlbum>): List<ScoredAlbum> {
        if (recommendations.size <= 5) return recommendations

        val rotationPeriod = 6 * 60 * 60 * 1000L
        val seed = System.currentTimeMillis() / rotationPeriod
        val random = java.util.Random(seed)

        // Keep top 5 in the mix, shuffle the rest
        val top = recommendations.take(5)
        val rest = recommendations.drop(5).shuffled(random)

        // Interleave top and rest for variety
        val result = mutableListOf<ScoredAlbum>()
        val topShuffled = top.shuffled(random)

        var topIdx = 0
        var restIdx = 0
        for (i in recommendations.indices) {
            if (topIdx < topShuffled.size && (i % 3 == 0 || restIdx >= rest.size)) {
                result.add(topShuffled[topIdx++])
            } else if (restIdx < rest.size) {
                result.add(rest[restIdx++])
            }
        }

        return result
    }
    /**
     * Recompute recommendations from in-memory caches (NO network).
     * Used when the manual exclude list changes. Falls back to a full
     * network refresh only if the album cache isn't populated yet.
     */
    suspend fun recomputeFromCache(): List<ScoredAlbum> = withContext(Dispatchers.IO) {
        val allAlbums = allAlbumsCache
        if (allAlbums.isEmpty()) {
            // Cache not ready → fall back to full refresh
            return@withContext refreshRecommendations()
        }

        val config = RecommendationConfig()

        // Rebuild profile from cached inputs (identical to the engine's path)
        val profile = engine.buildGenreProfile(
            frequentAlbumsCache,
            recentAlbumsCache,
            config
        )

        // Union of training-based and manual excludes
        val genreAdjustments = trainingDataManager.getGenreAdjustments()
        val trainingExcludes = trainingDataManager.getExcludedAlbumIds()
        val manualExcludes = excludedAlbumsManager.excludedAlbumIds.first()
        val excludedAlbumIds = trainingExcludes + manualExcludes
        val artistAdjustments = trainingDataManager.getArtistAdjustments()

        val scored = engine.scoreAlbums(
            candidates = allAlbums,
            profile = profile,
            playedAlbumIds = playedIdsCache,
            config = config,
            genreAdjustments = genreAdjustments,
            excludedAlbumIds = excludedAlbumIds,
            artistAdjustments = artistAdjustments
        )
        val recommendations = engine.applyFilters(scored, config)

        // Update caches so subsequent reads are consistent
        cachedProfile = profile
        cachedRecommendations = recommendations
        lastRefreshTime = System.currentTimeMillis()
        cache.save(profile, recommendations, cachedAllGenres ?: emptySet())

        Log.d(TAG, "Recomputed from Cache: ${recommendations.size} Recommendations")
        recommendations
    }

    /**
     * Refresh all recommendation data from Navidrome and persist to disk.
     */
    private suspend fun refreshRecommendations(): List<ScoredAlbum> = withContext(Dispatchers.IO) {
        if (!refreshMutex.tryLock()) {
            Log.d(TAG, "Refresh already in progress, skipping")
            return@withContext cachedRecommendations ?: emptyList()
        }

        try {
            Log.d(TAG, "Refreshing recommendations from server...")

            // 1. Get frequent and recent album IDs
            val frequentAlbumIds = getAlbumIds("frequent", 50)
            val recentAlbumIds = getAlbumIds("recent", 30)

            Log.d(
                TAG,
                "Found ${frequentAlbumIds.size} frequent, ${recentAlbumIds.size} recent albums"
            )

            // 2. Fetch genre data for frequent/recent albums
            val frequentAlbums = fetchAlbumsWithGenres(frequentAlbumIds)
            val recentAlbums = fetchAlbumsWithGenres(recentAlbumIds)

            // 3. Get ALL albums in library for scoring (paginated)
            val allAlbumIds = mutableListOf<String>()
            var offset = 0
            val pageSize = 500
            while (true) {
                val page = getAlbumIds("alphabeticalByName", pageSize, offset)
                allAlbumIds.addAll(page)
                if (page.size < pageSize) break
                offset += pageSize
            }

            val allAlbums = fetchAlbumsWithGenres(allAlbumIds)
            allAlbumsCache = allAlbums

            // 4. Collect all Genres (filtered)
            val allGenres = allAlbums
                .flatMap { genreRepository.normalizeAll(it.genres) }
                .toSet()

            Log.d(TAG, "Total genres found: ${allGenres.size}")

            // 5. Build Played Set
            val playedIds = (frequentAlbumIds + recentAlbumIds).toSet()

            // Cache Inputs
            frequentAlbumsCache = frequentAlbums
            recentAlbumsCache = recentAlbums
            playedIdsCache = playedIds

            // 6. Get training adjustments
            val genreAdjustments = trainingDataManager.getGenreAdjustments()
            val trainingExcludes = trainingDataManager.getExcludedAlbumIds()
            val manualExcludes = excludedAlbumsManager.excludedAlbumIds.first()
            val excludedAlbumIds = trainingExcludes + manualExcludes
            val artistAdjustments = trainingDataManager.getArtistAdjustments()

            // 7. Run Recommendation Engine
            val config = RecommendationConfig()
            val profile = engine.buildGenreProfile(frequentAlbums, recentAlbums, config)
            val recommendations = engine.getRecommendations(
                frequentAlbums = frequentAlbums,
                recentAlbums = recentAlbums,
                allAlbums = allAlbums,
                playedAlbumIds = playedIds,
                config = config,
                genreAdjustments = genreAdjustments,
                excludedAlbumIds = excludedAlbumIds,
                artistAdjustments = artistAdjustments
            )

            // Log profile
            val topGenres = profile.genreWeights.entries
                .sortedByDescending { it.value }
                .take(5)
                .map { it.key to it.value }
            algorithmLogger.logProfileBuilt(topGenres, profile.totalAlbumsAnalyzed)

            // Log recommendations
            algorithmLogger.logRecommendationsGenerated(recommendations.size, recommendations)

            // 8. Update memory cache
            cachedProfile = profile
            cachedRecommendations = recommendations
            cachedAllGenres = allGenres
            lastRefreshTime = System.currentTimeMillis()

            // 9. Persist to disk
            cache.save(profile, recommendations, allGenres)

            Log.d(TAG, "Recommendations refreshed and saved: ${recommendations.size} results")
            recommendations
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh recommendations", e)
            cachedRecommendations ?: emptyList()
        } finally {
            refreshMutex.unlock()
        }
    }

    /**
     * Get album IDs from getAlbumList2.
     */
    private suspend fun getAlbumIds(type: String, count: Int, offset: Int = 0): List<String> {
        val url = buildApiUrl(
            "getAlbumList2.view", mapOf(
                "type" to type,
                "size" to count.toString(),
                "offset" to offset.toString()
            )
        ) ?: return emptyList()

        return try {
            val responseText: String = client.get(url).body()
            val jsonResponse = json.parseToJsonElement(responseText).jsonObject
            val subsonicResponse =
                jsonResponse["subsonic-response"]?.jsonObject ?: return emptyList()

            if (subsonicResponse["status"]?.jsonPrimitive?.contentOrNull != "ok") return emptyList()

            val albumList = subsonicResponse["albumList2"]?.jsonObject
                ?.get("album")?.jsonArray ?: return emptyList()

            albumList.mapNotNull { albumElement ->
                albumElement.jsonObject["id"]?.jsonPrimitive?.contentOrNull
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAlbumIds failed for type=$type: ${e.message}")
            emptyList()
        }
    }

    /**
     * Fetch full album details (with genres) for a list of album IDs.
     */
    private suspend fun fetchAlbumsWithGenres(albumIds: List<String>): List<AlbumGenreData> =
        withContext(Dispatchers.IO) {
            val semaphore = Semaphore(5)

            albumIds.map { albumId ->
                async {
                    semaphore.withPermit {
                        fetchAlbumDetail(albumId)
                    }
                }
            }.awaitAll().filterNotNull()
        }

    /**
     * Fetch single album's details including genres.
     */
    private suspend fun fetchAlbumDetail(albumId: String): AlbumGenreData? {
        val url = buildApiUrl("getAlbum.view", mapOf("id" to albumId)) ?: return null

        return try {
            val responseText: String = client.get(url).body()
            val jsonResponse = json.parseToJsonElement(responseText).jsonObject
            val subsonicResponse = jsonResponse["subsonic-response"]?.jsonObject ?: return null

            if (subsonicResponse["status"]?.jsonPrimitive?.contentOrNull != "ok") return null

            val albumJson = subsonicResponse["album"]?.jsonObject ?: return null
            engine.parseAlbumFromJson(albumJson)
        } catch (e: Exception) {
            Log.e(TAG, "fetchAlbumDetail failed for $albumId: ${e.message}")
            null
        }
    }

    /**
     * Build a full Navidrome API URL.
     */
    private fun buildApiUrl(endpoint: String, params: Map<String, String> = emptyMap()): String? {
        val server = NavidromeManager.getCurrentServer() ?: return null

        val salt = NavidromeDataSource.generateSalt(8)
        val token = NavidromeDataSource.md5Hash(server.password + salt)

        return buildString {
            append("${server.url.trimEnd('/')}/rest/$endpoint")
            append("?u=${server.username}")
            append("&t=$token")
            append("&s=$salt")
            append("&v=1.16.1")
            append("&c=Chora")
            append("&f=json")
            params.forEach { (key, value) ->
                append("&$key=$value")
            }
        }
    }
    /**
     * Get all albums with genre data.
     */
    suspend fun getAllAlbumsWithGenres(): List<AlbumGenreData> = withContext(Dispatchers.IO) {
        // Memory cache
        if (allAlbumsCache.isNotEmpty()) return@withContext allAlbumsCache

        // Fetch fresh
        val allAlbumIds = mutableListOf<String>()
        var offset = 0
        val pageSize = 500
        while (true) {
            val page = getAlbumIds("alphabeticalByName", pageSize, offset)
            allAlbumIds.addAll(page)
            if (page.size < pageSize) break
            offset += pageSize
        }

        val result = fetchAlbumsWithGenres(allAlbumIds)
        allAlbumsCache = result
        result
    }

    private var allAlbumsCache: List<AlbumGenreData> = emptyList()
    fun getAllAlbumsCached(): List<AlbumGenreData> {
        return allAlbumsCache
    }
}