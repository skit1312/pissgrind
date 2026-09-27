package ch.pissgrind.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import ch.pissgrind.ui.theme.LocalIsDark
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import ch.pissgrind.R
import ch.pissgrind.data.model.MediaData
import ch.pissgrind.data.model.Screen
import ch.pissgrind.player.SongHelper
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen
import ch.pissgrind.ui.viewmodels.ArtistsScreenViewModel
import ch.pissgrind.ui.viewmodels.LibraryScreenViewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.net.URLEncoder
import androidx.compose.ui.focus.onFocusChanged
import ch.pissgrind.ui.theme.LocalLightBackground

enum class LibraryTab { ALL, ALBUMS, ARTISTS, SONGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    viewModel: LibraryScreenViewModel = hiltViewModel(),
    artistsViewModel: ArtistsScreenViewModel? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = LocalIsDark.current
    val lightBg = LocalLightBackground.current
    val textColor = if (isDark) PissGreen else PissBlack
    val subtleColor = if (isDark) PissGreen.copy(alpha = 0.5f) else PissBlack.copy(alpha = 0.5f)
    val borderColor = if (isDark) PissGreen else PissBlack

    // Search results
    val filteredAlbums by viewModel.filteredAlbums.collectAsStateWithLifecycle()
    val filteredArtists by viewModel.filteredArtists.collectAsStateWithLifecycle()
    val filteredSongs by viewModel.filteredSongs.collectAsStateWithLifecycle()

