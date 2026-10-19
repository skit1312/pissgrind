package ch.pissgrind.data.recommendation

import android.content.Context
import android.util.Log
import ch.pissgrind.data.datasource.account.AccountApiService
import ch.pissgrind.data.repository.GenreRepository
import ch.pissgrind.managers.NavidromeManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class TrainingFeedback(
    val albumId: String,
    val genres: List<String>,
    val liked: Boolean,
    val artistId: String = "",
    val artistName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class TrainingStore(
    val feedback: List<TrainingFeedback> = emptyList()
)

@Singleton
class TrainingDataManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountApiService: AccountApiService,
    private val genreRepository: GenreRepository
){
    private val TAG = "TrainingData"
    private val json = Json { ignoreUnknownKeys = true }
    private val file: File get() = File(context.filesDir, "recommendation_training.json")
    private var store: TrainingStore? = null
    // Thresholds
    private val excludeThreshold = -3
    private val boostThreshold = 3
    private val artistExcludeStrikes = 1
    // Caps
    private val genreBoostPerAlbum = 0.05f
    private val genreBoostCap = 0.15f
    private val artistBoostMultiplier = 1.10f
    private val syncDebounceMs = 30_000L
    private var lastSyncTime = 0L

    suspend fun getFeedback(): List<TrainingFeedback> = withContext(Dispatchers.IO) {
        loadStore().feedback
    }

    fun exportAsJson(): String {
        val current = loadStore()
        return json.encodeToString(current)
    }

    suspend fun importFromJson(jsonString: String) = withContext(Dispatchers.IO) {
        try {
            val imported = json.decodeFromString<TrainingStore>(jsonString)
            store = imported
            saveStore(imported)
            Log.d(TAG, "Imported ${imported.feedback.size} feedback entries from server")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import training data", e)
        }
    }

    suspend fun syncToServer() {
        val now = System.currentTimeMillis()
        if (now - lastSyncTime < syncDebounceMs) return
        lastSyncTime = now

        val server = NavidromeManager.getCurrentServer() ?: return
        val data = exportAsJson()

        accountApiService.syncTrainingData(
            username = server.username,
            password = server.password,
            trainingData = data
        )
        Log.d(TAG, "Training data synced to server")
    }

    suspend fun loadFromServer(): Boolean {
        val server = NavidromeManager.getCurrentServer() ?: return false

        val remoteData = accountApiService.getTrainingData(
            username = server.username,
            password = server.password
        ) ?: return false

        if (remoteData.isBlank()) return false

        val cleanJson = remoteData
            .trim()
            .removeSurrounding("\"")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "")
            .replace("\\t", "")

        if (!cleanJson.startsWith("{")) {
            Log.w(TAG, "Invalid JSON from server, skipping: ${cleanJson.take(50)}")
            return false
        }

        val localStore = loadStore()
        val remoteStore = try {
            json.decodeFromString<TrainingStore>(cleanJson)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse remote training data", e)
            return false
        }

        if (localStore.feedback.isEmpty() || remoteStore.feedback.size > localStore.feedback.size) {
            store = remoteStore
            saveStore(remoteStore)
            Log.d(TAG, "Loaded ${remoteStore.feedback.size} entries from server (remote had more data)")
            return true
        }

        Log.d(TAG, "Local data kept (${localStore.feedback.size} entries vs ${remoteStore.feedback.size} remote)")
        return false
    }

    suspend fun addFeedback(albumId: String, genres: List<String>, liked: Boolean, artistId: String = "", artistName: String = "") =
        withContext(Dispatchers.IO) {
            val normalizedGenres = genreRepository.normalizeAll(genres)
            val current = loadStore()
            val updated = current.copy(
                feedback = current.feedback + TrainingFeedback(
                    albumId = albumId,
                    genres = normalizedGenres,
                    liked = liked,
                    artistId = artistId,
                    artistName = artistName
                )
            )
            store = updated
            saveStore(updated)
            Log.d(TAG, "Feedback added: $albumId, liked=$liked, genres=$genres, artist=$artistName")

            syncToServer()
        }

    suspend fun getAlbumScore(albumId: String): Int = withContext(Dispatchers.IO) {
        val feedback = loadStore().feedback.filter { it.albumId == albumId }
        val likes = feedback.count { it.liked }
        val dislikes = feedback.count { !it.liked }
        likes - dislikes
    }

    suspend fun getExcludedAlbumIds(): Set<String> = withContext(Dispatchers.IO) {
        val feedback = loadStore().feedback
        val albumScores = mutableMapOf<String, Int>()

        feedback.forEach { entry ->
            val current = albumScores[entry.albumId] ?: 0
            albumScores[entry.albumId] = if (entry.liked) current + 1 else current - 1
        }

        albumScores
            .filter { (_, score) -> score <= excludeThreshold }
            .keys
    }

    suspend fun getBoostedAlbumIds(): Set<String> = withContext(Dispatchers.IO) {
        val feedback = loadStore().feedback
        val albumScores = mutableMapOf<String, Int>()

        feedback.forEach { entry ->
            val current = albumScores[entry.albumId] ?: 0
            albumScores[entry.albumId] = if (entry.liked) current + 1 else current - 1
        }

        albumScores
            .filter { (_, score) -> score >= boostThreshold }
            .keys
    }

    suspend fun getArtistAdjustments(): Map<String, Float> = withContext(Dispatchers.IO) {
        val feedback = loadStore().feedback
        val albumScores = mutableMapOf<String, Int>()
        val albumArtists = mutableMapOf<String, String>() // albumId -> artistId

        feedback.forEach { entry ->
            val current = albumScores[entry.albumId] ?: 0
            albumScores[entry.albumId] = if (entry.liked) current + 1 else current - 1
            if (entry.artistId.isNotBlank()) {
                albumArtists[entry.albumId] = entry.artistId
            }
        }

        val artistStrikes = mutableMapOf<String, Int>()
        val artistBoosts = mutableSetOf<String>()

        albumScores.forEach { (albumId, score) ->
            val artistId = albumArtists[albumId] ?: return@forEach
            if (score <= excludeThreshold) {
                artistStrikes[artistId] = (artistStrikes[artistId] ?: 0) + 1
            }
            if (score >= boostThreshold) {
                artistBoosts.add(artistId)
            }
        }

        val adjustments = mutableMapOf<String, Float>()

        // Apply strikes
        artistStrikes.forEach { (artistId, strikes) ->
            adjustments[artistId] = when {
                strikes >= artistExcludeStrikes -> 0.0f  // Excluded
                strikes == 2 -> 0.40f
                strikes == 1 -> 0.75f
                else -> 1.0f
            }
        }

        // Apply boost
        artistBoosts.forEach { artistId ->
            val current = adjustments[artistId] ?: 1.0f
            if (current >= 1.0f) {
                adjustments[artistId] = artistBoostMultiplier
            }
        }

        Log.d(TAG, "Artist adjustments: $adjustments")
        adjustments
    }

    suspend fun getGenreAdjustments(): Map<String, Float> = withContext(Dispatchers.IO) {
        val feedback = loadStore().feedback
        if (feedback.isEmpty()) return@withContext emptyMap()

        // Base genre adjustments
        val genreLikes = mutableMapOf<String, Int>()
        val genreDislikes = mutableMapOf<String, Int>()

        feedback.forEach { entry ->
            entry.genres.forEach { genre ->
                val normalized = genre.lowercase()
                if (entry.liked) {
                    genreLikes[normalized] = (genreLikes[normalized] ?: 0) + 1
                } else {
                    genreDislikes[normalized] = (genreDislikes[normalized] ?: 0) + 1
                }
            }
        }

        val allGenres = (genreLikes.keys + genreDislikes.keys).toSet()
        val adjustments = mutableMapOf<String, Float>()

        allGenres.forEach { genre ->
            val likes = genreLikes[genre] ?: 0
            val dislikes = genreDislikes[genre] ?: 0
            val total = likes + dislikes
            val ratio = if (total > 0) likes.toFloat() / total else 0.5f
            adjustments[genre] = 0.3f + (ratio * 1.5f)
        }

        val albumScores = mutableMapOf<String, Int>()
        val albumGenres = mutableMapOf<String, List<String>>()

        feedback.forEach { entry ->
            val current = albumScores[entry.albumId] ?: 0
            albumScores[entry.albumId] = if (entry.liked) current + 1 else current - 1
            albumGenres[entry.albumId] = entry.genres.map { it.lowercase() }
        }

        val genreBoosts = mutableMapOf<String, Int>()

        albumScores.filter { (_, score) -> score >= boostThreshold }.forEach { (albumId, _) ->
            albumGenres[albumId]?.forEach { genre ->
                genreBoosts[genre] = (genreBoosts[genre] ?: 0) + 1
            }
        }

        // Apply capped genre boosts
        genreBoosts.forEach { (genre, count) ->
            val boost = (count * genreBoostPerAlbum).coerceAtMost(genreBoostCap)
            val current = adjustments[genre] ?: 1.0f
            adjustments[genre] = current + boost
        }

        Log.d(TAG, "Genre adjustments: ${adjustments.entries.take(5)}")
        adjustments
    }

    suspend fun getExcludedArtistIds(): Set<String> = withContext(Dispatchers.IO) {
        getArtistAdjustments()
            .filter { (_, multiplier) -> multiplier == 0.0f }
            .keys
    }


    suspend fun getAlbumScoreDisplay(albumId: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val score = getAlbumScore(albumId)
        val threshold = if (score < 0) excludeThreshold else boostThreshold
        score to threshold
    }

    /**
     * Legacy compatibility: get dislike count.
     */
    suspend fun getDislikeCount(albumId: String): Int = withContext(Dispatchers.IO) {
        loadStore().feedback.count { it.albumId == albumId && !it.liked }
    }

    /**
     * Kept for compatibility, now uses net score system.
     */
    @Deprecated("Use getExcludedAlbumIds() instead", ReplaceWith("getExcludedAlbumIds()"))
    suspend fun getDislikedAlbumIds(): Set<String> = getExcludedAlbumIds()

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        store = TrainingStore()
        saveStore(TrainingStore())
    }

    private fun loadStore(): TrainingStore {
        if (store != null) return store!!
        return try {
            if (!file.exists()) TrainingStore()
            else json.decodeFromString<TrainingStore>(file.readText())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load training data", e)
            TrainingStore()
        }.also { store = it }
    }

    private fun saveStore(data: TrainingStore) {
        try {
            file.writeText(json.encodeToString(data))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save training data", e)
        }
    }
    /**
     * Wipes the server-side training data by overwriting it with an empty store.
     * Bypasses the sync debounce used by the "Wipe Algorithm" action.
     */
    suspend fun wipeServerData() = withContext(Dispatchers.IO) {
        val server = NavidromeManager.getCurrentServer() ?: return@withContext
        try {
            accountApiService.syncTrainingData(
                username = server.username,
                password = server.password,
                trainingData = json.encodeToString(TrainingStore())   // {"feedback":[]}
            )
            Log.d(TAG, "Server training data wiped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to wipe server training data", e)
        }
    }
}
