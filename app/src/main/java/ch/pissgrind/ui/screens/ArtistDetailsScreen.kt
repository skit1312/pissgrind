package ch.pissgrind.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ch.pissgrind.R
import ch.pissgrind.data.model.Screen
import ch.pissgrind.data.model.toAlbum
import ch.pissgrind.fadingEdge
import ch.pissgrind.player.SongHelper
import ch.pissgrind.ui.elements.AlbumCard
import ch.pissgrind.ui.elements.dialogs.dialogFocusable
import ch.pissgrind.ui.viewmodels.ArtistsScreenViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder
import androidx.compose.runtime.collectAsState
import ch.pissgrind.data.local.entity.CollectionItem
import ch.pissgrind.data.local.entity.CollectionItemType
import ch.pissgrind.ui.theme.LocalLightBackground
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalFoundationApi
@Composable
@Preview
fun ArtistDetails(
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    viewModel: ArtistsScreenViewModel = hiltViewModel()
) {
    val lightBg = LocalLightBackground.current
    val collectionViewModel: CollectionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val showLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val artist = viewModel.selectedArtist.collectAsStateWithLifecycle().value
    val artistAlbums = viewModel.artistAlbums.collectAsStateWithLifecycle().value
    val artistId = artist?.navidromeID ?: ""
    val isInCollection: Boolean by collectionViewModel.isInCollection(artistId)
        .collectAsState(initial = false)
    val context = LocalContext.current
    val imageFadingEdge = Brush.verticalGradient(listOf(Color.Red.copy(0.75f), Color.Transparent))

    val coroutineScope = rememberCoroutineScope()

    val isDark = LocalIsDark.current


    // Loading spinner
    AnimatedVisibility(
        visible = showLoading,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp
            )
            Text(
                text = "Loading",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }

    // Main Content
    AnimatedVisibility(
        visible = artist?.name?.isNotBlank() == true,
        enter = fadeIn()
    ) {
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                )
                .dialogFocusable(),
            columns = GridCells.Adaptive(96.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(12.dp)
        ) {

            val groupedAlbums =
                artistAlbums.groupBy { it.mediaMetadata.recordingYear }
                    .toSortedMap(compareByDescending { it })

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Box(
                        modifier = Modifier
                            .height(192.dp)
                            .fillMaxWidth()
                    ) {

                        val blurImageUrl = remember(artistAlbums, artist) {
                            artistAlbums.firstOrNull()?.mediaMetadata?.artworkUri?.toString()
                                ?.replace("size=128", "size=128")
                                ?: artist?.artistImageUrl
                        }

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(blurImageUrl)
                                .diskCacheKey("artist_bg_${artist?.navidromeID}")
                                .crossfade(true)
                                .build(),
                            placeholder = painterResource(R.drawable.skull_light),
                            fallback = painterResource(R.drawable.skull_light),
                            contentScale = ContentScale.Crop,
                            contentDescription = "Artist Background",
                            modifier = Modifier
                                .fillMaxSize()
                                .fadingEdge(imageFadingEdge)
                                .clip(
                                    if (artist?.description != "") RoundedCornerShape(2.dp, 2.dp, 0.dp, 0.dp)
                                    else RoundedCornerShape(2.dp)
                                )
                                .blur(6.dp)
                        )

                        // Album Name and Artist
                        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                            Text(
                                text = artist?.name.toString(),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                lineHeight = 32.sp
                            )
                        }
                    }
                }
            }

// Play and Shuffle Buttons
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val allArtistSongsList = artistAlbums.map {
                                    it.mediaMetadata.extras?.getString("navidromeID").let {
                                        val album = viewModel.getAlbum(it ?: "")
                                        if (album.isNotEmpty())
                                            album.subList(1, album.size)
                                        else
                                            emptyList()
                                    }
                                }
                                SongHelper.play(
                                    allArtistSongsList.flatten(),
                                    0,
                                    mediaController
                                )
                            }
                        },
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.widthIn(min = 128.dp, max = 320.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(stringResource(R.string.Action_Play), maxLines = 1)
                            Spacer(Modifier.width(6.dp))
                            Icon(imageVector = ImageVector.vectorResource(R.drawable.play_fill), "Play")
                        }
                    }

                    // Shuffle Button
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val allArtistSongsList = artistAlbums.map {
                                    it.mediaMetadata.extras?.getString("navidromeID").let {
                                        val album = viewModel.getAlbum(it ?: "")
                                        if (album.isNotEmpty())
                                            album.subList(1, album.size)
                                        else
                                            emptyList()
                                    }
                                }
                                mediaController?.shuffleModeEnabled = true
                                val random = allArtistSongsList.indices.random()
                                SongHelper.play(
                                    allArtistSongsList.flatten(),
                                    random,
                                    mediaController
                                )
                            }
                        },
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.widthIn(min = 128.dp, max = 320.dp),
                        border = BorderStroke(1.dp, if (isDark) PissGreen else PissBlack)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(
                                stringResource(R.string.Action_Shuffle),
                                maxLines = 1,
                                color = if (isDark) PissGreen else PissBlack
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                ImageVector.vectorResource(R.drawable.shuffle_angular_light),
                                contentDescription = "Shuffle",
                                tint = if (isDark) PissGreen else PissBlack
                            )
                        }
                    }

                    // Add to Library Button
                    OutlinedButton(
                        onClick = {
                            collectionViewModel.toggleInCollection(
                                CollectionItem(
                                    id = artistId,
                                    type = CollectionItemType.ARTIST,
                                    title = artist?.name ?: "",
                                    subtitle = "${artistAlbums.size} albums",
                                    coverArtUrl = artist?.artistImageUrl
                                )
                            )
                            Toast.makeText(
                                context,
                                if (isInCollection) "${artist?.name} removed from Library"
                                else "${artist?.name} added to Library",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(1.dp, if (isDark) PissGreen else PissBlack),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isDark) PissGreen else PissBlack,
                            contentColor = if (isDark) PissBlack else lightBg
                        )
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(
                                if (isInCollection) R.drawable.minus_circle_fill
                                else R.drawable.plus_circle_light
                            ),
                            contentDescription = if (isInCollection) "Remove from Library" else "Add to Library",
                            tint = if (isDark) PissBlack else lightBg,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Discography Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.Screen_Discography),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            groupedAlbums.forEach { (groupName, albumsInGroup) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = groupName.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(top = 12.dp)
                    )
                }

                itemsIndexed(albumsInGroup) { index, album ->
                    AlbumCard(
                        album = album,
                        onClick = {
                            val album = album.toAlbum()
                            val encodedImage = URLEncoder.encode(album.coverArt, "UTF-8")
                            navHostController.navigate(Screen.AlbumDetails.route + "/${album.navidromeID}/$encodedImage") {
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }
        }
    }
}