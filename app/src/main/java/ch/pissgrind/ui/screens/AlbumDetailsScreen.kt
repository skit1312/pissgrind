package ch.pissgrind.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import ch.pissgrind.ui.theme.LocalIsDark
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ch.pissgrind.R
import ch.pissgrind.formatMilliseconds
import ch.pissgrind.managers.settings.AppearanceSettingsManager
import ch.pissgrind.player.SongHelper
import ch.pissgrind.ui.elements.HorizontalSongCard
import ch.pissgrind.ui.elements.dialogs.AddSongToPlaylist
import ch.pissgrind.ui.elements.dialogs.dialogFocusable
import ch.pissgrind.ui.elements.dialogs.showAddSongToPlaylistDialog
import ch.pissgrind.ui.elements.dialogs.songToAddToPlaylist
import ch.pissgrind.ui.viewmodels.AlbumDetailsViewModel
import kotlinx.coroutines.launch
import ch.pissgrind.data.local.entity.CollectionItem
import ch.pissgrind.data.local.entity.CollectionItemType
import ch.pissgrind.data.model.Screen
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen
import androidx.compose.ui.focus.focusRequester
import ch.pissgrind.player.playNext
import ch.pissgrind.ui.theme.LocalLightBackground

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalFoundationApi
@Composable
fun AlbumDetails(
    selectedAlbumId: String = "",
    selectedAlbumImage: Uri = Uri.EMPTY,
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    viewModel: AlbumDetailsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val collectionViewModel: CollectionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val isDark = LocalIsDark.current
    val textColor = if (isDark) PissGreen else PissBlack
    val borderColor = if (isDark) PissGreen else PissBlack
    val optionsButtonContainer = if (isDark) PissGreen else PissBlack
    val lightBg = LocalLightBackground.current
    val optionsButtonContent = if (isDark) PissBlack else lightBg
    var showLoading by remember { mutableStateOf(false) }
    val currentAlbum = viewModel.songsInAlbum.collectAsStateWithLifecycle().value
    val showTrackNumbers by AppearanceSettingsManager(LocalContext.current).showTrackNumbersFlow.collectAsStateWithLifecycle(false)

    val context = LocalContext.current

    LaunchedEffect(selectedAlbumId) {
        viewModel.loadAlbumDetails(selectedAlbumId)
    }

    // Loading spinner
    AnimatedVisibility(
        visible = showLoading,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = textColor,
                strokeWidth = 6.dp
            )
            Text(
                text = "Loading",
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }

    // Main Content
    AnimatedVisibility(
        visible = currentAlbum.isNotEmpty(),
        enter = fadeIn()
    ) {
        val requester = remember { androidx.compose.ui.focus.FocusRequester() }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            requester.requestFocus()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .dialogFocusable(),
            contentPadding = PaddingValues(
                bottom = 16.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            ),
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Back button row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { navHostController.popBackStack() },
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.size(32.dp),
                            contentPadding = PaddingValues(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                contentColor = textColor
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                tint = textColor,
                                contentDescription = "Back",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Album Cover
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(selectedAlbumImage.toString())
                            .diskCacheKey(selectedAlbumId + "_hires")
                            .memoryCacheKey(selectedAlbumId + "_hires")
                            .crossfade(true)
                            .build(),
                        placeholder = painterResource(R.drawable.placeholder),
                        fallback = painterResource(R.drawable.placeholder),
                        contentScale = ContentScale.Crop,
                        contentDescription = "Album Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )

                    Spacer(Modifier.height(16.dp))

                    // Album Name
                    Text(
                        text = currentAlbum[0].mediaMetadata.title.toString(),
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 32.sp,
                    )
                    Spacer(Modifier.height(8.dp))

                    // Artist Name
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val artistId =
                                    currentAlbum[0].mediaMetadata.extras?.getString("artistId")
                                        ?: ""
                                val artistName =
                                    currentAlbum[0].mediaMetadata.artist.toString()
                                if (artistId.isNotBlank()) {
                                    navHostController.navigate(
                                        Screen.ArtistDetails.route + "?artistId=${artistId}&artistName=${
                                            java.net.URLEncoder.encode(artistName, "UTF-8")
                                        }"
                                    ) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(24.dp))
                            Text(
                                text = currentAlbum[0].mediaMetadata.artist.toString(),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Medium,
                                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.caret_right_light),
                                contentDescription = "Go to artist",
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Duration
                    Text(
                        text = formatMilliseconds(
                            currentAlbum[0].mediaMetadata.durationMs?.div(1000)?.toInt() ?: 0
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Normal,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ===== BUTTONS =====
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                SongHelper.play(
                                    currentAlbum.subList(1, currentAlbum.size),
                                    0,
                                    mediaController
                                )
                            }
                        },
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .widthIn(min = 112.dp, max = 280.dp)
                            .focusRequester(requester)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(20.dp)
                        ) {
                            Text(stringResource(R.string.Action_Play), maxLines = 1)
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.play_fill),
                                contentDescription = "Play Album"
                            )
                        }
                    }

                    // Shuffle Button
                    OutlinedButton(
                        onClick = {
                            mediaController?.shuffleModeEnabled = true
                            coroutineScope.launch {
                                val random =
                                    currentAlbum.subList(1, currentAlbum.size).indices.random()
                                SongHelper.play(
                                    currentAlbum.subList(1, currentAlbum.size),
                                    random,
                                    mediaController
                                )
                            }
                        },
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.widthIn(min = 112.dp, max = 280.dp),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(20.dp)
                        ) {
                            Text(
                                stringResource(R.string.Action_Shuffle),
                                maxLines = 1,
                                color = textColor
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                ImageVector.vectorResource(R.drawable.shuffle_angular_light),
                                contentDescription = "Shuffle Album",
                                tint = textColor
                            )
                        }
                    }

                    // Options Dropdown Button
                    if (currentAlbum[0].mediaMetadata.extras?.getString("navidromeID")
                            ?.startsWith("Local_") == false
                    ) {
                        val albumNavidromeId =
                            currentAlbum[0].mediaMetadata.extras?.getString("navidromeID") ?: ""
                        val isInCollection by collectionViewModel.isInCollection(albumNavidromeId)
                            .collectAsState(initial = false)
                        var showMenu by remember { mutableStateOf(false) }

                        Box {
                            OutlinedButton(
                                onClick = { showMenu = true },
                                shape = RoundedCornerShape(0.dp),
                                modifier = Modifier.size(40.dp),
                                contentPadding = PaddingValues(0.dp),
                                border = BorderStroke(1.dp, borderColor),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = optionsButtonContainer,
                                    contentColor = optionsButtonContent
                                )
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.dots_three_vertical_light),
                                    contentDescription = "More options",
                                    tint = optionsButtonContent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                shape = RoundedCornerShape(2.dp)
                            ) {
                                // Add/Remove Library
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isInCollection) "Remove from Library"
                                            else "Add to Library"
                                        )
                                    },
                                    onClick = {
                                        collectionViewModel.toggleInCollection(
                                            CollectionItem(
                                                id = albumNavidromeId,
                                                type = CollectionItemType.ALBUM,
                                                title = currentAlbum[0].mediaMetadata.title.toString(),
                                                subtitle = currentAlbum[0].mediaMetadata.artist.toString(),
                                                coverArtUrl = selectedAlbumImage.toString()
                                            )
                                        )
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(
                                                if (isInCollection) R.drawable.minus_circle_fill
                                                else R.drawable.plus_circle_light
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )

                                // Add Album to Playlist
                                DropdownMenuItem(
                                    text = { Text("Add to Playlist") },
                                    onClick = {
                                        showMenu = false
                                        // Set first song to trigger dialog, songs will be added via queue
                                        if (currentAlbum.size > 1) {
                                            songToAddToPlaylist.value = currentAlbum[1]
                                            showAddSongToPlaylistDialog.value = true
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.plus_light),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Album Songs
            val groupedAlbums = currentAlbum.subList(1, currentAlbum.size).groupBy { song ->
                song.mediaMetadata.discNumber
            }

            if (groupedAlbums.size > 1) {
                groupedAlbums.forEach { (discNumber, albumsInGroup) ->
                    item {
                        Column {
                            Text(
                                text = stringResource(R.string.Album_Disc_Number) + discNumber.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier
                                    .height(1.dp)
                                    .fillMaxWidth(),
                                color = textColor,
                            )
                        }
                    }
                    items(albumsInGroup) { song ->
                        HorizontalSongCard(
                            song = song,
                            modifier = Modifier.animateItem(),
                            showTrackNumber = showTrackNumbers,
                            onClick = {
                                coroutineScope.launch {
                                    SongHelper.play(
                                        currentAlbum.subList(1, currentAlbum.size),
                                        currentAlbum.subList(1, currentAlbum.size).indexOf(song),
                                        mediaController
                                    )
                                }
                            },
                            onAddToQueue = { mediaController?.addMediaItem(song) },
                            onPlayNext = { mediaController?.playNext(song) }
                        )
                    }
                }
            } else {
                items(currentAlbum.subList(1, currentAlbum.size)) { song ->
                    HorizontalSongCard(
                        song = song,
                        modifier = Modifier.animateItem(),
                        showTrackNumber = showTrackNumbers,
                        onClick = {
                            coroutineScope.launch {
                                SongHelper.play(
                                    currentAlbum.subList(1, currentAlbum.size),
                                    currentAlbum.subList(1, currentAlbum.size).indexOf(song),
                                    mediaController
                                )
                            }
                        },
                        onAddToQueue = { mediaController?.addMediaItem(song) },
                        onPlayNext = { mediaController?.playNext(song) }
                    )
                }
            }
        }
    }

    if (showAddSongToPlaylistDialog.value) {
        AddSongToPlaylist(setShowDialog = { showAddSongToPlaylistDialog.value = it })
    }
}
