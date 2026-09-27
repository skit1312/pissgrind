package ch.pissgrind.data.repository

import androidx.media3.common.MediaItem
import ch.pissgrind.data.datasource.local.LocalDataSource
import ch.pissgrind.data.datasource.navidrome.NavidromeDataSource
import ch.pissgrind.data.model.Playlist
import ch.pissgrind.managers.NavidromeManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val navidromeDataSource: NavidromeDataSource
) {

    suspend fun getPlaylists(ignoreCachedResponse: Boolean = false): List<MediaItem> = coroutineScope {
        val deferredPlaylists = mutableListOf<Deferred<List<MediaItem>>>()

        if (NavidromeManager.checkActiveServers())
            deferredPlaylists.add(async { navidromeDataSource.getNavidromePlaylists(ignoreCachedResponse) })

        //if (LocalProviderManager.checkActiveFolders())
        deferredPlaylists.add(async { localDataSource.getLocalPlaylists() })

        deferredPlaylists.awaitAll().flatten()
    }
    suspend fun renamePlaylist(playlistId: String, newName: String) {
        if (playlistId.startsWith("Local_")) {
            localDataSource.renameLocalPlaylist(playlistId, newName)
        } else {
            navidromeDataSource.renameNavidromePlaylist(playlistId, newName, true)
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songIndex: Int, songId: String) {
        if (playlistId.startsWith("Local_")) {
            // Local using songId (String)
            localDataSource.removeSongFromLocalPlaylist(playlistId, songId)
        } else {
            // Navidrome/Subsonic using Index (Int)
            navidromeDataSource.removeSongFromNavidromePlaylist(playlistId, songIndex, true)
        }
    }
    suspend fun getPlaylistSongs(playlistId: String, ignoreCachedResponse: Boolean = false): List<MediaItem> = coroutineScope {
        if (playlistId.startsWith("Local_")){
            localDataSource.getLocalPlaylistSongs(playlistId)
        } else {
            navidromeDataSource.getNavidromePlaylist(playlistId, ignoreCachedResponse) ?: emptyList()
        }
    }

    suspend fun createPlaylist(name: String, songsToAdd: String) {
        if (NavidromeManager.checkActiveServers()) {
            navidromeDataSource.createNavidromePlaylist(name, listOf(songsToAdd), true)
        }
    }

    suspend fun deletePlaylist(playlistId: String) {
        if (playlistId.startsWith("Local_")){
            localDataSource.deleteLocalPlaylist(playlistId)
        } else {
            navidromeDataSource.deleteNavidromePlaylist(playlistId, true)
        }
    }

    suspend fun createPlaylist(name: String): Playlist? {
        val success = navidromeDataSource.createNavidromePlaylist(name)
        if (success) {
            val playlists = navidromeDataSource.getNavidromePlaylists(ignoreCachedResponse = true)
            val mediaItem = playlists.find {
                it.mediaMetadata.title?.toString().equals(name, ignoreCase = true)
            }
            return mediaItem?.let {
                Playlist(
                    name = it.mediaMetadata.title.toString(),
                    coverArt = it.mediaMetadata.artworkUri ?: android.net.Uri.EMPTY,
                    navidromeID = it.mediaMetadata.extras?.getString("navidromeID") ?: it.mediaId
                )
            }
        }
        return null
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: String): Boolean {
        return navidromeDataSource.addSongToNavidromePlaylist(playlistId, songId)
    }

    suspend fun addAlbumToPlaylist(playlistId: String, songIds: List<String>) {
        songIds.forEach { songId ->
            navidromeDataSource.addSongToNavidromePlaylist(playlistId, songId, true)
        }
    }
}
