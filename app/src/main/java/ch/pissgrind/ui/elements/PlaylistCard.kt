package ch.pissgrind.ui.elements

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import ch.pissgrind.ui.elements.dialogs.playlistToDelete
import ch.pissgrind.ui.elements.dialogs.showDeletePlaylistDialog


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistCard(playlist: MediaItem, onClick: () -> Unit) {
    val metadata = playlist.mediaMetadata
    val context = LocalContext.current
    val playlistId = metadata.extras?.getString("navidromeID") ?: playlist.mediaId

    // Custom Cover from SharedPrefs
    val coverData = rememberPlaylistCoverUri(
        playlistId = playlistId,
        fallbackUri = if (playlistId.startsWith("Local"))
            null
        else
            metadata.artworkUri?.toString()
    ) ?: if (playlistId.startsWith("Local")) metadata.artworkData else metadata.artworkUri

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = {
                    playlistToDelete.value =
                        metadata.extras?.getString("navidromeID") ?: ""
                    showDeletePlaylistDialog.value = true
                },
                onLongClickLabel = "Delete Playlist"
            )
            .widthIn(min = 128.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(coverData)
                .crossfade(true)
                .diskCacheKey(playlistId)
                .build(),
            contentScale = ContentScale.Crop,
            contentDescription = "Playlist Cover",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))
                .clip(RoundedCornerShape(2.dp))
        )

        Text(
            text = metadata.title.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp)
                .wrapContentHeight(align = Alignment.CenterVertically),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
