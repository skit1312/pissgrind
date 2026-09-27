package ch.pissgrind.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import ch.pissgrind.data.recommendation.CoverArtHelper.getCoverArtUrl
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import ch.pissgrind.data.model.Screen
import ch.pissgrind.data.recommendation.ScoredAlbum
import ch.pissgrind.player.SongHelper
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen
import ch.pissgrind.ui.theme.PissWhite
import ch.pissgrind.ui.viewmodels.HomeScreenViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import ch.pissgrind.R
import ch.pissgrind.ui.theme.LocalLightBackground
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest


enum class RecommendationFilter { ALL, BY_GENRE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    navHostController: NavHostController,
    mediaController: MediaController? = null,
    viewModel: HomeScreenViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = LocalIsDark.current
    val textColor = if (isDark) PissGreen else PissBlack
    val subtleColor = if (isDark) PissGreen.copy(alpha = 0.7f) else PissBlack.copy(alpha = 0.7f)
    val lightBg = LocalLightBackground.current
    val chipSelectedBg = if (isDark) PissGreen else PissBlack
    val chipSelectedText = if (isDark) PissBlack else lightBg
    val chipBg = if (isDark) PissBlack else lightBg
    val chipText = if (isDark) PissGreen else PissBlack
    val chipBorder = if (isDark) PissGreen.copy(alpha = 0.5f) else PissBlack.copy(alpha = 0.5f)
    val dialogBg = if (isDark) PissBlack else lightBg

    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val excludedIds by viewModel.excludedAlbumIds.collectAsStateWithLifecycle()
    val genreProfile by viewModel.genreProfile.collectAsStateWithLifecycle()
    val topGenre by viewModel.topGenre.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf(RecommendationFilter.ALL) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }

    // Exclude dialog state
    var albumToExclude by remember { mutableStateOf<ScoredAlbum?>(null) }

    val availableGenres = remember(recommendations) {
        recommendations
            .flatMap { it.genres }
            .map { it.replaceFirstChar { c -> c.uppercase() } }
            .distinct()
            .sortedBy { genre ->
                -(genreProfile?.getWeight(genre) ?: 0f)
            }
    }

    // Filter Recommendations
    val displayedAlbums = remember(recommendations, selectedFilter, selectedGenre, excludedIds) {
        val filtered = recommendations.filter { it.albumId !in excludedIds }
        when (selectedFilter) {
            RecommendationFilter.ALL -> filtered
            RecommendationFilter.BY_GENRE -> {
                if (selectedGenre != null) {
                    filtered.filter { album ->
                        album.genres.any { it.equals(selectedGenre, ignoreCase = true) }
                    }
                } else {
                    filtered
                }
            }
        }
    }

    // Exclude Confirmation Dialog
    albumToExclude?.let { album ->
        AlertDialog(
            onDismissRequest = { albumToExclude = null },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.excludeAlbum(album.albumId)
                    albumToExclude = null
                }) {
                    Text("Exclude", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { albumToExclude = null }) {
                    Text("Cancel", color = textColor)
                }
            },
            title = {
                Text("Exclude Album", color = textColor, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Exclude \"${album.name}\" by ${album.artist} from Recommendations?\n\nThis won't affect your Training Data.",
                    color = subtleColor
                )
            },
            containerColor = dialogBg,
            shape = RoundedCornerShape(2.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = { navHostController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor
                )
            }
            Text(
                text = "Recommended for You",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        // Genre Profile Summary
  /*      genreProfile?.let { profile ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val topGenres = profile.genreWeights.entries
                    .sortedByDescending { it.value }
                    .take(3)

                topGenres.forEach { (genre, weight) ->
                    GenreWeightChip(
                        genre = genre.replaceFirstChar { it.uppercase() },
                        weight = weight,
                        color = textColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
*/
        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == RecommendationFilter.ALL,
                onClick = {
                    selectedFilter = RecommendationFilter.ALL
                    selectedGenre = null
                },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chipSelectedBg,
                    selectedLabelColor = chipSelectedText,
                    containerColor = chipBg,
                    labelColor = chipText
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (isDark) PissGreen else PissBlack,
                    selectedBorderColor = chipSelectedBg,
                    enabled = true,
                    selected = selectedFilter == RecommendationFilter.ALL
                ),
                shape = RoundedCornerShape(2.dp)
            )
            FilterChip(
                selected = selectedFilter == RecommendationFilter.BY_GENRE,
                onClick = { selectedFilter = RecommendationFilter.BY_GENRE },
                label = { Text("By Genre") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chipSelectedBg,
                    selectedLabelColor = chipSelectedText,
                    containerColor = chipBg,
                    labelColor = chipText
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = chipBorder,
                    selectedBorderColor = chipSelectedBg,
                    enabled = true,
                    selected = selectedFilter == RecommendationFilter.BY_GENRE
                ),
                shape = RoundedCornerShape(2.dp)
            )
        }

        // Genre selector
        if (selectedFilter == RecommendationFilter.BY_GENRE) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(availableGenres) { genre ->
                    FilterChip(
                        selected = selectedGenre == genre,
                        onClick = {
                            selectedGenre = if (selectedGenre == genre) null else genre
                        },
                        label = { Text(genre, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipSelectedBg.copy(alpha = 0.8f),
                            selectedLabelColor = chipSelectedText,
                            containerColor = chipBg,
                            labelColor = chipText.copy(alpha = 0.7f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = chipBorder.copy(alpha = 0.3f),
                            selectedBorderColor = chipSelectedBg,
                            enabled = true,
                            selected = selectedGenre == genre
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${displayedAlbums.size} albums",
            style = MaterialTheme.typography.bodySmall,
            color = textColor.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Album List with Swipe
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = displayedAlbums,
                key = { "rec_detail_${it.albumId}" }
            ) { album ->
                SwipeableRecommendationItem(
                    album = album,
                    context = context,
                    textColor = textColor,
                    subtleColor = subtleColor,
                    onAlbumClick = {
                        val coverUrl = album.getCoverArtUrl(500)
                        val encodedImage = URLEncoder.encode(coverUrl, "UTF-8")
                        navHostController.navigate(Screen.AlbumDetails.route + "/${album.albumId}/$encodedImage") {
                            launchSingleTop = true
                        }
                    },
                    onPlayClick = {
                        coroutineScope.launch {
                            val songs = viewModel.getAlbumSongs(album.albumId)
                            if (songs.size > 1) {
                                SongHelper.play(
                                    mediaItems = songs.drop(1),
                                    index = 0,
                                    mediaController = mediaController
                                )
                            }
                        }
                    },
                    onExclude = { albumToExclude = album }
                )
            }

            //item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableRecommendationItem(
    album: ScoredAlbum,
    context: android.content.Context,
    textColor: Color,
    subtleColor: Color,
    onAlbumClick: () -> Unit,
    onPlayClick: () -> Unit,
    onExclude: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onExclude()
            }
            false
        },
        positionalThreshold = { it * 0.3f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val isSwiping = dismissState.targetValue != dismissState.currentValue ||
                    dismissState.targetValue == SwipeToDismissBoxValue.EndToStart

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isSwiping) Color.Red.copy(alpha = 0.15f) else Color.Transparent
                    )
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isSwiping) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Exclude",
                            color = Color.Red.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.x_light),
                            contentDescription = null,
                            tint = Color.Red.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    ) {
        RecommendationListItem(
            album = album,
            context = context,
            textColor = textColor,
            subtleColor = subtleColor,
            onAlbumClick = onAlbumClick,
            onPlayClick = onPlayClick
        )
    }
}

@Composable
private fun RecommendationListItem(
    album: ScoredAlbum,
    context: android.content.Context,
    textColor: Color,
    subtleColor: Color,
    onAlbumClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAlbumClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(album.getCoverArtUrl(300))
                .crossfade(true)
                .build(),
            contentDescription = "Album Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(2.dp))
                .border(1.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = album.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = subtleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                album.genres.take(3).forEach { genre ->
                    Text(
                        text = genre.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.4f),
                        modifier = Modifier
                            .border(1.dp, textColor.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                text = album.reason,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.35f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        IconButton(onClick = onPlayClick) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.play_fill),
                contentDescription = "Play",
                tint = textColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun GenreWeightChip(genre: String, weight: Float, color: Color) {
    val percentage = (weight * 100).toInt()
    Box(
        modifier = Modifier
            .border(1.dp, color.copy(alpha = weight.coerceIn(0.2f, 0.8f)), RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$genre ${percentage}%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color.copy(alpha = 0.8f)
        )
    }
}
