package ch.pissgrind.ui.playing

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import ch.pissgrind.managers.settings.AppearanceSettingsManager
import kotlinx.coroutines.launch
import ch.pissgrind.data.recommendation.TrainingModeManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ch.pissgrind.ui.screens.CollectionViewModel


enum class NowPlayingAlignment {
    LEFT, CENTER, RIGHT
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun NowPlayingContent(
    mediaController: MediaController? = null,
    metadata: MediaMetadata? = null,
    viewModel: NowPlayingViewModel = viewModel(),
    onBackgroundStyleChange: (NowPlayingBackground) -> Unit = {},
    onNavigateToAlbum: (albumId: String, imageUrl: String) -> Unit = { _, _ -> },
    onNavigateToArtist: (artistId: String, artistName: String) -> Unit = { _, _ -> }
) {
    val acontext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backgroundStyle by viewModel.backgroundStyle.collectAsStateWithLifecycle(
        NowPlayingBackground.PISSGRIND
    )
    val backgroundDarkMode by viewModel.isBackgroundDark.collectAsStateWithLifecycle()
    val playQueueOpen by viewModel.playQueueOpen.collectAsStateWithLifecycle()
    val detailsOpen by viewModel.detailsOpen.collectAsStateWithLifecycle()
    val colors by viewModel.paletteColors.collectAsStateWithLifecycle()
    val iconTextColor by viewModel.iconTextColor.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()

    // Training mode
    val shouldShowFeedback by viewModel.shouldShowFeedback.collectAsStateWithLifecycle()
    val isTrainingPlaylist by TrainingModeManager.isTrainingPlaylist.collectAsStateWithLifecycle()

    // ===== LIBRARY & FAVOURITES STATE =====
    val navidromeId = metadata?.extras?.getString("navidromeID") ?: ""
    val collectionViewModel: CollectionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val isInLibrary by collectionViewModel.isInCollection(navidromeId)
        .collectAsState(initial = false)
    val isInFavourites by collectionViewModel.isSongInFavourites(navidromeId)
        .collectAsState(initial = false)

    // Check favourites when song changes
    LaunchedEffect(navidromeId) {
        if (navidromeId.isNotBlank()) {
            viewModel.checkIfInFavourites(navidromeId)
        }
    }

    // ===== ADD TO PLAYLIST DIALOG =====
    var showPlaylistDialog by remember { mutableStateOf(false) }

    LaunchedEffect(mediaController?.currentTimeline?.windowCount) {
        kotlinx.coroutines.delay(500)
        viewModel.updatePaletteFromUri(metadata?.artworkUri, backgroundStyle, isSystemDark)
    }

    val targetOverlayColor = if (backgroundStyle == NowPlayingBackground.PISSGRIND) {
        if (backgroundDarkMode) Color.Black.copy(0.2f) else Color.White.copy(0.2f)
    } else {
        Color.Transparent
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NowPlaying_Background(colors, backgroundStyle, targetOverlayColor)

        val bgCoroutineScope = rememberCoroutineScope()
        val appearanceSettingsManager = remember { AppearanceSettingsManager(acontext) }

        NowPlayingPortrait(
            mediaController = mediaController,
            isTrainingMode = shouldShowFeedback,
            metadata = metadata,
            iconColor = iconTextColor,
            backgroundStyle = backgroundStyle,
            isInLibrary = isInLibrary,
            isInFavourites = isInFavourites,
            onToggleQueue = { viewModel.setPlayQueueOpen(!playQueueOpen) },
            onBackgroundStyleChange = { newStyle ->
                bgCoroutineScope.launch {
                    appearanceSettingsManager.setBackgroundType(newStyle)
                }
            },
            onNavigateToAlbum = onNavigateToAlbum,
            onNavigateToArtist = onNavigateToArtist,
            onTrainingThumbsUp = {
                val songId = mediaController?.currentMediaItem?.mediaId ?: ""
                val albumId = metadata?.extras?.getString("albumId") ?: ""
                val artistId = metadata?.extras?.getString("artistId") ?: ""
                val artistName = metadata?.artist?.toString() ?: ""
                val albumName = metadata?.albumTitle?.toString() ?: ""
                val genres = metadata?.genre?.toString()
                    ?.split(",", "/")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()
                viewModel.submitFeedback(albumId, albumName, artistName, genres, liked = true, artistId = artistId, songId = songId)
            },
            onTrainingThumbsDown = {
                val albumId = metadata?.extras?.getString("albumId") ?: ""
                val songId = metadata?.extras?.getString("navidromeID") ?: ""
                val artistId = metadata?.extras?.getString("artistId") ?: ""
                val artistName = metadata?.artist?.toString() ?: ""
                val albumName = metadata?.albumTitle?.toString() ?: ""
                val genres = metadata?.genre?.toString()
                    ?.split(",", "/")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()
                viewModel.submitFeedback(albumId, albumName, artistName, genres, liked = false, artistId = artistId, songId = songId)
                mediaController?.seekToNext()
            },
            onStopTraining = {
                viewModel.stopTrainingMode()
            },
            onAddToFavourites = {
                mediaController?.currentMediaItem?.let { collectionViewModel.addSongToFavourites(it) }
            },
            onRemoveFromFavourites = {
                mediaController?.currentMediaItem?.let { collectionViewModel.removeSongFromFavourites(it) }
            },
            onAddToLibrary = {
                viewModel.addCurrentSongToLibrary(metadata)
            },
            onRemoveFromLibrary = {
                viewModel.removeCurrentSongFromLibrary(metadata)
            },
            onAddToPlaylist = {
                showPlaylistDialog = true
            }
        )

        // Queue Bottom Sheet
        val playQueueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        if (playQueueOpen) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setPlayQueueOpen(false) },
                sheetState = playQueueSheetState,
            ) {
                PlayQueueContent(mediaController = mediaController)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (detailsOpen) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setDetailsOpen(false) },
                sheetState = playQueueSheetState,
            ) {
                NowPlayingDetails(mediaController = mediaController)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // ===== ADD TO PLAYLIST DIALOG =====
    if (showPlaylistDialog) {
        val currentSong = mediaController?.currentMediaItem
        if (currentSong != null) {
            ch.pissgrind.ui.elements.dialogs.songToAddToPlaylist.value = currentSong
            ch.pissgrind.ui.elements.dialogs.showAddSongToPlaylistDialog.value = true
            showPlaylistDialog = false
        }
    }

    if (ch.pissgrind.ui.elements.dialogs.showAddSongToPlaylistDialog.value) {
        ch.pissgrind.ui.elements.dialogs.AddSongToPlaylist(
            setShowDialog = { ch.pissgrind.ui.elements.dialogs.showAddSongToPlaylistDialog.value = it }
        )
    }
}
