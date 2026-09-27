package ch.pissgrind.ui.elements

import androidx.compose.runtime.collectAsState
import android.widget.Toast
import ch.pissgrind.data.local.entity.CollectionItem
import ch.pissgrind.data.local.entity.CollectionItemType
import ch.pissgrind.ui.screens.CollectionViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import ch.pissgrind.R
import ch.pissgrind.formatMilliseconds
import ch.pissgrind.ui.elements.dialogs.showAddSongToPlaylistDialog
import ch.pissgrind.ui.elements.dialogs.songToAddToPlaylist
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.LaunchedEffect
import ch.pissgrind.ui.theme.PissGreen

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HorizontalSongCard(
    song: MediaItem,
    modifier: Modifier = Modifier,
    showTrackNumber: Boolean = false,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToFavourites: () -> Unit = {},
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onLongClick: () -> Unit = {},
) {

    val context = LocalContext.current
    val collectionViewModel: CollectionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val navidromeId = song.mediaMetadata.extras?.getString("navidromeID") ?: song.mediaId
    val isInCollection by collectionViewModel.isInCollection(navidromeId)
        .collectAsState(initial = false)

    var expanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val swipeColor = MaterialTheme.colorScheme.primary

    var hasTriggeredSwipe by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showQueueFeedback by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (!hasTriggeredSwipe) {
                        hasTriggeredSwipe = true
                        showQueueFeedback = true
                        onAddToQueue()
                        Toast.makeText(context, "${song.mediaMetadata.title} added to Queue", Toast.LENGTH_SHORT).show()
                    }
                    false
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    if (onRemoveFromPlaylist != null) {
                        showDeleteConfirm = true
                    }
                    false
                }

                else -> false
            }
        },
        positionalThreshold = { it * 0.5f }
    )

    LaunchedEffect(showQueueFeedback) {
        if (showQueueFeedback) {
            kotlinx.coroutines.delay(300)
            showQueueFeedback = false
        }
    }

    Box(modifier = modifier) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromEndToStart = onRemoveFromPlaylist != null,
            enableDismissFromStartToEnd = true,
            backgroundContent = {
                val isQueueSwipe = dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd || showQueueFeedback
                val isRemoveSwipe = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            when {
                                isQueueSwipe -> swipeColor.copy(alpha = 0.15f)
                                isRemoveSwipe -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                else -> Color.Transparent
                            }
                        )
                        .padding(horizontal = 20.dp),
                    contentAlignment = if (isRemoveSwipe) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    if (isQueueSwipe) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.list_plus_light),
                                contentDescription = null,
                                tint = swipeColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add to Queue", color = swipeColor, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (isRemoveSwipe) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.trash_light),
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                modifier = Modifier.combinedClickable(
                    onClick = onClick,
                    onLongClick = { expanded = true }
                )
            ) {
                Row(
                    modifier = Modifier.height(72.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showTrackNumber) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .padding(8.dp, 0.dp, 0.dp, 0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = song.mediaMetadata.trackNumber.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(song.mediaMetadata.artworkUri)
                                .crossfade(true)
                                .size(64)
                                .diskCacheKey(
                                    song.mediaMetadata.extras?.getString("navidromeID")
                                        ?: song.mediaId
                                )
                                .build(),
                            contentDescription = "Album Image",
                            contentScale = ContentScale.FillHeight,
                            modifier = Modifier
                                .size(64.dp)
                                .padding(4.dp, 0.dp, 0.dp, 0.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        Modifier
                            .padding(end = 12.dp)
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = song.mediaMetadata.title.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = song.mediaMetadata.artist.toString() + if (song.mediaMetadata.recordingYear != 0) " • " + song.mediaMetadata.recordingYear else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.9f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                    }

                    val formattedDuration by remember(song.mediaMetadata.durationMs) {
                        derivedStateOf {
                            formatMilliseconds(
                                (song.mediaMetadata.durationMs?.div(1000))?.toInt() ?: 0
                            )
                        }
                    }
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(end = 12.dp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        // Play Next
                        DropdownMenuItem(
                            text = { Text("Play Next") },
                            onClick = {
                                onPlayNext()
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.list_plus_light),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )

                        // Add to Playlist
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.Dialog_Add_To_Playlist).replace(
                                        "/ ",
                                        ""
                                    )
                                )
                            },
                            onClick = {
                                showAddSongToPlaylistDialog.value = true
                                songToAddToPlaylist.value = song
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.plus_light),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )

                        // Add/Remove Favourites
                        val isFavourite by collectionViewModel.isSongInFavourites(navidromeId)
                            .collectAsState(initial = false)

                        DropdownMenuItem(
                            text = { Text(if (isFavourite) "Remove from Favourites" else "Add to Favourites") },
                            onClick = {
                                if (isFavourite) {
                                    collectionViewModel.removeSongFromFavourites(song)
                                    Toast.makeText(
                                        context,
                                        "${song.mediaMetadata.title} removed from Favourites",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    collectionViewModel.addSongToFavourites(song)
                                    Toast.makeText(
                                        context,
                                        "${song.mediaMetadata.title} added to Favourites",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(
                                        if (isFavourite) R.drawable.heart_straight_fill else R.drawable.heart_straight_light
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )

                        // Add/Remove from Library
                        DropdownMenuItem(
                            text = {
                                Text(if (isInCollection) "Remove from Library" else "Add to Library")
                            },
                            onClick = {
                                collectionViewModel.toggleInCollection(
                                    CollectionItem(
                                        id = navidromeId,
                                        type = CollectionItemType.SONG,
                                        title = song.mediaMetadata.title.toString(),
                                        subtitle = song.mediaMetadata.artist.toString(),
                                        coverArtUrl = song.mediaMetadata.artworkUri.toString(),
                                        streamUrl = song.mediaId.removeSuffix("&estimateContentLength=true"),
                                        duration = song.mediaMetadata.extras?.getInt("duration"),
                                        trackNumber = song.mediaMetadata.trackNumber,
                                        year = song.mediaMetadata.recordingYear,
                                        album = song.mediaMetadata.albumTitle.toString()
                                    )
                                )
                                Toast.makeText(
                                    context,
                                    if (isInCollection) "${song.mediaMetadata.title} removed from Library"
                                    else "${song.mediaMetadata.title} added to Library",
                                    Toast.LENGTH_SHORT
                                ).show()
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    painter = if (isInCollection)
                                        painterResource(R.drawable.minus_circle_fill)
                                    else
                                        painterResource(R.drawable.plus_circle_light),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        // Confirm Delete Overlay
        if (showDeleteConfirm) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.7f),
                            RoundedCornerShape(2.dp)
                        )
                        .clickable {
                            showDeleteConfirm = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.x_light),
                        contentDescription = "Cancel",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Confirm Delete Button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.error,
                            RoundedCornerShape(2.dp)
                        )
                        .clickable {
                            onRemoveFromPlaylist?.invoke()
                            showDeleteConfirm = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.trash_light),
                        contentDescription = "Confirm Remove",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}



