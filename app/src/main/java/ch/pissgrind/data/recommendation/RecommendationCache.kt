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
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CachedRecommendationData(
    val profile: SerializableGenreProfile,
    val recommendations: List<SerializableScoredAlbum>,
    val allGenres: Set<String>,
    val lastRefreshTime: Long
)

@Serializable
data class SerializableGenreProfile(
    val genreWeights: Map<String, Float>,
    val topGenre: String = "",
    val totalAlbumsAnalyzed: Int = 0
)

@Serializable
data class SerializableScoredAlbum(
    val albumId: String,
    val name: String,
    val artist: String,
    val artistId: String,
    val coverArtId: String,
    val genres: List<String>,
    val year: Int,
    val score: Float,
    val reason: String
)

@Singleton
class RecommendationCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "RecommendationCache"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val cacheFile: File get() = File(context.filesDir, "recommendation_cache.json")

    suspend fun save(
        profile: GenreProfile,
        recommendations: List<ScoredAlbum>,
        allGenres: Set<String>
    ) = withContext(Dispatchers.IO) {
        try {
            val data = CachedRecommendationData(
                profile = profile.toSerializable(),
                recommendations = recommendations.map { it.toSerializable() },
                allGenres = allGenres,
                lastRefreshTime = System.currentTimeMillis()
            )
            cacheFile.writeText(json.encodeToString(data))
            Log.d(TAG, "Saved ${recommendations.size} recommendations to disk")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cache", e)
        }
    }

    suspend fun load(): CachedRecommendationData? = withContext(Dispatchers.IO) {
        try {
            if (!cacheFile.exists()) return@withContext null
            val text = cacheFile.readText()
            if (text.isBlank()) return@withContext null
            json.decodeFromString<CachedRecommendationData>(text)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cache", e)
            null
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        cacheFile.delete()
    }

    private fun GenreProfile.toSerializable() = SerializableGenreProfile(
        genreWeights = genreWeights,
        topGenre = topGenre,
        totalAlbumsAnalyzed = totalAlbumsAnalyzed
    )

    private fun ScoredAlbum.toSerializable() = SerializableScoredAlbum(
        albumId = albumId,
        name = name,
        artist = artist,
        artistId = artistId,
        coverArtId = coverArtId,
        genres = genres,
        year = year,
        score = score,
        reason = reason
    )
}

fun SerializableGenreProfile.toDomain() = GenreProfile(
    genreWeights = genreWeights,
    topGenre = topGenre,
    totalAlbumsAnalyzed = totalAlbumsAnalyzed
)

fun SerializableScoredAlbum.toDomain() = ScoredAlbum(
    albumId = albumId,
    name = name,
    artist = artist,
    artistId = artistId,
    coverArtId = coverArtId,
    genres = genres,
    year = year,
    score = score,
    reason = reason
)
