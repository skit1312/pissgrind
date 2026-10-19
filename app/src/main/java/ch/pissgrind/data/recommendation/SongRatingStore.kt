package ch.pissgrind.data.recommendation

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SongRating(
    val liked: Boolean,
    val timestamp: Long,
    val albumId: String = ""
)

enum class RatingResult { NEW, CHANGED, UNCHANGED }

/**
 * Stores one rating per song (navidromeID).
 * Naturally bounded by library size; MAX_ENTRIES only a safety net.
 * Stored LOCALLY only (not synced to the server).
 */
@Singleton
class SongRatingStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "SongRatingStore"
    private val MAX_ENTRIES = 20_000

    private val file = File(context.filesDir, "song_ratings.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var loaded = false

    // LinkedHashMap keeps insertion order -> first entry = oldest
    private val ratingsMap = LinkedHashMap<String, SongRating>()

    private val _ratings = MutableStateFlow<Map<String, SongRating>>(emptyMap())
    val ratings: StateFlow<Map<String, SongRating>> = _ratings.asStateFlow()

    suspend fun load() = mutex.withLock { ensureLoaded() }

    suspend fun getRating(songId: String): SongRating? = mutex.withLock {
        ensureLoaded()
        ratingsMap[songId]
    }

    suspend fun setRating(songId: String, liked: Boolean, albumId: String = ""): RatingResult =
        mutex.withLock {
            ensureLoaded()
            val existing = ratingsMap[songId]
            if (existing != null && existing.liked == liked) return@withLock RatingResult.UNCHANGED

            // remove + put = moves the entry to the end (newest)
            ratingsMap.remove(songId)
            ratingsMap[songId] = SongRating(liked, System.currentTimeMillis(), albumId)

            // Cap: drop oldest entries
            while (ratingsMap.size > MAX_ENTRIES) {
                ratingsMap.remove(ratingsMap.keys.first())
            }

            persist()
            _ratings.value = ratingsMap.toMap()
            if (existing == null) RatingResult.NEW else RatingResult.CHANGED
        }

    private suspend fun ensureLoaded() {
        if (loaded) return
        withContext(Dispatchers.IO) {
            try {
                if (file.exists()) {
                    val data: Map<String, SongRating> = json.decodeFromString(file.readText())
                    data.entries
                        .sortedBy { it.value.timestamp }          // oldest first
                        .forEach { ratingsMap[it.key] = it.value }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load ratings, starting fresh", e)
            }
        }
        _ratings.value = ratingsMap.toMap()
        loaded = true
    }

    private suspend fun persist() = withContext(Dispatchers.IO) {
        try {
            // Atomic write: temp file -> rename (no corruption on crash)
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(json.encodeToString(ratingsMap.toMap()))
            tmp.renameTo(file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist ratings", e)
        }
    }
}


