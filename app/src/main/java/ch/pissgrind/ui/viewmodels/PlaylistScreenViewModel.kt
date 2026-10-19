package ch.pissgrind.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import ch.pissgrind.data.repository.PlaylistRepository
import ch.pissgrind.data.repository.StarredRepository
import ch.pissgrind.managers.DataRefreshManager
import ch.pissgrind.providers.local.localPlaylistImageGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistScreenViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {
    private val _allPlaylists = MutableStateFlow<List<MediaItem>>(emptyList())
    val allPlaylists: StateFlow<List<MediaItem>> = _allPlaylists.asStateFlow()

    private var _selectedPlaylist = MutableStateFlow<MediaItem?>(null)
    val selectedPlaylist: StateFlow<MediaItem?> = _selectedPlaylist

    private var _selectedPlaylistSongs = MutableStateFlow<List<MediaItem>>(emptyList())
    val selectedPlaylistSongs: StateFlow<List<MediaItem>> = _selectedPlaylistSongs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadPlaylists()

        viewModelScope.launch {
            DataRefreshManager.dataSourceChangedEvent.collect {
                loadPlaylists()
            }
        }
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            _isLoading.value = true
            _allPlaylists.value = playlistRepository.getPlaylists(true)
            _isLoading.value = false
        }
    }

    fun setCurrentPlaylist(playlist: MediaItem){
        _selectedPlaylistSongs.value = emptyList<MediaItem>()
        _selectedPlaylist.value = playlist
        fetchPlaylistDetails()
    }

    suspend fun updatePlaylistsImages(context: Context) {
        _allPlaylists.value = _allPlaylists.value.map {
            if (it.mediaMetadata.extras?.getString("navidromeID")?.startsWith("Local_") == true && it.mediaMetadata.artworkData == null) {
                val songs = playlistRepository.getPlaylistSongs(it.mediaMetadata.extras?.getString("navidromeID") ?: "", true)
                it.buildUpon()
                    .setMediaMetadata(
                        it.mediaMetadata.buildUpon()
                            .setArtworkData(localPlaylistImageGenerator(songs, context), MediaMetadata.PICTURE_TYPE_OTHER)
                            .build()
                    )
                    .build()
            } else {
                it
            }
        }
    }

    fun fetchPlaylistDetails() {
        if (_selectedPlaylist.value == null) return

        val playlistId = _selectedPlaylist.value?.mediaMetadata?.extras?.getString("navidromeID")
        if (playlistId == null) return

        println("Fetching playlist details for playlist ID: $playlistId")

        viewModelScope.launch {
            val loadingJob = launch {
                delay(1000)
                if (_selectedPlaylistSongs.value.isEmpty()) {
                    _isLoading.value = true
                }
            }
            loadingJob.start()
            coroutineScope {
                _selectedPlaylistSongs.value = async { playlistRepository.getPlaylistSongs(playlistId) }.await()
            }
            loadingJob.cancel()
            _isLoading.value = false
        }
    }

    fun createPlaylist(name: String, songsToAdd: String, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            playlistRepository.createPlaylist(name, songsToAdd)
            DataRefreshManager.notifyDataSourcesChanged()
            _isLoading.value = false
            updatePlaylistsImages(context)
            loadPlaylists()
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            playlistRepository.addSongToPlaylist(playlistId, songId)
            _isLoading.value = false
            loadPlaylists()
        }
    }

    fun setSelectedPlaylist(playlist: MediaItem) {
        _selectedPlaylistSongs.value = emptyList()
        _selectedPlaylist.value = playlist
        fetchPlaylistDetails()
    }
    private val _playlistsChanged = MutableSharedFlow<Unit>(replay = 1)
    val playlistsChanged = _playlistsChanged.asSharedFlow()

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
            _playlistsChanged.emit(Unit)
        }
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        viewModelScope.launch {
            playlistRepository.renamePlaylist(playlistId, newName)
            _selectedPlaylist.value?.let { current ->
                val updatedMetadata = current.mediaMetadata.buildUpon()
                    .setTitle(newName)
                    .build()
                _selectedPlaylist.value = current.buildUpon()
                    .setMediaMetadata(updatedMetadata)
                    .build()
            }
            _playlistsChanged.emit(Unit)
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songIndex: Int, songId: String) {
        viewModelScope.launch {
            playlistRepository.removeSongFromPlaylist(playlistId, songIndex, songId)
            fetchPlaylistDetails()
        }
    }
    // ===== Für den Add-to-Playlist-Dialog (warten auf den Server) =====

    /** Song-IDs einer Playlist in Reihenfolge (Index 0 = oberster Eintrag). */
    suspend fun getPlaylistSongIds(playlistId: String): List<String> =
        playlistRepository.getPlaylistSongs(playlistId, true)
            .map { it.mediaMetadata.extras?.getString("navidromeID") ?: it.mediaId }

    suspend fun addSongToPlaylistAwait(playlistId: String, songId: String) {
        playlistRepository.addSongToPlaylist(playlistId, songId)
    }

    suspend fun removeSongFromPlaylistAwait(playlistId: String, songIndex: Int, songId: String) {
        playlistRepository.removeSongFromPlaylist(playlistId, songIndex, songId)
        fetchPlaylistDetails()   // falls die Playlist gerade in PlaylistDetails offen ist
    }
}
