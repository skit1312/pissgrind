package ch.pissgrind.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import ch.pissgrind.R
import ch.pissgrind.ui.theme.PissWhite

private enum class SwipeDirection { NONE, LEFT, RIGHT, UP }
private enum class QuickSaveState { HIDDEN, SHOWING }

@Composable
fun QuickTrainScreen(
    mediaController: MediaController?,
    playlist: List<MediaItem>,
    onExit: () -> Unit,
    onThumbsUp: (MediaMetadata) -> Unit,
    onThumbsDown: (MediaMetadata) -> Unit,
    onAddToLibrary: (type: String, id: String) -> Unit,
    onRemoveFromLibrary: (type: String, id: String) -> Unit = { _, _ -> },
) {
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    var currentIndex by remember { mutableIntStateOf(0) }
    var quickSaveState by remember { mutableStateOf(QuickSaveState.HIDDEN) }

    // Swipe Animation
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    var swipeDirection by remember { mutableStateOf(SwipeDirection.NONE) }

    // Like/Dislike overlay alpha
    val likeAlpha by remember {
        derivedStateOf { (offsetX.value / screenWidthPx).coerceIn(0f, 0.4f) }
    }
    val dislikeAlpha by remember {
        derivedStateOf { (-offsetX.value / screenWidthPx).coerceIn(0f, 0.4f) }
    }

    val currentItem = playlist.getOrNull(currentIndex)
    val metadata = currentItem?.mediaMetadata

    // ===== COLLECTION & FAVOURITES STATE =====
    val collectionViewModel: CollectionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val navidromeId = metadata?.extras?.getString("navidromeID") ?: ""
    val albumId = metadata?.extras?.getString("albumId") ?: ""
    val artistId = metadata?.extras?.getString("artistId") ?: ""

    // Refresh trigger increments to force re-collection of flows
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val isSongInLibrary by remember(navidromeId, refreshTrigger) {
        collectionViewModel.isInCollection(navidromeId)
    }.collectAsState(initial = false)

    val isAlbumInLibrary by remember(albumId, refreshTrigger) {
        collectionViewModel.isInCollection(albumId)
    }.collectAsState(initial = false)

    val isArtistInLibrary by remember(artistId, refreshTrigger) {
        collectionViewModel.isInCollection(artistId)
    }.collectAsState(initial = false)

    val isSongInFavourites by remember(navidromeId, refreshTrigger) {
        collectionViewModel.isSongInFavourites(navidromeId)
    }.collectAsState(initial = false)

    // Start song at correct position when index changes
    LaunchedEffect(currentIndex) {
        if (playlist.isEmpty()) return@LaunchedEffect
        val item = playlist.getOrNull(currentIndex) ?: return@LaunchedEffect

        mediaController?.apply {
            setMediaItem(item)
            prepare()
            delay(300)
            val durationMs = duration
            if (durationMs > 0) {
                val startPercent = if (durationMs < 30_000) {
                    (15 + (Math.random() * 5)).toFloat() / 100f
                } else {
                    (30 + (Math.random() * 30)).toFloat() / 100f
                }
                seekTo((durationMs * startPercent).toLong())
            }
            play()
        }
    }

    // Entry Animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 3 },
        exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it / 3 }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PissBlack)
        ) {
            // Blurred Background Cover Art
            metadata?.artworkUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(25.dp)
                        .graphicsLayer { alpha = 0.2f }
                )
            }

            // Dark scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PissBlack.copy(alpha = 0.4f))
            )

            // Like Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PissGreen.copy(alpha = likeAlpha))
            )

            // Dislike Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = dislikeAlpha))
            )

            // Main Content: Swipeable Card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 32.dp, end = 32.dp, top = 64.dp, bottom = 200.dp)
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .graphicsLayer {
                        rotationZ = (offsetX.value / screenWidthPx) * 15f
                    }
                    .pointerInput(currentIndex, quickSaveState) {
                        if (quickSaveState == QuickSaveState.SHOWING) return@pointerInput

                        var totalDragY = 0f

                        detectDragGestures(
                            onDragStart = { totalDragY = 0f },
                            onDragEnd = {
                                coroutineScope.launch {
                                    when {
                                        // Swipe Right = Like
                                        offsetX.value > screenWidthPx * 0.3f -> {
                                            offsetX.animateTo(screenWidthPx, tween(200))
                                            metadata?.let { onThumbsUp(it) }
                                            advanceToNext(
                                                currentIndex, playlist.size,
                                                onIndexChange = { currentIndex = it },
                                                onEnd = onExit
                                            )
                                            offsetX.snapTo(0f)
                                        }
                                        // Swipe Left = Dislike
                                        offsetX.value < -screenWidthPx * 0.3f -> {
                                            offsetX.animateTo(-screenWidthPx, tween(200))
                                            metadata?.let { onThumbsDown(it) }
                                            advanceToNext(
                                                currentIndex, playlist.size,
                                                onIndexChange = { currentIndex = it },
                                                onEnd = onExit
                                            )
                                            offsetX.snapTo(0f)
                                        }
                                        // Swipe Up = Quick Save Menu
                                        totalDragY < -screenHeightPx * 0.1f -> {
                                            quickSaveState = QuickSaveState.SHOWING
                                            offsetX.snapTo(0f)
                                        }
                                        // Snap Back
                                        else -> {
                                            offsetX.animateTo(
                                                0f,
                                                spring(Spring.DampingRatioMediumBouncy)
                                            )
                                        }
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragY += dragAmount.y
                                coroutineScope.launch {
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Cover Art Card
                    metadata?.artworkUri?.let { uri ->
                        AsyncImage(
                            model = uri.toString().replace("size=128", "size=500"),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(280.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .shadow(16.dp, RoundedCornerShape(2.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Song Title
                    Text(
                        text = metadata?.title?.toString() ?: "",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Artist/Album
                    Text(
                        text = buildString {
                            append(metadata?.artist?.toString() ?: "")
                            metadata?.albumTitle?.let {
                                if (it.isNotBlank()) append(" - $it")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Swipe direction indicators
            if (offsetX.value > 50f) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.thumbs_up_light),
                    contentDescription = null,
                    tint = PissGreen.copy(alpha = (offsetX.value / screenWidthPx).coerceIn(0f, 1f)),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 24.dp)
                        .size(48.dp)
                )
            }
            if (offsetX.value < -50f) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.thumbs_down_light),
                    contentDescription = null,
                    tint = Color.Red.copy(alpha = (-offsetX.value / screenWidthPx).coerceIn(0f, 1f)),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                        .size(48.dp)
                )
            }

            // ===== PLAYBACK CONTROLS (upper area of bottom third) =====
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 160.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rewind 5s
                var rewindAnimating by remember { mutableStateOf(false) }
                val rewindAngle by animateFloatAsState(
                    targetValue = if (rewindAnimating) -45f else 0f,
                    animationSpec = tween(150),
                    finishedListener = { if (rewindAnimating) rewindAnimating = false },
                    label = "rewindAngle"
                )

                IconButton(
                    onClick = {
                        rewindAnimating = true
                        mediaController?.let {
                            val newPos = (it.currentPosition - 5000).coerceAtLeast(0)
                            it.seekTo(newPos)
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer { rotationZ = rewindAngle }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.arrow_counter_clockwise_light),
                        contentDescription = "Rewind 5s",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Skip
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            advanceToNext(
                                currentIndex, playlist.size,
                                onIndexChange = { currentIndex = it },
                                onEnd = onExit
                            )
                        }
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(2.dp)
                        )
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.skip_forward_light),
                        contentDescription = "Skip",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Forward 5s
                var forwardAnimating by remember { mutableStateOf(false) }
                val forwardAngle by animateFloatAsState(
                    targetValue = if (forwardAnimating) 45f else 0f,
                    animationSpec = tween(150),
                    finishedListener = { if (forwardAnimating) forwardAnimating = false },
                    label = "forwardAngle"
                )

                IconButton(
                    onClick = {
                        forwardAnimating = true
                        mediaController?.let {
                            val newPos = (it.currentPosition + 5000).coerceAtMost(it.duration)
                            it.seekTo(newPos)
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer { rotationZ = forwardAngle }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.arrow_clockwise_light),
                        contentDescription = "Forward 5s",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // ===== SWIPE UP HINT + CLOSE BUTTON =====
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Swipe Up Hint
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.hand_deposit_light),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Swipe Up to Save",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.3f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Close/Exit Button
                IconButton(
                    onClick = {
                        mediaController?.stop()
                        onExit()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(2.dp)
                        )
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.x_light),
                        contentDescription = "Exit",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // ===== QUICK SAVE OVERLAY =====
            AnimatedVisibility(
                visible = quickSaveState == QuickSaveState.SHOWING,
                enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                QuickSaveSheet(
                    metadata = metadata,
                    isSongInLibrary = isSongInLibrary,
                    isAlbumInLibrary = isAlbumInLibrary,
                    isArtistInLibrary = isArtistInLibrary,
                    isSongInFavourites = isSongInFavourites,
                    onAddArtist = {
                        if (artistId.isNotBlank()) onAddToLibrary("ARTIST", artistId)
                        refreshTrigger++
                    },
                    onRemoveArtist = {
                        if (artistId.isNotBlank()) onRemoveFromLibrary("ARTIST", artistId)
                        refreshTrigger++
                    },
                    onAddAlbum = {
                        if (albumId.isNotBlank()) onAddToLibrary("ALBUM", albumId)
                        refreshTrigger++
                    },
                    onRemoveAlbum = {
                        if (albumId.isNotBlank()) onRemoveFromLibrary("ALBUM", albumId)
                        refreshTrigger++
                    },
                    onAddSong = {
                        if (navidromeId.isNotBlank()) onAddToLibrary("SONG", navidromeId)
                        refreshTrigger++
                    },
                    onRemoveSong = {
                        if (navidromeId.isNotBlank()) onRemoveFromLibrary("SONG", navidromeId)
                        refreshTrigger++
                    },
                    onAddToFavourites = {
                        if (navidromeId.isNotBlank()) {
                            collectionViewModel.addSongToFavourites(
                                mediaController?.currentMediaItem ?: return@QuickSaveSheet
                            )
                        }
                        refreshTrigger++
                    },
                    onRemoveFromFavourites = {
                        if (navidromeId.isNotBlank()) {
                            collectionViewModel.removeSongFromFavourites(
                                mediaController?.currentMediaItem ?: return@QuickSaveSheet
                            )
                        }
                        refreshTrigger++
                    },
                    onDismiss = { quickSaveState = QuickSaveState.HIDDEN }
                )
            }
        }
    }
}


@Composable
private fun QuickSaveSheet(
    metadata: MediaMetadata?,
    isSongInLibrary: Boolean,
    isAlbumInLibrary: Boolean,
    isArtistInLibrary: Boolean,
    isSongInFavourites: Boolean,
    onAddArtist: () -> Unit,
    onRemoveArtist: () -> Unit,
    onAddAlbum: () -> Unit,
    onRemoveAlbum: () -> Unit,
    onAddSong: () -> Unit,
    onRemoveSong: () -> Unit,
    onAddToFavourites: () -> Unit,
    onRemoveFromFavourites: () -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.45f)
            .background(
                PissBlack.copy(alpha = 1f),
                RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (dragAmount.y > 50f) {
                        coroutineScope.launch { onDismiss() }
                    }
                }
            }
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Handle Bar
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .background(PissWhite, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Save",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PissWhite
            )

            // Song
            QuickSaveRow(
                label = metadata?.title?.toString() ?: "Unknown Song",
                sublabel = "Song",
                isInLibrary = isSongInLibrary,
                isInFavourites = isSongInFavourites,
                showFavourites = true,
                onAddToLibrary = onAddSong,
                onRemoveFromLibrary = onRemoveSong,
                onAddToFavourites = onAddToFavourites,
                onRemoveFromFavourites = onRemoveFromFavourites
            )

            // Album
            QuickSaveRow(
                label = metadata?.albumTitle?.toString() ?: "Unknown Album",
                sublabel = "Album",
                isInLibrary = isAlbumInLibrary,
                onAddToLibrary = onAddAlbum,
                onRemoveFromLibrary = onRemoveAlbum
            )

            // Artist
            QuickSaveRow(
                label = metadata?.artist?.toString() ?: "Unknown Artist",
                sublabel = "Artist",
                isInLibrary = isArtistInLibrary,
                onAddToLibrary = onAddArtist,
                onRemoveFromLibrary = onRemoveArtist
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}


@Composable
private fun QuickSaveRow(
    label: String,
    sublabel: String,
    onAddToLibrary: () -> Unit,
    onRemoveFromLibrary: () -> Unit,
    onAddToFavourites: () -> Unit = {},
    onRemoveFromFavourites: () -> Unit = {},
    isInLibrary: Boolean = false,
    isInFavourites: Boolean = false,
    showFavourites: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isInLibrary || isInFavourites) PissGreen.copy(alpha = 0.1f)
                else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(2.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        // Add/Remove Favourites
        if (showFavourites) {
            IconButton(
                onClick = {
                    if (isInFavourites) onRemoveFromFavourites() else onAddToFavourites()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        if (isInFavourites) R.drawable.heart_straight_fill
                        else R.drawable.heart_straight_light
                    ),
                    contentDescription = if (isInFavourites) "Remove from Favourites" else "Add to Favourites",
                    tint = if (isInFavourites) PissGreen else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Add/Remove Library
        IconButton(
            onClick = {
                if (isInLibrary) onRemoveFromLibrary() else onAddToLibrary()
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(
                    if (isInLibrary) R.drawable.minus_circle_fill
                    else R.drawable.plus_circle_light
                ),
                contentDescription = if (isInLibrary) "Remove from Library" else "Add to Library",
                tint = if (isInLibrary) PissGreen else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun advanceToNext(
    currentIndex: Int,
    playlistSize: Int,
    onIndexChange: (Int) -> Unit,
    onEnd: () -> Unit
) {
    if (currentIndex + 1 < playlistSize) {
        onIndexChange(currentIndex + 1)
    } else {
        onEnd()
    }
}