    val artistCoverMap by viewModel.artistCoverMap.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // Recent Searches
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(LibraryTab.ALL) }
    var queryText by remember { mutableStateOf("") }
    var isSearchBarFocused by remember { mutableStateOf(false) }

    val isSearching = queryText.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
    ) {
        // Header
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = queryText,
            onValueChange = {
                queryText = it
                viewModel.search(it)
            },
            placeholder = { Text("Search Albums, Artists, Songs...", color = subtleColor) },
            leadingIcon = {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.magnifying_glass_light),
                    contentDescription = "Search",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedBorderColor = borderColor,
                unfocusedBorderColor = borderColor.copy(alpha = 0.8f),
                cursorColor = textColor
            ),
            shape = RoundedCornerShape(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .onFocusChanged { isSearchBarFocused = it.isFocused }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips (only when searching)
        if (isSearching) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LibraryTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = {
                            Text(
                                when (tab) {
                                    LibraryTab.ALL -> "All"
                                    LibraryTab.ALBUMS -> "Albums"
                                    LibraryTab.ARTISTS -> "Artists"
                                    LibraryTab.SONGS -> "Songs"
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = textColor,
                            selectedLabelColor = if (isDark) PissBlack else lightBg,
                            containerColor = if (isDark) PissBlack else lightBg,
                            labelColor = textColor
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = borderColor.copy(alpha = 0.5f),
                            selectedBorderColor = borderColor,
                            enabled = true,
                            selected = selectedTab == tab
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Loading
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp), color = textColor)
            }
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (!isSearching) {
                    // ===== DISCOVER MODE (empty-ish screen) =====
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 50.dp)
                    ) {
                        // Two half-width action buttons (Browse Genres + Recently Added)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ActionTile(
                                    iconRes = R.drawable.music_notes_light,
                                    label = "Browse Genres",
                                    textColor = textColor,
                                    borderColor = borderColor,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        navHostController.navigate(Screen.GenreBrowse.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                                ActionTile(
                                    iconRes = R.drawable.clock_counter_clockwise_light,
                                    label = "Recently Added",
                                    textColor = textColor,
                                    borderColor = borderColor,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        navHostController.navigate(Screen.HomeLists.route + "/recently_added") {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }

                        // Recent Searches when search bar is focused
                        if (recentSearches.isNotEmpty() && isSearchBarFocused) {
                            item { SectionHeader("Recent Searches", textColor) }
                            items(recentSearches, key = { "recent_$it" }) { search ->
                                RecentSearchItem(
                                    query = search,
                                    textColor = textColor,
                                    subtleColor = subtleColor,
                                    onClick = {
                                        queryText = search
                                        viewModel.search(search)
                                    },
                                    onRemove = { viewModel.removeRecentSearch(search) }
                                )
                            }
                        }
                    }
                } else {
                    // ===== SEARCH MODE =====
                    LaunchedEffect(queryText) {
                        if (queryText.length >= 3) {
                            kotlinx.coroutines.delay(2000)
                            viewModel.addRecentSearch(queryText)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        when (selectedTab) {
                            LibraryTab.ALL -> {
                                items(filteredAlbums, key = { "album_${it.mediaId}" }) { album ->
                                    AlbumListItem(album, context, navHostController, textColor, borderColor)
                                }
                                items(filteredArtists, key = { "artist_${it.navidromeID}" }) { artist ->
                                    ArtistListItem(artist, navHostController, artistCoverMap, artistsViewModel, textColor, borderColor)
                                }
                                items(filteredSongs, key = { "song_${it.mediaId}" }) { song ->
                                    SongListItem(song, filteredSongs, mediaController, coroutineScope, textColor, borderColor)
                                }
                            }
                            LibraryTab.ALBUMS -> {
                                items(filteredAlbums, key = { "album_${it.mediaId}" }) { album ->
                                    AlbumListItem(album, context, navHostController, textColor, borderColor)
                                }
                            }
                            LibraryTab.ARTISTS -> {
                                items(filteredArtists, key = { "artist_${it.navidromeID}" }) { artist ->
                                    ArtistListItem(artist, navHostController, artistCoverMap, artistsViewModel, textColor, borderColor)
                                }
                            }
                            LibraryTab.SONGS -> {
                                items(filteredSongs, key = { "song_${it.mediaId}" }) { song ->
                                    SongListItem(song, filteredSongs, mediaController, coroutineScope, textColor, borderColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ===== COMPONENTS =====

@Composable
private fun RecentSearchItem(
    query: String,
    textColor: Color,
    subtleColor: Color,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.clock_counter_clockwise_light),
                contentDescription = null,
                tint = subtleColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = query, style = MaterialTheme.typography.bodyMedium, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(imageVector = ImageVector.vectorResource(R.drawable.x_light), contentDescription = "Remove", tint = subtleColor, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ActionTile(
    iconRes: Int,
    label: String,
    textColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(2.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(iconRes),
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionHeader(title: String, textColor: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun AlbumListItem(album: MediaItem, context: android.content.Context, navHostController: NavHostController, textColor: Color, borderColor: Color) {
    val albumMeta = album.mediaMetadata
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .clickable {
                val navidromeID = albumMeta.extras?.getString("navidromeID") ?: ""
                val coverArt = albumMeta.artworkUri?.toString()?.replace("size=128", "size=300") ?: ""
                val encodedImage = URLEncoder.encode(coverArt, "UTF-8")
                navHostController.navigate(Screen.AlbumDetails.route + "/${navidromeID}/$encodedImage") { launchSingleTop = true }
            }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context).data(albumMeta.artworkUri?.toString()?.replace("size=128", "size=200")).crossfade(true).build(),
            contentDescription = "Album Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(2.dp)).border(1.dp, borderColor, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = albumMeta.albumTitle?.toString() ?: "", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = buildString {
                    append("Album")
                    albumMeta.artist?.toString()?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                    albumMeta.releaseYear?.let { if (it > 0) append(" · $it") }
                },
                style = MaterialTheme.typography.bodyMedium, color = textColor.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ArtistListItem(artist: MediaData.Artist, navHostController: NavHostController, artistCoverMap: Map<String, String>, artistsViewModel: ArtistsScreenViewModel?, textColor: Color, borderColor: Color) {
    val context = LocalContext.current
    val coverUrl = artistCoverMap[artist.name]?.replace("size=128", "size=200")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .clickable {
                artistsViewModel?.setSelectedArtist(artist)
                val encodedName = android.net.Uri.encode(artist.name)
                navHostController.navigate(Screen.ArtistDetails.route + "?artistId=${artist.navidromeID}&artistName=$encodedName") { launchSingleTop = true }
            }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Artists are round to visually distinguish them (Spotify-style)
        if (coverUrl != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context).data(coverUrl).crossfade(true).build(),
                contentDescription = "Artist Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(CircleShape).border(1.dp, borderColor, CircleShape)
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp).clip(CircleShape).border(1.dp, borderColor, CircleShape)) {
                Text(text = artist.name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = artist.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = "Artist", style = MaterialTheme.typography.bodyMedium, color = textColor.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SongListItem(song: MediaItem, allSongs: List<MediaItem>, mediaController: MediaController?, coroutineScope: kotlinx.coroutines.CoroutineScope, textColor: Color, borderColor: Color) {
    val context = LocalContext.current
    val songMeta = song.mediaMetadata
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .clickable {
                val index = allSongs.indexOf(song)
                coroutineScope.launch { SongHelper.play(allSongs, if (index >= 0) index else 0, mediaController) }
            }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context).data(songMeta.artworkUri?.toString()?.replace("size=128", "size=400")).crossfade(true).build(),
            contentDescription = "Album Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(2.dp)).border(1.dp, borderColor, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = songMeta.title?.toString() ?: "", style = MaterialTheme.typography.bodyLarge, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = buildString {
                    append("Song")
                    songMeta.artist?.toString()?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                },
                style = MaterialTheme.typography.bodyMedium, color = textColor.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}
