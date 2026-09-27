package ch.pissgrind.providers.navidrome

import ch.pissgrind.data.NavidromeLibrary
import kotlinx.serialization.Serializable

@Serializable
data class MusicFolder(
    val musicFolder: List<NavidromeLibrary>
)

fun parseNavidromeLibrariesJSON(
    response: String
): List<NavidromeLibrary> {
    val subsonicResponse = parseSubsonicResponse(response)

    return subsonicResponse.musicFolders?.musicFolder ?: emptyList()
}