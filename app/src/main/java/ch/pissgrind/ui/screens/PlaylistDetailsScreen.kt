@file:Suppress("PreviewAnnotationInFunctionWithParameters")

package ch.pissgrind.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import ch.pissgrind.ui.theme.LocalIsDark
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import ch.pissgrind.R
import ch.pissgrind.formatMilliseconds
import ch.pissgrind.player.SongHelper
import ch.pissgrind.player.playNext
import ch.pissgrind.player.rememberManagedMediaController
import ch.pissgrind.ui.elements.HorizontalSongCard
import ch.pissgrind.ui.elements.dialogs.dialogFocusable
import ch.pissgrind.ui.theme.LocalLightBackground
import ch.pissgrind.ui.viewmodels.PlaylistScreenViewModel
import kotlinx.coroutines.launch
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Preview(showBackground = true, showSystemUi = false)
@Composable
fun PlaylistDetails(
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = rememberManagedMediaController().value,
    viewModel: PlaylistScreenViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val isDark = LocalIsDark.current
    val textColor = if (isDark) PissGreen else PissBlack
    val subtleColor = if (isDark) PissGreen.copy(alpha = 0.9f) else PissBlack.copy(alpha = 0.9f)
    val lightBg = LocalLightBackground.current
    val buttonContainer = if (isDark) PissGreen else PissBlack
    val buttonContent = if (isDark) PissBlack else lightBg

    val requester = remember { FocusRequester() }

    val playlistMetadata =
        viewModel.selectedPlaylist.collectAsStateWithLifecycle().value?.mediaMetadata
    val playlistId = playlistMetadata?.extras?.getString("navidromeID") ?: ""
    val playlistSongs = viewModel.selectedPlaylistSongs.collectAsStateWithLifecycle().value
    val isLoading = viewModel.isLoading.collectAsStateWithLifecycle().value

    val playlistDuration =
        remember(playlistSongs) { playlistSongs.sumOf { it.mediaMetadata.durationMs ?: 0 } }

    val coroutineScope = rememberCoroutineScope()


    // Dialog states
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    // Custom cover image
    var customCoverUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(playlistId) {
        if (playlistId.isNotBlank()) {
            val prefs = context.getSharedPreferences("playlist_covers", android.content.Context.MODE_PRIVATE)
            val saved = prefs.getString("cover_$playlistId", null)
            customCoverUri = saved?.let { Uri.parse(it) }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            customCoverUri = it

            val currentPlaylistId = playlistMetadata?.extras?.getString("navidromeID") ?: ""
            if (currentPlaylistId.isNotBlank()) {
                val prefs = context.getSharedPreferences("playlist_covers", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("cover_$currentPlaylistId", it.toString()).apply()
                android.util.Log.d("PlaylistCover", "Saved cover for '$currentPlaylistId': $it")
            }
        }
    }

    // ===== DELETE CONFIRMATION DIALOG =====
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = if (isDark) PissBlack else lightBg,
            titleContentColor = textColor,
            textContentColor = subtleColor,
            title = { Text("Delete Playlist?", fontWeight = FontWeight.Bold) },
            text = { Text("This action can not be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.deletePlaylist(playlistId)
                            showDeleteDialog = false
                            navHostController.popBackStack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("Cancel", color = textColor)
                }
            },
            shape = RoundedCornerShape(2.dp)
        )
    }

    // ===== RENAME DIALOG =====
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = if (isDark) PissBlack else lightBg,
            titleContentColor = textColor,
            title = { Text("Rename Playlist", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(2.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                        focusedLabelColor = textColor,
                        unfocusedLabelColor = subtleColor,
                        cursorColor = textColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = subtleColor
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.renamePlaylist(playlistId, renameText)
                            showRenameDialog = false
                            navHostController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("playlists_changed", true)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonContainer,
                        contentColor = buttonContent
                    ),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRenameDialog = false },
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("Cancel", color = textColor)
                }
            },
            shape = RoundedCornerShape(2.dp)
        )
    }

    // Loading spinner
    AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
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
    AnimatedVisibility(visible = !isLoading && playlistSongs.isNotEmpty(), enter = fadeIn()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .dialogFocusable(),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = 16.dp, start = 12.dp, end = 12.dp
            )
        ) {
            // ===== HEADER WITH 1:1 COVER =====
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Back button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Button(
                            onClick = { navHostController.popBackStack() },
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.size(32.dp),
                            contentPadding = PaddingValues(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
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

                    // 1:1 Cover Image (clickable to change)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { imagePickerLauncher.launch(arrayOf("image/*")) },
                        contentAlignment = Alignment.Center
                    ) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(
                                    customCoverUri
                                        ?: if (playlistId.startsWith("Local"))
                                            playlistMetadata?.artworkData
                                        else
                                            playlistMetadata?.artworkUri
                                )
                                .crossfade(true)
                                .diskCacheKey(playlistId.ifEmpty { playlistMetadata?.title.toString() })
                                .build(),
                            contentScale = ContentScale.Crop,
                            contentDescription = "Playlist Cover Art",
                            modifier = Modifier.fillMaxSize()
                        )

                        // Overlay hint
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    RoundedCornerShape(2.dp)
                                )
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.pencil_simple_light),
                                contentDescription = "Change cover",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Playlist Name
                    Text(
                        text = playlistMetadata?.title.toString(),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 32.sp,
                    )

                    Spacer(Modifier.height(4.dp))

                    // Duration + Song Count
                    Text(
                        text = "${playlistSongs.size} songs • ${formatMilliseconds((playlistDuration / 1000).toInt())}",
                        color = subtleColor,
                        fontWeight = FontWeight.Normal,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))
                }
            }

            // ===== BUTTONS =====
            item {
                var showMenu by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                SongHelper.play(playlistSongs, 0, mediaController)
                            }
                        },
                        modifier = Modifier
                            .widthIn(min = 100.dp, max = 280.dp)
                            .focusRequester(requester),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonContainer,
                            contentColor = buttonContent
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(stringResource(R.string.Action_Play), maxLines = 1)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.play_fill),
                                contentDescription = "Play Playlist"
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            mediaController?.shuffleModeEnabled = true
                            coroutineScope.launch {
                                val random = playlistSongs.indices.random()
                                SongHelper.play(playlistSongs, random, mediaController)
                            }
                        },
                        modifier = Modifier.widthIn(min = 100.dp, max = 280.dp),
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, if (isDark) PissGreen else PissBlack)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(
                                stringResource(R.string.Action_Shuffle),
                                maxLines = 1,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                ImageVector.vectorResource(R.drawable.shuffle_angular_light),
                                contentDescription = "Shuffle Playlist",
                                tint = textColor
                            )
                        }
                    }

                    // Dropdown Menu Button
                    Box {
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .aspectRatio(1f)
                                .background(buttonContainer, RoundedCornerShape(2.dp))
                                .clickable { showMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.dots_three_vertical_light),
                                contentDescription = "More options",
                                tint = buttonContent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    showMenu = false
                                    renameText = playlistMetadata?.title?.toString() ?: ""
                                    showRenameDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.pencil_light),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Change Cover") },
                                onClick = {
                                    showMenu = false
                                    imagePickerLauncher.launch(arrayOf("image/*"))
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.pencil_simple_light),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.trash_light),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // ===== SONGS WITH SWIPE-TO-REMOVE (with confirm) =====
            itemsIndexed(
                items = playlistSongs,
                key = { index, song ->
                    "${song.mediaMetadata.extras?.getString("navidromeID") ?: index}_$index"
                }
            ) { index, song ->
                HorizontalSongCard(
                    song = song,
                    modifier = Modifier.animateItem(),
                    coverArtSize = 256,
                    onClick = {
                        coroutineScope.launch {
                            SongHelper.play(playlistSongs, index, mediaController)
                        }
                    },
                    onAddToQueue = {
                        mediaController?.addMediaItem(song)
                    },
                    onPlayNext = {
                        mediaController?.playNext(song)
                    },
                    onRemoveFromPlaylist = {
                        val songId = song.mediaMetadata.extras?.getString("navidromeID") ?: ""
                        coroutineScope.launch {
                            viewModel.removeSongFromPlaylist(playlistId, index, songId)
                        }
                    }
                )
            }
        }
    }
}

