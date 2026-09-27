package ch.pissgrind.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import ch.pissgrind.ui.theme.LocalIsDark
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import ch.pissgrind.data.model.Screen
import ch.pissgrind.data.recommendation.ScoredAlbum
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen
import ch.pissgrind.ui.viewmodels.GenreBrowseViewModel
import ch.pissgrind.data.recommendation.CoverArtHelper.getCoverArtUrl
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import java.net.URLEncoder
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import ch.pissgrind.ui.theme.LocalLightBackground

@Composable
fun GenreBrowseScreen(
    navHostController: NavHostController,
    viewModel: GenreBrowseViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isDark = LocalIsDark.current
    val textColor = if (isDark) PissGreen else PissBlack
    val subtleColor = if (isDark) PissGreen.copy(alpha = 0.5f) else PissBlack.copy(alpha = 0.5f)
    val lightBg = LocalLightBackground.current
    val chipSelectedBg = if (isDark) PissGreen else PissBlack
    val chipSelectedText = if (isDark) PissBlack else lightBg
    val chipBg = if (isDark) PissBlack else lightBg
    val chipText = if (isDark) PissGreen else PissBlack
    val chipBorder = if (isDark) PissGreen.copy(alpha = 0.5f) else PissBlack.copy(alpha = 0.5f)

    val genresByCategory by viewModel.genresByCategory.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val albumsForGenre by viewModel.albumsForGenre.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

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
            IconButton(
                onClick = { navHostController.popBackStack() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Browse Genres",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }


        // Category Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            genresByCategory.keys.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { viewModel.selectCategory(category) },
                    label = { Text(category, maxLines = 1) },
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
                        selected = selectedCategory == category
                    ),
                    shape = RoundedCornerShape(2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Genre Chips within selected Category
        val genresInCategory = genresByCategory[selectedCategory] ?: emptyList()

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            genresInCategory.forEach { genre ->
                FilterChip(
                    selected = selectedGenre == genre,
                    onClick = { viewModel.selectGenre(genre) },
                    label = {
                        Text(
                            genre,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipSelectedBg,
                        selectedLabelColor = chipSelectedText,
                        containerColor = chipBg,
                        labelColor = chipText
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = chipBorder.copy(alpha = 1f),
                        selectedBorderColor = chipSelectedBg,
                        enabled = true,
                        selected = selectedGenre == genre
                    ),
                    shape = RoundedCornerShape(2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Album Grid
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = textColor)
            }
        } else if (selectedGenre != null) {
            if (albumsForGenre.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Albums found for $selectedGenre",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }
            } else {
                Text(
                    text = "${albumsForGenre.size} albums",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(albumsForGenre, key = { it.albumId }) { album ->
                        GenreAlbumCard(
                            album = album,
                            textColor = textColor,
                            subtleColor = textColor,
                            onClick = {
                                val encodedImage = URLEncoder.encode(album.getCoverArtUrl(500), "UTF-8")
                                navHostController.navigate(Screen.AlbumDetails.route + "/${album.albumId}/$encodedImage") {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Select a Genre to Browse Albums",
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtleColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun GenreAlbumCard(
    album: ScoredAlbum,
    textColor: androidx.compose.ui.graphics.Color,
    subtleColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(album.getCoverArtUrl())
                .crossfade(true)
                .build(),
            contentDescription = "Album Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(2.dp))
                .border(1.dp, textColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.labelSmall,
            color = subtleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
