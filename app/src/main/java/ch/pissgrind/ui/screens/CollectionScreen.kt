package ch.pissgrind.ui.screens

import androidx.compose.ui.graphics.Color
import ch.pissgrind.data.model.MediaData
import ch.pissgrind.ui.viewmodels.ArtistsScreenViewModel
import android.os.Bundle
import ch.pissgrind.data.model.Screen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import ch.pissgrind.data.local.entity.CollectionItem
import ch.pissgrind.data.local.entity.CollectionItemType
import ch.pissgrind.player.SongHelper
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import ch.pissgrind.R
import ch.pissgrind.ui.elements.rememberPlaylistCoverUri
import ch.pissgrind.ui.theme.LocalIsDark
import ch.pissgrind.ui.theme.LocalLightBackground
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen
import ch.pissgrind.ui.theme.PissWhite
import kotlinx.coroutines.delay
import ch.pissgrind.ui.viewmodels.PlaylistScreenViewModel

@Composable
fun CollectionScreen(
    navHostController: NavHostController,
    mediaController: MediaController? = null,
    artistsViewModel: ArtistsScreenViewModel? = null,
    collectionViewModel: CollectionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val isDark = LocalIsDark.current
    val items by collectionViewModel.items.collectAsStateWithLifecycle()
    val selectedFilter by collectionViewModel.selectedFilter.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val lightBg = LocalLightBackground.current
    val parentEntry = remember(navHostController.currentBackStackEntry) {
        navHostController.getBackStackEntry("main_graph")
    }
    val playlistViewModel: PlaylistScreenViewModel = androidx.hilt.navigation.compose.hiltViewModel(parentEntry)

    LaunchedEffect(Unit) {
        collectionViewModel.syncPlaylists()
    }

    // Refresh on Playlists Changes(Delete/Rename)
    LaunchedEffect(Unit) {
        playlistViewModel.playlistsChanged.collect {
            collectionViewModel.syncPlaylists()
        }
    }


    val filterOptions = listOf(
        null to "All",
        CollectionItemType.ALBUM to "Albums",
        CollectionItemType.ARTIST to "Artists",
        CollectionItemType.SONG to "Songs",
        CollectionItemType.PLAYLIST to "Playlists"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
    ) {
        // Header
        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDark) PissGreen else PissBlack,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(filterOptions) { (type, label) ->
                FilterChip(
                    selected = selectedFilter == type,
                    onClick = { collectionViewModel.setFilter(type) },
                    label = { Text(label) },
                    shape = RoundedCornerShape(2.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isDark) PissGreen else PissBlack,
                        selectedLabelColor = if (isDark) PissBlack else lightBg,
                        containerColor = Color.Transparent,
                        labelColor = if (isDark) PissGreen else PissBlack
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isDark) PissGreen.copy(alpha = (0.7f)) else PissBlack,
                        selectedBorderColor = if (isDark) PissGreen.copy(alpha = 0.7f) else PissBlack,
                        enabled = true,
                        selected = selectedFilter == type
                    )
                )
            }
        }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    CollectionListItem(
                        item = item,
                        onClick = {
                            when (item.type) {
                                CollectionItemType.ALBUM -> {
                                    val encodedImage =
                                        android.net.Uri.encode(item.coverArtUrl ?: "")
                                    navHostController.navigate(Screen.AlbumDetails.route + "/${item.id}/${encodedImage}")
                                }

                                CollectionItemType.ARTIST -> {
                                    artistsViewModel?.setSelectedArtist(
                                        MediaData.Artist(
                                            navidromeID = item.id,
                                            name = item.title,
                                            artistImageUrl = item.coverArtUrl
                                        )
                                    )
                                    val encodedName = android.net.Uri.encode(item.title)
                                    navHostController.navigate(
                                        Screen.ArtistDetails.route + "?artistId=${item.id}&artistName=$encodedName"
                                    ) {
                                        launchSingleTop = true
                                    }
                                }

                                CollectionItemType.SONG -> {
                                    coroutineScope.launch {
                                        val allSongs = items
                                            .filter { it.type == CollectionItemType.SONG }
                                            .map { it.toMediaItem() }
                                        val index = items
                                            .filter { it.type == CollectionItemType.SONG }
                                            .indexOf(item)
                                        SongHelper.play(allSongs, index, mediaController)
                                    }
                                }

                                CollectionItemType.PLAYLIST -> {
                                    val playlistMediaItem = item.toMediaItem()
                                    playlistViewModel.setSelectedPlaylist(playlistMediaItem)
                                    navHostController.navigate(Screen.PlaylistDetails.route) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        },
                        onRemove = {
                            if (item.type == CollectionItemType.PLAYLIST) {
                                playlistViewModel.deletePlaylist(item.id)
                            }
                            collectionViewModel.removeFromCollection(item.id)
                        }
                    )
                }
            }
        }
    }

