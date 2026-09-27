package ch.pissgrind.data.recommendation

import ch.pissgrind.data.datasource.navidrome.NavidromeDataSource
import ch.pissgrind.managers.NavidromeManager

/**
 * Helper to generate cover art URLs for ScoredAlbums.
 */
object CoverArtHelper {

    fun ScoredAlbum.getCoverArtUrl(size: Int = 300): String {
        val server = NavidromeManager.getCurrentServer() ?: return ""
        val salt = NavidromeDataSource.generateSalt(8)
        val token = NavidromeDataSource.md5Hash(server.password + salt)
        return "${server.url.trimEnd('/')}/rest/getCoverArt.view?u=${server.username}&t=$token&s=$salt&v=1.16.1&c=Chora&id=${coverArtId}&size=$size"
    }
}
