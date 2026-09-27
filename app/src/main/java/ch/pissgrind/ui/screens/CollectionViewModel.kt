package ch.pissgrind.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import ch.pissgrind.data.local.entity.CollectionItem
import ch.pissgrind.data.local.entity.CollectionItemType
import ch.pissgrind.data.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import ch.pissgrind.data.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val application: Application,
    private val repository: CollectionRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val prefs = application.getSharedPreferences("pissgrind_prefs", 0)

    private val _selectedFilter = MutableStateFlow<CollectionItemType?>(
        prefs.getString("collection_filter", null)?.let {
            try {
                CollectionItemType.valueOf(it)
            } catch (_: Exception) {
                null
            }
        }
    )
    val selectedFilter: StateFlow<CollectionItemType?> = _selectedFilter

    val items: StateFlow<List<CollectionItem>> = _selectedFilter
        .flatMapLatest { filter ->
            if (filter == null) repository.getAllItems()
            else repository.getItemsByType(filter)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setFilter(type: CollectionItemType?) {
        _selectedFilter.value = type
        prefs.edit().apply {
            if (type == null) remove("collection_filter")
            else putString("collection_filter", type.name)
        }.apply()
    }
/*
    fun addToCollection(item: CollectionItem) {
        viewModelScope.launch { repository.addItem(item) }
    }
*/
    fun removeFromCollection(id: String) {
        viewModelScope.launch { repository.removeItem(id) }
    }

    fun toggleInCollection(item: CollectionItem) {
        viewModelScope.launch { repository.toggleItem(item) }
    }

    fun isInCollection(id: String): Flow<Boolean> = repository.isInCollection(id)

    fun syncPlaylists() {
        viewModelScope.launch {
            repository.removeItemsByType(CollectionItemType.PLAYLIST)

            val playlists = playlistRepository.getPlaylists(ignoreCachedResponse = true)
            playlists.forEach { mediaItem ->
                val metadata = mediaItem.mediaMetadata
                val navidromeID = metadata.extras?.getString("navidromeID") ?: mediaItem.mediaId

                val trackCount = metadata.totalTrackCount ?: 0

                val item = CollectionItem(
                    id = navidromeID,
                    type = CollectionItemType.PLAYLIST,
                    title = metadata.title?.toString() ?: "Unknown Playlist",
                    subtitle = "$trackCount tracks",
                    coverArtUrl = metadata.artworkUri?.toString()
                )
                repository.addItem(item)
            }
        }
    }
    private val FAVOURITES_PLAYLIST_NAME = "Favourites"

    fun addAlbumToPlaylist(playlistId: String, songs: List<MediaItem>) {
        viewModelScope.launch {
            val songIds = songs.mapNotNull { it.mediaMetadata.extras?.getString("navidromeID") }
            playlistRepository.addAlbumToPlaylist(playlistId, songIds)
        }
    }
    suspend fun getPlaylistsForDialog(): List<MediaItem> {
        return playlistRepository.getPlaylists(false)
    }


    // ===== FAVOURITES CACHE =====
    private val _favouritesSongIds = MutableStateFlow<Set<String>>(emptySet())
    val favouritesSongIds: StateFlow<Set<String>> = _favouritesSongIds.asStateFlow()

    private var favouritesPlaylistId: String? = null

    init {
        loadFavouritesCache()
    }

    private fun loadFavouritesCache() {
        viewModelScope.launch {
            try {
                val playlists = playlistRepository.getPlaylists(false)
                val favourites = playlists.find {
                    it.mediaMetadata.title?.toString().equals("Favourites", ignoreCase = true)
                }
                if (favourites != null) {
                    favouritesPlaylistId = favourites.mediaMetadata.extras?.getString("navidromeID")
                        ?: favourites.mediaId
                    val songs = playlistRepository.getPlaylistSongs(favouritesPlaylistId!!)
                    _favouritesSongIds.value = songs.mapNotNull {
                        it.mediaMetadata.extras?.getString("navidromeID")
                    }.toSet()
                }
            } catch (e: Exception) {
                Log.e("CollectionVM", "Failed to load favourites cache", e)
            }
        }
    }

    fun refreshFavouritesCache() {
        loadFavouritesCache()
    }

    fun isSongInFavourites(navidromeId: String): Flow<Boolean> {
        return _favouritesSongIds.map { ids -> navidromeId in ids }
    }

    fun addSongToFavourites(song: MediaItem) {
        val navidromeId = song.mediaMetadata.extras?.getString("navidromeID") ?: return

        _favouritesSongIds.value = _favouritesSongIds.value + navidromeId

        viewModelScope.launch {
            try {
                val playlistId = getOrCreateFavouritesPlaylistId() ?: return@launch
                playlistRepository.addSongToPlaylist(playlistId, navidromeId)
            } catch (e: Exception) {
                _favouritesSongIds.value = _favouritesSongIds.value - navidromeId
                Log.e("CollectionVM", "Failed to add to favourites", e)
            }
        }
    }

    fun removeSongFromFavourites(song: MediaItem) {
        val navidromeId = song.mediaMetadata.extras?.getString("navidromeID") ?: return

        _favouritesSongIds.value = _favouritesSongIds.value - navidromeId

        viewModelScope.launch {
            try {
                val playlistId = favouritesPlaylistId ?: return@launch
                val songs = playlistRepository.getPlaylistSongs(playlistId)
                val songIndex = songs.indexOfFirst {
                    it.mediaMetadata.extras?.getString("navidromeID") == navidromeId
                }
                if (songIndex >= 0) {
                    playlistRepository.removeSongFromPlaylist(playlistId, songIndex, navidromeId)
                }
            } catch (e: Exception) {
                _favouritesSongIds.value = _favouritesSongIds.value + navidromeId
                Log.e("CollectionVM", "Failed to remove from favourites", e)
            }
        }
    }

    private suspend fun getOrCreateFavouritesPlaylistId(): String? {
        if (favouritesPlaylistId != null) return favouritesPlaylistId

        val playlists = playlistRepository.getPlaylists(false)
        val favourites = playlists.find {
            it.mediaMetadata.title?.toString().equals("Favourites", ignoreCase = true)
        }

        return if (favourites != null) {
            favouritesPlaylistId = favourites.mediaMetadata.extras?.getString("navidromeID")
                ?: favourites.mediaId
            favouritesPlaylistId
        } else {
            val newPlaylist = playlistRepository.createPlaylist("Favourites")
            favouritesPlaylistId = newPlaylist?.navidromeID
            favouritesPlaylistId
        }
    }
}

