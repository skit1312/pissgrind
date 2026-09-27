package ch.pissgrind.data.recommendation

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class LogEntry(
    val timestamp: Long,
    val action: String,        // THUMBS_UP, THUMBS_DOWN, PROFILE_BUILT, RECOMMENDATIONS_GENERATED
    val albumId: String = "",
    val albumName: String = "",
    val artist: String = "",
    val genres: List<String> = emptyList(),
    val details: String = ""   // Additional context
)

@Serializable
data class AlgorithmLogStore(
    val entries: List<LogEntry> = emptyList()
)

data class AlgorithmStats(
    val totalFeedback: Int,
    val thumbsUp: Int,
    val thumbsDown: Int,
    val genresBoosted: Int,
    val genresPenalized: Int,
    val albumsExcluded: Int,
    val artistsBoosted: Int,
    val artistsPenalized: Int,
    val artistsExcluded: Int,
    val topBoostedGenres: List<Pair<String, Int>>,   // genre -> like count
    val topPenalizedGenres: List<Pair<String, Int>>  // genre -> dislike count
)


@Singleton
class AlgorithmLogger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "AlgorithmLogger"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file: File get() = File(context.filesDir, "algorithm_log.json")

    private var store: AlgorithmLogStore? = null

    // ===== LOGGING =====

    suspend fun logThumbsUp(albumId: String, albumName: String, artist: String, genres: List<String>, albumScore: Int) {
        val boostNote = if (albumScore >= 2) {
            "Album reached +3. Artist boosted +10%, genres [${genres.joinToString()}] boosted +5% each."
        } else {
            "Score $albumScore/+3 - not yet boosting artist/genre."
        }

        addEntry(LogEntry(
            timestamp = System.currentTimeMillis(),
            action = "THUMBS_UP",
            albumId = albumId,
            albumName = albumName,
            artist = artist,
            genres = genres,
            details = "User liked this album (score: $albumScore). $boostNote"
        ))
    }

    suspend fun logThumbsDown(albumId: String, albumName: String, artist: String, genres: List<String>, albumScore: Int, artistStrikes: Int = 0) {
        val exclusionNote = if (albumScore <= -3) {
            "Album now Excluded from Recommendations."
        } else {
            "Score $albumScore/-3 - Album not yet excluded."
        }

        val artistNote = when (artistStrikes) {
            0 -> ""
            1 -> " Artist has 1 strike (weight -25%)."
            2 -> " Artist has 2 strikes (weight -60%)."
            else -> " Artist Excluded (3 strikes)."
        }

        addEntry(LogEntry(
            timestamp = System.currentTimeMillis(),
            action = "THUMBS_DOWN",
            albumId = albumId,
            albumName = albumName,
            artist = artist,
            genres = genres,
            details = "User disliked this album (score: $albumScore). $exclusionNote$artistNote"
        ))
    }


    suspend fun logProfileBuilt(topGenres: List<Pair<String, Float>>, totalAlbums: Int) {
        addEntry(LogEntry(
            timestamp = System.currentTimeMillis(),
            action = "PROFILE_BUILT",
            details = "Genre profile rebuilt from $totalAlbums albums. Top genres: ${
                topGenres.take(5).joinToString { "${it.first} (${(it.second * 100).toInt()}%)" }
            }"
        ))
    }

    suspend fun logRecommendationsGenerated(count: Int, topAlbums: List<ScoredAlbum>) {
        addEntry(LogEntry(
            timestamp = System.currentTimeMillis(),
            action = "RECOMMENDATIONS_GENERATED",
            details = "Generated $count recommendations. Top picks: ${
                topAlbums.take(3).joinToString { "${it.name} by ${it.artist} (score: ${"%.2f".format(it.score)})" }
            }"
        ))
    }

    suspend fun logGenreAdjustment(genre: String, multiplier: Float, reason: String) {
        addEntry(LogEntry(
            timestamp = System.currentTimeMillis(),
            action = "GENRE_ADJUSTMENT",
            genres = listOf(genre),
            details = "$genre → ${(multiplier * 100).toInt()}% weight. Reason: $reason"
        ))
    }

    // ===== EXPORT =====

    suspend fun exportFullLog(context: Context): File = withContext(Dispatchers.IO) {
        val entries = loadStore().entries
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val outputFile = File(context.cacheDir, "pissgrind_algorithm_full.log")

        val content = buildString {
            appendLine("# PISSGRIND Algorithm Full Log")
            appendLine("# Generated: ${dateFormat.format(Date())}")
            appendLine("# Entries: ${entries.size}")
            appendLine("# ─────────────────────────────────────")
            appendLine()

            entries.forEach { entry ->
                val date = dateFormat.format(Date(entry.timestamp))
                appendLine("[$date] [${entry.action}]")
                if (entry.albumId.isNotBlank()) appendLine("  Album ID: ${entry.albumId}")
                if (entry.albumName.isNotBlank()) appendLine("  Album: ${entry.albumName}")
                if (entry.artist.isNotBlank()) appendLine("  Artist: ${entry.artist}")
                if (entry.genres.isNotEmpty()) appendLine("  Genres: ${entry.genres.joinToString()}")
                if (entry.details.isNotBlank()) appendLine("  Details: ${entry.details}")
                appendLine()
            }
        }

        outputFile.writeText(content)
        outputFile
    }

    // ===== STATS =====

    fun getStats(): AlgorithmStats {
        val entries = loadStore().entries
        val thumbsUp = entries.count { it.action == "THUMBS_UP" }
        val thumbsDown = entries.count { it.action == "THUMBS_DOWN" }

        val likedGenres = entries
            .filter { it.action == "THUMBS_UP" }
            .flatMap { it.genres }
            .map { it.lowercase() }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }

        val dislikedGenres = entries
            .filter { it.action == "THUMBS_DOWN" }
            .flatMap { it.genres }
            .map { it.lowercase() }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }

        // Count artists with boost/penalty mentions in log details
        val artistsWithStrikes = entries
            .filter { it.action == "THUMBS_DOWN" && it.details.contains("strike") }
            .map { it.artist }
            .distinct()

        val artistsExcluded = entries
            .filter { it.action == "THUMBS_DOWN" && it.details.contains("EXCLUDED") && it.details.contains("3 strikes") }
            .map { it.artist }
            .distinct()

        val artistsBoosted = entries
            .filter { it.action == "THUMBS_UP" && it.details.contains("Artist boosted") }
            .map { it.artist }
            .distinct()

        val albumsExcluded = entries
            .filter { it.action == "THUMBS_DOWN" && it.details.contains("Album now EXCLUDED") }
            .map { it.albumId }
            .distinct()

        return AlgorithmStats(
            totalFeedback = thumbsUp + thumbsDown,
            thumbsUp = thumbsUp,
            thumbsDown = thumbsDown,
            genresBoosted = likedGenres.size,
            genresPenalized = dislikedGenres.size,
            albumsExcluded = albumsExcluded.size,
            artistsBoosted = artistsBoosted.size,
            artistsPenalized = artistsWithStrikes.size,
            artistsExcluded = artistsExcluded.size,
            topBoostedGenres = likedGenres.take(5),
            topPenalizedGenres = dislikedGenres.take(5)
        )
    }


    // ===== MANAGEMENT =====

    suspend fun clearLog() = withContext(Dispatchers.IO) {
        store = AlgorithmLogStore()
        file.writeText(json.encodeToString(AlgorithmLogStore()))
    }

    // ===== INTERNAL =====

    private suspend fun addEntry(entry: LogEntry) = withContext(Dispatchers.IO) {
        try {
            val current = loadStore()
            val updated = current.copy(entries = current.entries + entry)
            store = updated
            file.writeText(json.encodeToString(updated))
            Log.d(TAG, "[${entry.action}] ${entry.details}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add log entry", e)
        }
    }

    private fun loadStore(): AlgorithmLogStore {
        if (store != null) return store!!
        return try {
            if (!file.exists()) AlgorithmLogStore()
            else json.decodeFromString<AlgorithmLogStore>(file.readText())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load log", e)
            AlgorithmLogStore()
        }.also { store = it }
    }
}
