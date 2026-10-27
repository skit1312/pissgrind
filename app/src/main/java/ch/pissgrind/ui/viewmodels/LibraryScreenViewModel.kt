package ch.pissgrind.ui.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import ch.pissgrind.data.model.MediaData
import ch.pissgrind.data.recommendation.RecommendationRepository
import ch.pissgrind.data.recommendation.ScoredAlbum
import ch.pissgrind.data.repository.AlbumRepository
import ch.pissgrind.data.repository.ArtistRepository
import ch.pissgrind.data.repository.GenreRepository
import ch.pissgrind.data.repository.SongRepository
import ch.pissgrind.managers.NavidromeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryStats(val albums: Int, val artists: Int, val songs: Int)

@HiltViewModel
class LibraryScreenViewModel @Inject constructor(
    private val application: Application,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val songRepository: SongRepository,
    private val recommendationRepository: RecommendationRepository,
    private val genreRepository: GenreRepository
) : ViewModel() {

    // Full cached data (loaded once)
    private val _allAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _allArtists = MutableStateFlow<List<MediaData.Artist>>(emptyList())
    private val _allSongs = MutableStateFlow<List<MediaItem>>(emptyList())

    // Random discover picks (8 each)
    private val _discoverAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val discoverAlbums: StateFlow<List<MediaItem>> = _discoverAlbums.asStateFlow()

    private val _discoverArtists = MutableStateFlow<List<MediaData.Artist>>(emptyList())
    val discoverArtists: StateFlow<List<MediaData.Artist>> = _discoverArtists.asStateFlow()

    private val _discoverSongs = MutableStateFlow<List<MediaItem>>(emptyList())
    val discoverSongs: StateFlow<List<MediaItem>> = _discoverSongs.asStateFlow()

    // Search results
    private val _filteredAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val filteredAlbums: StateFlow<List<MediaItem>> = _filteredAlbums.asStateFlow()

    private val _filteredArtists = MutableStateFlow<List<MediaData.Artist>>(emptyList())
    val filteredArtists: StateFlow<List<MediaData.Artist>> = _filteredArtists.asStateFlow()

    private val _filteredSongs = MutableStateFlow<List<MediaItem>>(emptyList())
    val filteredSongs: StateFlow<List<MediaItem>> = _filteredSongs.asStateFlow()

    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _artistCoverMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val artistCoverMap: StateFlow<Map<String, String>> = _artistCoverMap.asStateFlow()

    private val _libraryStats = MutableStateFlow<LibraryStats?>(null)
    val libraryStats: StateFlow<LibraryStats?> = _libraryStats.asStateFlow()
    private val _availableCategories = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val availableCategories: StateFlow<List<Pair<String, Int>>> = _availableCategories.asStateFlow()

    // Track if Data was already loaded
    private var dataLoaded = false

    init {
        loadAllIfNeeded()
        observeServerChanges()
    }

    private fun observeServerChanges() {
        viewModelScope.launch {
            NavidromeManager.allServers
                .drop(1)
                .collect { servers ->
                    if (servers.isNotEmpty()) {
                        refresh()
                    }
                }
        }
    }

    /**
     * Only loads data once. Subsequent calls are no-ops.
     */
    private fun loadAllIfNeeded() {
        if (dataLoaded) return
        viewModelScope.launch {
            _isLoading.value = true
            loadData()
            _isLoading.value = false
            dataLoaded = true
        }
    }

    /**
     * Pull-to-refresh: Force reload + reshuffle.
     */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadData(ignoreCached = true)
            _isRefreshing.value = false
        }
    }

    /**
     * Reshuffle discover picks without reloading from server.
     */

    private suspend fun loadData(ignoreCached: Boolean = false) {
        val albumsDeferred = viewModelScope.async {
            val allAlbums = mutableListOf<MediaItem>()
            var offset = 0
            val pageSize = 500
            while (true) {
                val page = albumRepository.getAlbums(
                    size = pageSize,
                    offset = offset,
                    ignoreCachedResponse = ignoreCached
                )
                allAlbums.addAll(page)
                if (page.size < pageSize) break
                offset += pageSize
            }
            allAlbums.toList()
        }

        val songsDeferred = viewModelScope.async {
            val allSongs = mutableListOf<MediaItem>()
            var offset = 0
            val pageSize = 500
            while (true) {
                val page = songRepository.getSongs(
                    songCount = pageSize,
                    songOffset = offset,
                    ignoreCachedResponse = ignoreCached
                )
                allSongs.addAll(page)
                if (page.size < pageSize) break
                offset += pageSize
            }
            allSongs.toList()
        }

        val artistsDeferred = viewModelScope.async {
            artistRepository.getArtists(size = 2000)
        }

        val albums = albumsDeferred.await()
        val artists = artistsDeferred.await()
        val songs = songsDeferred.await()

        _allAlbums.value = albums
        _allArtists.value = artists
        _allSongs.value = songs

        // Build artist cover map
        val coverMap = mutableMapOf<String, String>()
        albums.forEach { album ->
            val artistName = album.mediaMetadata.artist?.toString() ?: return@forEach
            if (!coverMap.containsKey(artistName)) {
                album.mediaMetadata.artworkUri?.toString()?.let { uri ->
                    coverMap[artistName] = uri
                }
            }
        }
        _artistCoverMap.value = coverMap

        // Set discover picks (random 8)
        _discoverAlbums.value = albums.shuffled().take(6)
        _discoverArtists.value = artists.shuffled().take(4)
        _discoverSongs.value = songs.shuffled().take(12)
        _libraryStats.value = LibraryStats(albums.size, artists.size, songs.size)

        // Reset filtered to full lists
        _filteredAlbums.value = albums
        _filteredArtists.value = artists
        _filteredSongs.value = songs

        viewModelScope.launch { loadGenreCounts() }
    }

    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _filteredAlbums.value = _allAlbums.value
            _filteredArtists.value = _allArtists.value
            _filteredSongs.value = _allSongs.value
            return
        }

        val lowerQuery = query.lowercase()

        _filteredAlbums.value = _allAlbums.value.filter {
            val meta = it.mediaMetadata
            meta.albumTitle?.toString()?.lowercase()?.contains(lowerQuery) == true ||
                    meta.artist?.toString()?.lowercase()?.contains(lowerQuery) == true
        }

        _filteredArtists.value = _allArtists.value.filter {
            it.name.lowercase().contains(lowerQuery)
        }

        _filteredSongs.value = _allSongs.value.filter {
            val meta = it.mediaMetadata
            meta.title?.toString()?.lowercase()?.contains(lowerQuery) == true ||
                    meta.artist?.toString()?.lowercase()?.contains(lowerQuery) == true ||
                    meta.albumTitle?.toString()?.lowercase()?.contains(lowerQuery) == true
        }
    }

    private val prefs = application.getSharedPreferences("pissgrind_prefs", Context.MODE_PRIVATE)

    private val _recentSearches = MutableStateFlow<List<String>>(loadRecentSearches())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 3) return
        val current = _recentSearches.value.toMutableList()
        current.remove(trimmed) // Remove duplicate
        current.add(0, trimmed) // Add to top
        val limited = current.take(10) // Max 10
        _recentSearches.value = limited
        saveRecentSearches(limited)
    }

    fun removeRecentSearch(query: String) {
        val current = _recentSearches.value.toMutableList()
        current.remove(query)
        _recentSearches.value = current
        saveRecentSearches(current)
    }

    private fun loadRecentSearches(): List<String> {
        val json = prefs.getString("recent_searches", null) ?: return emptyList()
        return try {
            json.split("||").filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveRecentSearches(searches: List<String>) {
        prefs.edit().putString("recent_searches", searches.joinToString("||")).commit()
    }

    fun shuffleDiscover() {
        _discoverAlbums.value = _allAlbums.value.shuffled().take(6)
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        saveRecentSearches(emptyList())
    }

    private suspend fun loadGenreCounts() {
        val albums = try {
            recommendationRepository.getAllAlbumsWithGenres()
        } catch (e: Exception) {
            emptyList()
        }
        if (albums.isEmpty()) return

        val normalized = albums.map { genreRepository.normalizeAll(it.genres).toSet() }
        _availableCategories.value = genreRepository.getGenresByCategory()
            .mapNotNull { (category, genres) ->
                val count = normalized.count { albumGenres -> genres.any { it in albumGenres } }
                if (count > 0) category to count else null
            }
    }
}