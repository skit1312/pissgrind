package ch.pissgrind.ui.elements

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberPlaylistCoverUri(playlistId: String, fallbackUri: String?): Any? {
    val context = LocalContext.current
    val customCover = remember(playlistId) {
        if (playlistId.isNotBlank()) {
            val prefs = context.getSharedPreferences("playlist_covers", android.content.Context.MODE_PRIVATE)
            prefs.getString("cover_$playlistId", null)?.let { Uri.parse(it) }
        } else null
    }
    return customCover ?: fallbackUri
}
