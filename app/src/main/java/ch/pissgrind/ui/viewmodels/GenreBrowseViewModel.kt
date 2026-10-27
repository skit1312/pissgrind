package ch.pissgrind.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.pissgrind.data.recommendation.AlbumGenreData
import ch.pissgrind.data.recommendation.RecommendationRepository
import ch.pissgrind.data.recommendation.CoverArtHelper.getCoverArtUrl
import ch.pissgrind.data.recommendation.ScoredAlbum
import ch.pissgrind.data.repository.GenreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GenreBrowseViewModel @Inject constructor(
    private val genreRepository: GenreRepository,
    private val recommendationRepository: RecommendationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val initialCategory: String? = savedStateHandle.get<String>("category")
    private val _genresByCategory = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val genresByCategory: StateFlow<Map<String, List<String>>> = _genresByCategory.asStateFlow()

    private val _selectedCategory = MutableStateFlow(initialCategory ?: "Grind")

    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    private val _albumsForGenre = MutableStateFlow<List<ScoredAlbum>>(emptyList())
    val albumsForGenre: StateFlow<List<ScoredAlbum>> = _albumsForGenre.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var allAlbums: List<AlbumGenreData> = emptyList()
    private var availableGenres: Set<String> = emptySet()

    init {
        loadAllAlbums()
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _selectedGenre.value = null
        _albumsForGenre.value = emptyList()
    }

    fun selectGenre(genre: String) {
        _selectedGenre.value = genre
        filterAlbumsByGenre(genre)
    }

    private fun loadAllAlbums() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                allAlbums = recommendationRepository.getAllAlbumsWithGenres()

                // Normalize all genres and collect which ones actually exist
                availableGenres = allAlbums.flatMap { album ->
                    genreRepository.normalizeAll(album.genres)
                }.toSet()

                // Filter genresByCategory to only show genres with albums
                val fullCategories = genreRepository.getGenresByCategory()
                val unknownRaw = allAlbums
                    .flatMap { it.genres }
                    .filter { genreRepository.normalize(it) == null }
                    .groupingBy { it.trim().lowercase() }
                    .eachCount()
                    .entries.sortedByDescending { it.value }
                android.util.Log.d(
                    "GenreBrowse",
                    "Unknown Tags (${unknownRaw.size}): " +
                            unknownRaw.joinToString { "${it.key} (${it.value})" }
                )
                val filteredCategories = fullCategories.mapValues { (_, genres) ->
                    genres.filter { it in availableGenres }
                }.filter { it.value.isNotEmpty() }

                _genresByCategory.value = filteredCategories

                // Set default category to first non-empty
                if (filteredCategories.isNotEmpty() && _selectedCategory.value !in filteredCategories) {
                    _selectedCategory.value = filteredCategories.keys.first()
                }
            } catch (e: Exception) {
                allAlbums = emptyList()
            }
            _isLoading.value = false
        }
    }

    private fun filterAlbumsByGenre(genre: String) {
        _isLoading.value = true

        val filtered = allAlbums.filter { album ->
            val normalizedGenres = genreRepository.normalizeAll(album.genres)
            normalizedGenres.any { it.equals(genre, ignoreCase = true) }
        }.map { albumData ->
            // Convert AlbumGenreData to ScoredAlbum for display
            ScoredAlbum(
                albumId = albumData.albumId,
                name = albumData.name,
                artist = albumData.artist,
                artistId = albumData.artistId,
                coverArtId = albumData.coverArtId,
                genres = genreRepository.normalizeAll(albumData.genres),
                year = albumData.year,
                score = 0f,
                reason = genre
            )
        }.sortedBy { it.artist.lowercase() }

        _albumsForGenre.value = filtered
        _isLoading.value = false
    }
}