@Composable
fun CollectionListItem(
    item: CollectionItem,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val isDark = LocalIsDark.current
    val context = LocalContext.current
    var isRemoved by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val lightBg = LocalLightBackground.current
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.7f },
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                showDeleteConfirm = true
                false
            } else false
        }
    )

    AnimatedVisibility(
        visible = !isRemoved,
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic)
        ) + fadeOut(animationSpec = tween(durationMillis = 650))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val color = when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.Settled -> Color.Transparent
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        else -> Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color, RoundedCornerShape(2.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.trash_light),
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                enableDismissFromStartToEnd = false
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) PissBlack else lightBg)
                        .clickable(onClick = onClick)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Cover Data w/ Custom Cover Support
                    val coverData = if (item.type == CollectionItemType.PLAYLIST) {
                        rememberPlaylistCoverUri(
                            playlistId = item.id,
                            fallbackUri = item.coverArtUrl
                        ) ?: item.coverArtUrl
                    } else {
                        item.coverArtUrl
                    }

                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(coverData)
                            .crossfade(true)
                            .size(256)
                            .build(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (item.type) {
                                        CollectionItemType.ALBUM -> "\uD83D\uDCBF"
                                        CollectionItemType.ARTIST -> "\uD83C\uDFA4"
                                        CollectionItemType.SONG -> "\uD83C\uDFB5"
                                        CollectionItemType.PLAYLIST -> "\uD83D\uDCCB"
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )

                    Spacer(Modifier.width(12.dp))

                    // Title + Subtitle + Type
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isDark) PissGreen else PissBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.subtitle != null) {
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) PissGreen else PissBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = item.type.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) PissGreen else PissBlack
                        )
                    }
                }
            }

            // Confirm Delete Overlay
            if (showDeleteConfirm) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.error,
                            RoundedCornerShape(2.dp)
                        )
                        .clickable {
                            isRemoved = true
                            showDeleteConfirm = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.trash_light),
                        contentDescription = "Confirm delete",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }

    // Reset Confirm State
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.Settled) {
            showDeleteConfirm = false
        }
    }

    LaunchedEffect(isRemoved) {
        if (isRemoved) {
            delay(450)
            onRemove()
        }
    }
}

fun CollectionItem.toMediaItem(): MediaItem {
    val mediaType = when (this.type) {
        CollectionItemType.PLAYLIST -> MediaMetadata.MEDIA_TYPE_PLAYLIST
        else -> MediaMetadata.MEDIA_TYPE_MUSIC
    }

    val mediaMetadata = MediaMetadata.Builder()
        .setTitle(this.title)
        .setArtist(this.subtitle)
        .setAlbumTitle(this.album)
        .setArtworkUri(this.coverArtUrl?.toUri())
        .setTrackNumber(this.trackNumber)
        .setRecordingYear(this.year)
        .setIsBrowsable(this.type == CollectionItemType.PLAYLIST)
        .setIsPlayable(this.type != CollectionItemType.PLAYLIST)
        .setMediaType(mediaType)
        .setDurationMs(this.duration?.times(1000)?.toLong())
        .setExtras(Bundle().apply {
            putString("navidromeID", this@toMediaItem.id)
            putInt("duration", this@toMediaItem.duration ?: 0)
        })
        .build()

    return MediaItem.Builder()
        .setMediaId(this.streamUrl ?: this.id)
        .setUri(this.streamUrl?.toUri())
        .setMediaMetadata(mediaMetadata)
        .build()
}
