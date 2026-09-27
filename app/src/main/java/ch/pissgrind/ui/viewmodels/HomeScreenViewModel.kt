package ch.pissgrind.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import ch.pissgrind.data.recommendation.CoverArtHelper.getCoverArtUrl
import ch.pissgrind.data.recommendation.GenreProfile
import ch.pissgrind.data.recommendation.RecommendationRepository
import ch.pissgrind.data.recommendation.ScoredAlbum
import ch.pissgrind.data.recommendation.TrainingModeManager
import ch.pissgrind.data.repository.AlbumRepository
import ch.pissgrind.data.repository.GenreRepository
import ch.pissgrind.data.repository.SongRepository
import ch.pissgrind.managers.NavidromeManager
import ch.pissgrind.managers.settings.ExcludedAlbumsManager
import ch.pissgrind.managers.settings.ShuffleSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ch.pissgrind.data.recommendation.TrainingDataEvent
import kotlinx.coroutines.flow.drop


@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val albumRepository: AlbumRepository,
    private val songRepository: SongRepository,
    private val recommendationRepository: RecommendationRepository,
    private val genreRepository: GenreRepository,
    private val shuffleSettingsManager: ShuffleSettingsManager,
    private val excludedAlbumsManager: ExcludedAlbumsManager
) : ViewModel() {

    private val _recentlyPlayedAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val recentlyPlayedAlbums: StateFlow<List<MediaItem>> = _recentlyPlayedAlbums.asStateFlow()

    private val _recentAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val recentAlbums: StateFlow<List<MediaItem>> = _recentAlbums.asStateFlow()

    private val _mostPlayedAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val mostPlayedAlbums: StateFlow<List<MediaItem>> = _mostPlayedAlbums.asStateFlow()

    private val _shuffledAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val shuffledAlbums: StateFlow<List<MediaItem>> = _shuffledAlbums.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _recommendations = MutableStateFlow<List<ScoredAlbum>>(emptyList())
    val recommendations: StateFlow<List<ScoredAlbum>> = _recommendations.asStateFlow()

    private val _genreProfile = MutableStateFlow<GenreProfile?>(null)
    val genreProfile: StateFlow<GenreProfile?> = _genreProfile.asStateFlow()

    private val _topGenre = MutableStateFlow("")
    val topGenre: StateFlow<String> = _topGenre.asStateFlow()

    private val _discoverGenre = MutableStateFlow("")
    val discoverGenre: StateFlow<String> = _discoverGenre.asStateFlow()

    private val _discoverAlbums = MutableStateFlow<List<ScoredAlbum>>(emptyList())
    val discoverAlbums: StateFlow<List<ScoredAlbum>> = _discoverAlbums.asStateFlow()

    private val _myMix = MutableStateFlow<List<ScoredAlbum>>(emptyList())
    val myMix: StateFlow<List<ScoredAlbum>> = _myMix.asStateFlow()

    private val _isRecommendationsLoading = MutableStateFlow(false)
    val isRecommendationsLoading: StateFlow<Boolean> = _isRecommendationsLoading.asStateFlow()

    private val _genreMixCovers = MutableStateFlow<List<String>>(emptyList())
    val genreMixCovers: StateFlow<List<String>> = _genreMixCovers.asStateFlow()
    private val _myMixCovers = MutableStateFlow<List<String>>(emptyList())
    val myMixCovers: StateFlow<List<String>> = _myMixCovers.asStateFlow()
    private val _excludedAlbumIds = MutableStateFlow<Set<String>>(emptySet())
    val excludedAlbumIds: StateFlow<Set<String>> = _excludedAlbumIds.asStateFlow()
    private var lastRecommendationRefresh: Long = 0L
    private var trainingDataChangedSinceLastRefresh = false

    fun markTrainingDataChanged() {
        trainingDataChangedSinceLastRefresh = true
    }

    fun refreshRecommendationsIfNeeded() {
        val now = System.currentTimeMillis()
        val oneHour = 60 * 60 * 1000L

        if (now - lastRecommendationRefresh > oneHour && trainingDataChangedSinceLastRefresh) {
            viewModelScope.launch {
                loadRecommendations()
                lastRecommendationRefresh = now
                trainingDataChangedSinceLastRefresh = false
            }
        }
    }

    private fun loadLastRefreshTime() {
        val prefs = appContext.getSharedPreferences("pissgrind_recommendations", Context.MODE_PRIVATE)
        lastRecommendationRefresh = prefs.getLong("last_refresh", 0L)
        trainingDataChangedSinceLastRefresh = prefs.getBoolean("training_changed", false)
    }

    private fun saveRefreshState() {
        val prefs = appContext.getSharedPreferences("pissgrind_recommendations", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("last_refresh", lastRecommendationRefresh)
            .putBoolean("training_changed", trainingDataChangedSinceLastRefresh)
            .apply()
    }

    init {
        loadExcludedAlbums()
        loadHomeScreenData()
        loadRecommendations()
        preloadLibraryThenCovers()
        loadLastRefreshTime()
        refreshRecommendationsIfNeeded()
        observeServerChanges()
        viewModelScope.launch {
            TrainingDataEvent.changed.collect {
                trainingDataChangedSinceLastRefresh = true
                saveRefreshState()
            }
        }
    }
    private fun observeServerChanges() {
        viewModelScope.launch {
            NavidromeManager.allServers
                .drop(1)
                .collect { servers ->
                    if (servers.isNotEmpty()) {
                        loadHomeScreenData()
                        loadRecommendations(forceRefresh = true)
                        preloadLibraryThenCovers()
                    }
                }
        }
    }

    private fun loadExcludedAlbums() {
        viewModelScope.launch {
            excludedAlbumsManager.excludedAlbumIds.collect { ids ->
                _excludedAlbumIds.value = ids
            }
        }
    }

    fun excludeAlbum(albumId: String) {
        viewModelScope.launch {
            excludedAlbumsManager.excludeAlbum(albumId)
            val recs = recommendationRepository.recomputeFromCache()
            _recommendations.value = recs
            _myMix.value = recs.take(20).shuffled()
        }
    }

    fun unexcludeAlbum(albumId: String) {
        viewModelScope.launch {
            excludedAlbumsManager.restoreAlbum(albumId)
            val recs = recommendationRepository.recomputeFromCache()
            _recommendations.value = recs
            _myMix.value = recs.take(20).shuffled()
        }
    }

    private fun preloadLibraryThenCovers() {
        viewModelScope.launch {
            recommendationRepository.getAllAlbumsWithGenres()
            loadGenreMixCovers()
        }
    }

    private val _selectedShuffleGenre = MutableStateFlow<String?>(null)
    val selectedShuffleGenre: StateFlow<String?> = _selectedShuffleGenre.asStateFlow()
    fun setShuffleGenre(genre: String) {
        _selectedShuffleGenre.value = genre
        loadGenreMixCovers(genre)
    }
    private fun loadGenreMixCovers(genre: String? = null) {
        viewModelScope.launch {
            val allAlbums = recommendationRepository.getAllAlbumsCached()
            val targetGenre = genre ?: _topGenre.value
            if (targetGenre.isBlank() || allAlbums.isEmpty()) return@launch

            val matchingAlbums = allAlbums
                .filter { album ->
                    genreRepository.normalizeAll(album.genres)
                        .any { it.equals(targetGenre, ignoreCase = true) }
                }
                .shuffled()
                .take(4)

            val covers = matchingAlbums.map { album ->
                ScoredAlbum(
                    albumId = album.albumId,
                    name = album.name,
                    artist = album.artist,
                    artistId = album.artistId,
                    coverArtId = album.coverArtId,
                    genres = emptyList(),
                    year = album.year,
                    score = 0f,
                    reason = ""
                ).getCoverArtUrl()
            }

            _genreMixCovers.value = covers
        }
    }

    private fun preloadLibrary() {
        viewModelScope.launch {
            recommendationRepository.getAllAlbumsWithGenres()
        }
    }

    fun loadHomeScreenData() {
        viewModelScope.launch {
            _isLoading.value = true
            coroutineScope {
                val recentlyPlayedDeferred = async { albumRepository.getAlbums("recent", 21, 0, true) }
                val recentDeferred = async { albumRepository.getAlbums("newest", 60, 0, true) }
                val mostPlayedDeferred = async { albumRepository.getAlbums("frequent", 21, 0, true) }
                val shuffledDeferred = async { albumRepository.getAlbums("random", 20, 0, true) }

                _recentlyPlayedAlbums.value = recentlyPlayedDeferred.await()
                _recentAlbums.value = recentDeferred.await()
                android.util.Log.d("HomeScreenVM", "recentlyPlayed: ${_recentlyPlayedAlbums.value.size}, recent: ${_recentAlbums.value.size}, mostPlayed: ${_mostPlayedAlbums.value.size}, shuffled: ${_shuffledAlbums.value.size}")
                _mostPlayedAlbums.value = mostPlayedDeferred.await()
                _shuffledAlbums.value = shuffledDeferred.await()
            }
            _isLoading.value = false
        }
    }

    // ===== RECOMMENDATIONS =====
    fun loadRecommendations(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isRecommendationsLoading.value = true
            try {
                val recs = recommendationRepository.getRecommendations(forceRefresh)
                _recommendations.value = recs

                val profile: GenreProfile = recommendationRepository.getGenreProfile()
                _genreProfile.value = profile
                _topGenre.value = profile.topGenre.replaceFirstChar { it.uppercase() }

                _myMix.value = recs.take(20).shuffled()

                if (forceRefresh) loadGenreMixCovers()

            } catch (e: Exception) {
                android.util.Log.e("HomeScreenVM", "Failed to load recommendations", e)
            } finally {
                _isRecommendationsLoading.value = false
            }
        }
    }

    // Training Button
    suspend fun getTrainingPlaylist(): List<MediaItem> {
        val range = shuffleSettingsManager.shuffleRangeFlow.first()

        return if (range == ShuffleSettingsManager.SHUFFLE_RANGE_ALL) {
            val randomOffset = Random.nextInt(0, 5000)
            val songs = songRepository.getSongs(songCount = 10000, songOffset = randomOffset)
            if (songs.size >= 50) songs.shuffled()
            else songRepository.getSongs(songCount = 10000, songOffset = 0).shuffled()
        } else {
            val randomOffset = if (range > 50) Random.nextInt(0, range) else 0
            val songs = songRepository.getSongs(songCount = range, songOffset = randomOffset)
            if (songs.size >= 50) songs.shuffled()
            else songRepository.getSongs(songCount = range, songOffset = 0).shuffled()
        }
    }

    private fun spreadByAlbum(songs: List<MediaItem>): List<MediaItem> {
        if (songs.size <= 1) return songs

        val result = mutableListOf<MediaItem>()
        val remaining = songs.toMutableList()

        var lastAlbum = ""

        while (remaining.isNotEmpty()) {
            // Find a Song not from the same album as last
            val nextIndex = remaining.indexOfFirst {
                (it.mediaMetadata.albumTitle?.toString() ?: "") != lastAlbum
            }

            if (nextIndex != -1) {
                val song = remaining.removeAt(nextIndex)
                lastAlbum = song.mediaMetadata.albumTitle?.toString() ?: ""
                result.add(song)
            } else {
                // No choice -> add the rest (edge case: many songs from same album)
                result.addAll(remaining)
                break
            }
        }

        return result
    }

    fun getMyMixAlbumIds(): List<String> {
        return _myMix.value.map { it.albumId }
    }

    fun getGenreMixAlbumIds(): List<String> {
        return _recommendations.value
            .filter { album ->
                album.genres.any { it.equals(_topGenre.value, ignoreCase = true) }
            }
            .shuffled()
            .take(20)
            .map { it.albumId }
    }

    fun startTrainingMode() {
        TrainingModeManager.startTrainingPlaylist()
    }

    fun submitTrainingFeedback(albumId: String, genres: List<String>, liked: Boolean) {
        viewModelScope.launch {
            recommendationRepository.submitFeedback(albumId, genres, liked)
        }
    }

    // ===== EXISTING FUNCTIONS =====

    fun reshuffleQuickPicks() {
        viewModelScope.launch {
            _shuffledAlbums.value = albumRepository.getAlbums("random", 20, 0, true)
        }
    }

    suspend fun getAlbumSongs(albumId: String): List<MediaItem> {
        return albumRepository.getAlbum(albumId) ?: emptyList()
    }

    suspend fun getAllSongsShuffled(): List<MediaItem> {
        val range = shuffleSettingsManager.shuffleRangeFlow.first()
        val songCount = if (range == ShuffleSettingsManager.SHUFFLE_RANGE_ALL) 10000 else range
        val randomOffset = if (songCount > 50) Random.nextInt(0, songCount) else 0

        val songs = songRepository.getSongs(songCount = songCount, songOffset = randomOffset)
        return if (songs.size >= 50) songs.shuffled().take(100)
        else songRepository.getSongs(songCount = songCount, songOffset = 0).shuffled().take(100)
    }

    fun getAvailableGenres(): List<String> {
        val allAlbums = recommendationRepository.getAllAlbumsCached()
        return allAlbums
            .flatMap { genreRepository.normalizeAll(it.genres) }
            .distinct()
            .sorted()
    }
    suspend fun getSongsForGenre(genre: String): List<MediaItem> {
        val allAlbums = recommendationRepository.getAllAlbumsCached()
        val matchingAlbumIds = allAlbums
            .filter { album ->
                genreRepository.normalizeAll(album.genres).any { it.equals(genre, ignoreCase = true) }
            }
            .shuffled()
            .take(8)
            .map { it.albumId }

        val songs = mutableListOf<MediaItem>()
        matchingAlbumIds.forEach { id ->
            val albumSongs = getAlbumSongs(id)
            songs.addAll(albumSongs)
        }
        return songs
    }
}
