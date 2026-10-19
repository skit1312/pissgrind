@file:OptIn(UnstableApi::class)

package ch.pissgrind.ui.playing

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import ch.pissgrind.managers.settings.AppearanceSettingsManager
import ch.pissgrind.player.PissgrindMediaLibraryService
import ch.pissgrind.ui.theme.PissBlack
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.gigamole.composefadingedges.marqueeHorizontalFadingEdges
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import ch.pissgrind.ui.theme.LocalIsDark
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import ch.pissgrind.R
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import ch.pissgrind.data.recommendation.TrainingModeManager
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import ch.pissgrind.ui.theme.LocalLightBackground
import ch.pissgrind.ui.theme.PissGreen

@kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Preview(
    showSystemUi = true, device = "id:pixel_9a",
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE, showBackground = true
)
@Preview(
    showSystemUi = true, device = "id:pixel",
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE, showBackground = true
)
@Composable
fun NowPlayingPortrait(
    mediaController: MediaController? = null,
    metadata: MediaMetadata? = null,
    iconColor: Color = Color.White,
    backgroundStyle: NowPlayingBackground = NowPlayingBackground.PISSGRIND,
    isTrainingMode: Boolean = false,
    onToggleQueue: () -> Unit = {},
    onBackgroundStyleChange: (NowPlayingBackground) -> Unit = {},
    onNavigateToAlbum: (albumId: String, imageUrl: String) -> Unit = { _, _ -> },
    onNavigateToArtist: (artistId: String, artistName: String) -> Unit = { _, _ -> },
    onTrainingThumbsUp: () -> Unit = {},
    onTrainingThumbsDown: () -> Unit = {},
    onStopTraining: () -> Unit = {},
    onAddToFavourites: () -> Unit = {},
    onRemoveFromFavourites: () -> Unit = {},
    onAddToLibrary: () -> Unit = {},
    onRemoveFromLibrary: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    isInLibrary: Boolean = false,
    isInFavourites: Boolean = false,
    currentRating: Boolean? = null,
    ) {
    val optionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iconTextColor by animateColorAsState(
        targetValue = iconColor,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "Animated Text Color"
    )
    val isDark = LocalIsDark.current
    val isPersistentTraining by TrainingModeManager.isPersistentTraining.collectAsState()
    val isTrainingPlaylist by TrainingModeManager.isTrainingPlaylist.collectAsState()
    val context = LocalContext.current
    val settingsManager = remember { AppearanceSettingsManager(context) }
    val titleAlignment by settingsManager.nowPlayingTitleAlignment.collectAsStateWithLifecycle(
        NowPlayingAlignment.LEFT
    )

    var showOptionsMenu by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
        ) {

            Spacer(modifier = Modifier.weight(0.2f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Album Art
                val player = PissgrindMediaLibraryService.getInstance()?.player
                val dragOffset = remember { Animatable(0f) }
                val coroutineScope = rememberCoroutineScope()

                Crossfade(
                    targetState = metadata?.artworkUri.toString()
                        .replace("size=128", "size=500"),
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = FastOutSlowInEasing
                    ),
                    label = "Crossfade between Albums",
                    modifier = Modifier.weight(1f, fill = false)
                ) { artworkUri ->
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(artworkUri)
                            .placeholderMemoryCacheKey(metadata?.artworkUri.toString())
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = "Album Cover Art",
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .aspectRatio(1f)
                            .graphicsLayer {
                                translationX = dragOffset.value
                            }
                            .pointerInput(Unit) {
                                var totalDrag = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { totalDrag = 0f },
                                    onDragEnd = {
                                        if (totalDrag < -100f && player?.hasNextMediaItem() == true) {
                                            player.seekToNextMediaItem()
                                        } else if (totalDrag > 100f) {
                                            player?.seekToPreviousMediaItem()
                                        }
                                        coroutineScope.launch {
                                            dragOffset.animateTo(0f)
                                        }
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        totalDrag += dragAmount
                                        coroutineScope.launch {
                                            dragOffset.snapTo(totalDrag)
                                        }
                                    }
                                )
                            }
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(0.dp)
                            )
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clip(RoundedCornerShape(0.dp))
                    )
                }

                val titleOutlineColor =
                    if (backgroundStyle == NowPlayingBackground.PISSGRIND) Color(0xFF70DD00) else Color.Black
                val titleFillColor =
                    if (backgroundStyle == NowPlayingBackground.PISSGRIND) Color.Black else Color.White

                // Song Title
                Crossfade(
                    targetState = metadata?.title.toString(),
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = FastOutSlowInEasing
                    ),
                    label = "Animated Song Title"
                ) { title ->
                    var isPressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.95f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "title press scale"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isPressed = true
                                        tryAwaitRelease()
                                        isPressed = false
                                    },
                                    onTap = {
                                        val albumId = metadata?.extras?.getString("albumId")
                                            ?: return@detectTapGestures
                                        val imageUrl = metadata.artworkUri?.toString() ?: ""
                                        onNavigateToAlbum(albumId, imageUrl)
                                    }
                                )
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        // Outline layer
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMediumEmphasized.copy(
                                drawStyle = Stroke(width = 12f, join = StrokeJoin.Round)
                            ),
                            fontWeight = FontWeight.Bold,
                            color = PissBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            softWrap = false,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .marqueeHorizontalFadingEdges(marqueeProvider = { Modifier.basicMarquee() })
                        )
                        // Fill layer
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMediumEmphasized,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF70DD00) else Color(0XFFFFFFFF),
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            softWrap = false,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .marqueeHorizontalFadingEdges(marqueeProvider = { Modifier.basicMarquee() })
                        )
                    }
                }

                // Artist Outline -> only for PISSGRIND
                val useArtistOutline = backgroundStyle == NowPlayingBackground.PISSGRIND
                val artistFillColor =
                    if (backgroundStyle == NowPlayingBackground.PISSGRIND) Color.Black else Color.White

                // Artist
                Crossfade(
                    targetState = metadata?.artist.toString(),
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = FastOutSlowInEasing
                    ),
                    label = "Animated Artist"
                ) { artistInfo ->
                    var isPressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.95f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "artist press scale"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isPressed = true
                                        tryAwaitRelease()
                                        isPressed = false
                                    },
                                    onTap = {
                                        val artistId = metadata?.extras?.getString("artistId")
                                            ?: return@detectTapGestures
                                        val artistName = metadata.artist?.toString() ?: ""
                                        onNavigateToArtist(artistId, artistName)
                                    }
                                )
                            }
                            .padding(vertical = 6.dp)
                    ) {
                        // Outline Layer(only PISSGRIND)
                        if (useArtistOutline) {
                            Text(
                                text = artistInfo,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    drawStyle = Stroke(width = 10f, join = StrokeJoin.Round)
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = PissBlack,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .marqueeHorizontalFadingEdges(marqueeProvider = { Modifier.basicMarquee() })
                            )
                        }
                        // Fill layer
                        Text(
                            text = artistInfo,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF70DD00) else Color(0XFFFFFFFF),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .marqueeHorizontalFadingEdges(marqueeProvider = { Modifier.basicMarquee() })
                        )
                    }
                }

                // Progress Slider
                Box(Modifier.padding(horizontal = 24.dp)) {
                    PlaybackProgressSlider(iconTextColor, mediaController)
                }
            }
            Spacer(Modifier.size(12.dp))

            // ===== PLAYBACK CONTROLS =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PissgrindMediaLibraryService.getInstance()?.player?.let { player ->
                    ShuffleButton(player, iconTextColor, Modifier.size(24.dp))
                    PreviousSongButton(player, iconTextColor, Modifier.size(40.dp))
                    PlayPauseButton(player, iconTextColor, Modifier.size(88.dp))
                    NextSongButton(player, iconTextColor, Modifier.size(40.dp))
                    RepeatButton(player, iconTextColor, Modifier.size(24.dp))
                }
            }

            // Training Buttons
            AnimatedVisibility(
                visible = isTrainingMode,
                enter = expandVertically(
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Top
                ) + fadeIn(animationSpec = tween(350)),
                exit = shrinkVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(animationSpec = tween(200))
            ) {
                TrainingButtons(
                    isVisible = true,
                    currentSongId = metadata?.extras?.getString("navidromeID") ?: "",
                    currentRating = currentRating,
                    color = iconTextColor,
                    onThumbsUp = onTrainingThumbsUp,
                    onThumbsDown = onTrainingThumbsDown,
                    onStopTraining = onStopTraining
                )
            }

            // Training Toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                TrainingChip(
                    enabled = isPersistentTraining || isTrainingPlaylist,
                    onToggle = { on ->
                        TrainingModeManager.savePersistentTraining(context, on)
                        if (!on) TrainingModeManager.stopTrainingPlaylist()
                    }
                )
            }

            // Swipe-Up Detection Area
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -20f) {
                                showOptionsMenu = true
                            }
                        }
                    }
            )

            // Spacer -> System Gesture Bar
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ===== OPTIONS BOTTOM SHEET =====
        if (showOptionsMenu) {
            ModalBottomSheet(
                onDismissRequest = { showOptionsMenu = false },
                sheetState = optionsSheetState,
                containerColor = PissBlack,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .width(32.dp)
                            .height(4.dp)
                            .background(
                                iconTextColor.copy(alpha = 0.5f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            ) {
                NowPlayingOptionsSheet(
                    iconColor = iconTextColor,
                    isInLibrary = isInLibrary,
                    isInFavourites = isInFavourites,
                    onAddToFavourites = {
                        onAddToFavourites()
                        showOptionsMenu = true
                    },
                    onRemoveFromFavourites = {
                        onRemoveFromFavourites()
                        showOptionsMenu = true
                    },
                    onAddToPlaylist = {
                        onAddToPlaylist()
                        showOptionsMenu = true
                    },
                    onAddToLibrary = {
                        onAddToLibrary()
                        showOptionsMenu = true
                    },
                    onRemoveFromLibrary = {
                        onRemoveFromLibrary()
                        showOptionsMenu = true
                    },
                    onToggleQueue = {
                        onToggleQueue()
                        showOptionsMenu = false
                    }
                )
            }
        }
    }
}
    @Composable
    private fun TrainingChip(
        enabled: Boolean,
        onToggle: (Boolean) -> Unit
    ) {
        val isDark = LocalIsDark.current
        val lightBg = LocalLightBackground.current
        val accent = if (isDark) PissGreen else PissBlack
        val surfaceCol = if (isDark) PissBlack else lightBg

        FilterChip(
            selected = enabled,
            onClick = { onToggle(!enabled) },
            label = {
                Text(
                    text = if (enabled) "Training: ON" else "Training: OFF",
                    fontWeight = FontWeight.Bold
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.power_light),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            shape = RoundedCornerShape(2.dp),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = surfaceCol,
                labelColor = accent,
                iconColor = accent,
                selectedContainerColor = accent,
                selectedLabelColor = surfaceCol,
                selectedLeadingIconColor = surfaceCol
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = enabled,
                borderColor = accent,
                selectedBorderColor = accent
            )
        )
    }

