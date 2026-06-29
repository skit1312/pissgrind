package ch.skit.pissgrind.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import ch.skit.pissgrind.data.repository.SongRepository
import ch.skit.pissgrind.managers.DataRefreshManager
import ch.skit.pissgrind.managers.settings.LocalDataSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongsScreenViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val localDataSettingsManager: LocalDataSettingsManager
) : ViewModel() {

    private val _allSongs = MutableStateFlow<List<MediaItem>>(emptyList())
    val allSongs: StateFlow<List<MediaItem>> = _allSongs.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    init {
        getSongs()
        viewModelScope.launch {
            localDataSettingsManager.showFavoriteOnly.collect { showFavorites ->
                _showFavoritesOnly.value = showFavorites
                getSongs()
            }
            DataRefreshManager.dataSourceChangedEvent.collect {
                getSongs()
            }
        }
    }

    fun getSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            coroutineScope {
                _allSongs.value = songRepository.getSongs(ignoreCachedResponse = true, favoritesOnly = _showFavoritesOnly.value)
            }
            _isLoading.value = false
        }
    }

    fun getMoreSongs(size: Int){
        viewModelScope.launch {
            _isLoading.value = true
            coroutineScope {
                val songOffset = _allSongs.value.size
                _allSongs.value += songRepository.getSongs(songCount = size, songOffset = songOffset)
            }
            _isLoading.value = false
        }
    }

    fun search(query: String){
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            coroutineScope {
                _searchResults.value = songRepository.searchSongs(query)
            }
            _isLoading.value = false
        }
    }
    fun setShowFavoritesOnly(showFavorites: Boolean) {
        viewModelScope.launch {
            localDataSettingsManager.saveShowFavoriteOnly(showFavorites)
        }
    }
}