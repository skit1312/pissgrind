package ch.skit.pissgrind.providers.navidrome

import androidx.media3.common.MediaItem
import ch.skit.pissgrind.data.datasource.navidrome.NavidromeDataSource
import ch.skit.pissgrind.data.model.MediaData
import ch.skit.pissgrind.data.model.playlistList
import ch.skit.pissgrind.data.model.toMediaItem
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistContainer(val playlist: List<MediaData.Playlist>? = listOf())

fun parseNavidromePlaylistsJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String
) : List<MediaItem> {
    val subsonicResponse = parseSubsonicResponse(response)

    // Generate password salt and hash
    val passwordSaltMedia = NavidromeDataSource.generateSalt(8)
    val passwordHashMedia = NavidromeDataSource.md5Hash(navidromePassword + passwordSaltMedia)

    subsonicResponse.playlists?.playlist?.map {
        it.coverArt = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.16.1&c=Chora&size=128"
    }

    subsonicResponse.playlists?.playlist?.filterNot { newPlaylist ->
        playlistList.any { existingPlaylist ->
            existingPlaylist.navidromeID == newPlaylist.navidromeID
        }
    }
    return subsonicResponse.playlists?.playlist?.map { it.toMediaItem() } ?: emptyList()
}

fun parseNavidromePlaylistJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String
) : List<MediaItem> {
    val subsonicResponse = parseSubsonicResponse(response)

    val mediaDataPlaylist = mutableListOf<MediaItem>()

    // Generate password salt and hash
    val passwordSalt = NavidromeDataSource.generateSalt(8)
    val passwordHash = NavidromeDataSource.md5Hash(navidromePassword + passwordSalt)

    subsonicResponse.playlist?.songs?.map {
        it.imageUrl = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora&size=128"
        it.media = "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.12.0&c=Chora"
    }

    mediaDataPlaylist.addAll(subsonicResponse.playlist?.songs?.map { it.toMediaItem() } ?: emptyList())

    return mediaDataPlaylist
}