@Composable
private fun NowPlayingOptionsSheet(
    iconColor: Color,
    isInLibrary: Boolean,
    isInFavourites: Boolean,
    onAddToFavourites: () -> Unit,
    onRemoveFromFavourites: () -> Unit,
    onAddToLibrary: () -> Unit,
    onRemoveFromLibrary: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleQueue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.25.dp)
    ) {
        NowPlayingSheetItem(
            icon = if (isInFavourites) R.drawable.heart_straight_fill else R.drawable.heart_straight_light,
            label = if (isInFavourites) "Remove from Favourites" else "Add to Favourites",
            color = iconColor,
            onClick = if (isInFavourites) onRemoveFromFavourites else onAddToFavourites
        )

        NowPlayingSheetItem(
            icon = R.drawable.plus_light,
            label = "Add to Playlist",
            color = iconColor,
            onClick = onAddToPlaylist
        )

        NowPlayingSheetItem(
            icon = if (isInLibrary) R.drawable.minus_circle_fill else R.drawable.plus_circle_light,
            label = if (isInLibrary) "Remove from Library" else "Add to Library",
            color = iconColor,
            onClick = if (isInLibrary) onRemoveFromLibrary else onAddToLibrary
        )

        NowPlayingSheetItem(
            icon = R.drawable.queue_light,
            label = "Queue",
            color = iconColor,
            onClick = onToggleQueue
        )
    }
}

@Composable
private fun NowPlayingSheetItem(
    icon: Int,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

