package ch.pissgrind.data.recommendation

import ch.pissgrind.managers.NavidromeManager

/**
 * Genre weight profile for a user.
 * Maps genre names (lowercase) to their normalized weight (0.0 - 1.0).
 */
data class GenreProfile(
    val genreWeights: Map<String, Float>,
    val topGenre: String = genreWeights.maxByOrNull { it.value }?.key ?: "",
    val totalAlbumsAnalyzed: Int = 0
) {
    fun getWeight(genre: String): Float {
        return genreWeights[genre.lowercase()] ?: 0f
    }
}
fun AlbumGenreData.getCoverArtUrl(size: Int = 300): String {
    val server = NavidromeManager.getCurrentServer() ?: return ""
    return "${server.url.trimEnd('/')}/rest/getCoverArt.view?id=${coverArtId}&size=$size&u=${server.username}&t=...&s=...&v=1.16.1&c=pissgrind&f=json"
}

/**
 * An album with its genres and calculated recommendation score.
 */
data class ScoredAlbum(
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

/**
 * Raw album data with genres fetched from the API.
 */
data class AlbumGenreData(
    val albumId: String,
    val name: String,
    val artist: String,
    val artistId: String,
    val coverArtId: String,
    val year: Int,
    val genres: List<String>,
    val playCount: Int = 0
)

/**
 * Configuration for the recommendation engine.
 */
data class RecommendationConfig(
    val maxRecommendations: Int = 50,
    val maxPerArtist: Int = 2,
    val recencyBias: Float = 1.3f,
    val diversityFactor: Float = 0.2f,
    val minScore: Float = 0.05f,
    val frequentAlbumsCount: Int = 50,
    val recentAlbumsCount: Int = 30
)
