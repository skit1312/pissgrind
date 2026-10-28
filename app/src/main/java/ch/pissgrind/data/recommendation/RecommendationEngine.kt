package ch.pissgrind.data.recommendation

import android.util.Log
import ch.pissgrind.data.repository.GenreRepository
import kotlinx.serialization.json.*

/**
 * Genre based recommendation engine.
 *
 * Algorithm:
 * 1. Build genre profile from user's listening history (frequent + recent albums)
 * 2. Score all albums in the library against the profile
 * 3. Filter out already-played albums, enforce diversity rules
 * 4. Return top N recommendations
 */
class RecommendationEngine(
    private val genreRepository: GenreRepository
) {
    companion object {
        private const val DAMPEN_FACTOR = 0.4f
    }

    private val TAG = "RecommendationEngine"
    private val DAMPENED_GENRES = setOf("Ambient")

    /**
     * Normalize and filter genres through GenreRepository.
     * Only valid, recognized genres pass through.
     */
    private fun filterGenres(genres: List<String>): List<String> {
        return genreRepository.normalizeAll(genres)
    }

    /**
     * Build a genre profile from user's listened albums.
     */
    fun buildGenreProfile(
        frequentAlbums: List<AlbumGenreData>,
        recentAlbums: List<AlbumGenreData>,
        config: RecommendationConfig = RecommendationConfig()
    ): GenreProfile {
        val genreCounts = mutableMapOf<String, Float>()

        frequentAlbums.forEach { album ->
            filterGenres(album.genres).forEach { genre ->
                val key = genre.lowercase()  // Store lowercase
                val weight = if (genre in DAMPENED_GENRES) DAMPEN_FACTOR else 1f
                genreCounts[key] = (genreCounts[key] ?: 0f) + weight
            }
        }

        recentAlbums.forEach { album ->
            filterGenres(album.genres).forEach { genre ->
                val key = genre.lowercase()  // Store lowercase
                val weight = if (genre in DAMPENED_GENRES) DAMPEN_FACTOR * config.recencyBias else config.recencyBias
                genreCounts[key] = (genreCounts[key] ?: 0f) + weight
            }
        }

        val maxCount = genreCounts.values.maxOrNull() ?: 1f
        val normalized = genreCounts.mapValues { (_, count) -> count / maxCount }

        Log.d(TAG, "Genre profile built: ${normalized.entries.sortedByDescending { it.value }.take(5)}")

        return GenreProfile(
            genreWeights = normalized,
            totalAlbumsAnalyzed = frequentAlbums.size + recentAlbums.size
        )
    }

    /**
     * Score all candidate albums against user's genre profile.
     */
    fun scoreAlbums(
        candidates: List<AlbumGenreData>,
        profile: GenreProfile,
        playedAlbumIds: Set<String>,
        config: RecommendationConfig = RecommendationConfig(),
        genreAdjustments: Map<String, Float> = emptyMap(),
        excludedAlbumIds: Set<String> = emptySet(),
        artistAdjustments: Map<String, Float> = emptyMap()
    ): List<ScoredAlbum> {
        val scored = candidates.mapNotNull { album ->
            // Skip excluded albums (score <= -4)
            if (album.albumId in excludedAlbumIds) return@mapNotNull null

            // Skip excluded artists (3 strikes)
            val artistMultiplier = artistAdjustments[album.artistId] ?: 1f
            if (artistMultiplier == 0f) return@mapNotNull null

            val filteredGenres = filterGenres(album.genres)
            if (filteredGenres.isEmpty()) return@mapNotNull null

            val genreScores = filteredGenres.map { genre ->
                val baseWeight = profile.getWeight(genre)
                val adjustment = genreAdjustments[genre.lowercase()] ?: 1f
                baseWeight * adjustment
            }

            val baseScore = genreScores.sum()

            val matchingGenres = filteredGenres.count { profile.getWeight(it) > 0.1f }
            val multiGenreBonus = if (matchingGenres > 1) 0.1f * (matchingGenres - 1) else 0f

            val playedPenalty = if (album.albumId in playedAlbumIds) 0.5f else 1f

            val finalScore = (baseScore + multiGenreBonus) * playedPenalty * artistMultiplier

            if (finalScore < config.minScore) return@mapNotNull null

            val topMatchingGenre = filteredGenres
                .maxByOrNull { profile.getWeight(it) * (genreAdjustments[it.lowercase()] ?: 1f) }
                ?: filteredGenres.firstOrNull() ?: ""

            ScoredAlbum(
                albumId = album.albumId,
                name = album.name,
                artist = album.artist,
                artistId = album.artistId,
                coverArtId = album.coverArtId,
                genres = filteredGenres,
                year = album.year,
                score = finalScore,
                reason = "Because you listen to $topMatchingGenre"
            )
        }.sortedByDescending { it.score }

        Log.d(TAG, "Scored ${scored.size} albums, top: ${scored.firstOrNull()?.name} (${scored.firstOrNull()?.score})")

        return scored
    }

    /**
     * Apply diversity filters to scored albums.
     */
    fun applyFilters(
        scoredAlbums: List<ScoredAlbum>,
        config: RecommendationConfig = RecommendationConfig()
    ): List<ScoredAlbum> {
        val result = mutableListOf<ScoredAlbum>()
        val artistCount = mutableMapOf<String, Int>()
        val genreRepresented = mutableSetOf<String>()

        for (album in scoredAlbums) {
            val currentArtistCount = artistCount[album.artistId] ?: 0
            if (currentArtistCount >= config.maxPerArtist) continue

            result.add(album)
            artistCount[album.artistId] = currentArtistCount + 1
            genreRepresented.addAll(album.genres)

            if (result.size >= config.maxRecommendations) break
        }

        Log.d(TAG, "Filtered to ${result.size} recommendations, ${artistCount.size} unique artists")

        return result
    }

    /**
     * Full recommendation pipeline.
     */
    fun getRecommendations(
        frequentAlbums: List<AlbumGenreData>,
        recentAlbums: List<AlbumGenreData>,
        allAlbums: List<AlbumGenreData>,
        playedAlbumIds: Set<String>,
        config: RecommendationConfig = RecommendationConfig(),
        genreAdjustments: Map<String, Float> = emptyMap(),
        excludedAlbumIds: Set<String> = emptySet(),
        artistAdjustments: Map<String, Float> = emptyMap()
    ): List<ScoredAlbum> {
        val profile = buildGenreProfile(frequentAlbums, recentAlbums, config)
        val scored = scoreAlbums(allAlbums, profile, playedAlbumIds, config, genreAdjustments, excludedAlbumIds, artistAdjustments)
        return applyFilters(scored, config)
    }

    /**
     * Get "My Mix" shuffled queue from recommendations.
     */
    fun getMyMix(
        recommendations: List<ScoredAlbum>,
        maxAlbums: Int = 60
    ): List<ScoredAlbum> {
        return recommendations.take(maxAlbums).shuffled()
    }

    // ===== JSON PARSING HELPERS =====

    fun parseAlbumGenres(albumJson: JsonObject): List<String> {
        val genresArray = albumJson["genres"]?.jsonArray ?: return emptyList()
        return genresArray.mapNotNull { genreObj ->
            genreObj.jsonObject["name"]?.jsonPrimitive?.contentOrNull
        }
    }

    fun parseAlbumFromJson(albumJson: JsonObject): AlbumGenreData? {
        return try {
            AlbumGenreData(
                albumId = albumJson["id"]?.jsonPrimitive?.contentOrNull ?: return null,
                name = albumJson["name"]?.jsonPrimitive?.contentOrNull ?: "",
                artist = albumJson["artist"]?.jsonPrimitive?.contentOrNull ?: "",
                artistId = albumJson["artistId"]?.jsonPrimitive?.contentOrNull ?: "",
                coverArtId = albumJson["coverArt"]?.jsonPrimitive?.contentOrNull ?: "",
                year = albumJson["year"]?.jsonPrimitive?.intOrNull ?: 0,
                genres = parseAlbumGenres(albumJson),
                playCount = albumJson["playCount"]?.jsonPrimitive?.intOrNull ?: 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse album: ${e.message}")
            null
        }
    }
}
