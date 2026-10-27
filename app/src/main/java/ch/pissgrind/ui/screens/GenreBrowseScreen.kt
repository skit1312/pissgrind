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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

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

        var expandedCategory by remember { mutableStateOf(viewModel.initialCategory) }

        // ===== Maingenres ====
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(genresByCategory.keys.toList(), key = { "cat_$it" }) { category ->
                CategoryChip(
                    label = category,
                    selected = selectedCategory == category,
                    expanded = expandedCategory == category,
                    hasSubgenres = !genresByCategory[category].isNullOrEmpty(),
                    accent = chipSelectedBg,
                    surfaceCol = chipBg,
                    borderColor = chipBorder,
                    onSelect = { viewModel.selectCategory(category) },
                    onToggleExpand = {
                        expandedCategory = if (expandedCategory == category) null else category
                        if (expandedCategory == category) viewModel.selectCategory(category)
                    }
                )
            }
        }

        // ==== Subgenres ====
        val genresInCategory = genresByCategory[expandedCategory] ?: emptyList()

        AnimatedVisibility(
            visible = expandedCategory != null && genresInCategory.isNotEmpty(),
            enter = expandVertically(tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(250)),
            exit = shrinkVertically(tween(200, easing = FastOutSlowInEasing)) + fadeOut(tween(150))
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                genresInCategory.forEach { genre ->
                    FilterChip(
                        selected = selectedGenre == genre,
                        onClick = { viewModel.selectGenre(genre) },
                        label = {
                            Text(genre, style = MaterialTheme.typography.bodySmall, maxLines = 1)
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
@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    expanded: Boolean,
    hasSubgenres: Boolean,
    accent: Color,
    surfaceCol: Color,
    borderColor: Color,
    onSelect: () -> Unit,
    onToggleExpand: () -> Unit
) {
    val bg by animateColorAsState(if (selected) accent else surfaceCol, tween(200), label = "chip bg")
    val fg by animateColorAsState(if (selected) surfaceCol else accent, tween(200), label = "chip fg")
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(250), label = "chevron")

    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(bg)
            .border(1.dp, if (selected) accent else borderColor, RoundedCornerShape(2.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Name: auswählen
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clickable { onSelect() }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = fg,
                maxLines = 1
            )
        }

        // Chevron: auf-/zuklappen
        if (hasSubgenres) {
            Box(Modifier.width(1.dp).fillMaxHeight().background(fg.copy(alpha = 0.4f)))
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = fg,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
        }
    }
}