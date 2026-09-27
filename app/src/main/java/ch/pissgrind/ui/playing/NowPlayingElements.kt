@file:androidx.annotation.OptIn(UnstableApi::class)

package ch.pissgrind.ui.playing

import android.util.Log
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.ui.compose.state.rememberNextButtonState
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPreviousButtonState
import androidx.media3.ui.compose.state.rememberRepeatButtonState
import androidx.media3.ui.compose.state.rememberShuffleButtonState
import ch.pissgrind.R
import ch.pissgrind.formatMilliseconds
import ch.pissgrind.providers.navidrome.downloadNavidromeSong
import ch.pissgrind.ui.elements.bounceClick
import ch.pissgrind.ui.elements.moveClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.hilt.navigation.compose.hiltViewModel
import ch.pissgrind.data.local.entity.CollectionItem
import ch.pissgrind.data.local.entity.CollectionItemType
import ch.pissgrind.ui.screens.CollectionViewModel
import kotlinx.coroutines.flow.flowOf
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import compose.icons.FeatherIcons
import compose.icons.feathericons.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.res.painterResource


@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun PlaybackProgressSlider(
    color: Color = Color.White,
    mediaController: MediaController? = null,
    metadata: MediaMetadata? = null
) {

    var currentValue by remember { mutableLongStateOf(0L) }
    val currentDuration by remember(mediaController?.duration) {
        derivedStateOf {
            mediaController?.duration?.coerceAtLeast(0L)
        }
    }

    val animatedValue by animateFloatAsState(
        targetValue = currentValue.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "Smooth Slider Update"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val focused = remember { mutableStateOf(false) }
    var isInteracting by remember { mutableStateOf(false) }

    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(mediaController, isPlaying) {
        if (mediaController != null && isPlaying) {
            while (isActive && !isInteracting) {
                currentValue = mediaController.currentPosition
                delay(1000L)
            }
        } else {
            if (mediaController != null) {
                currentValue = mediaController.currentPosition
            }
        }
    }

    DisposableEffect(mediaController) {
        if (mediaController == null) {
            onDispose { }
        }

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                Log.d("TAG", "MediaController isPlaying changed: $playing")
                isPlaying = playing
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                if (reason != Player.DISCONTINUITY_REASON_SEEK)
                    currentValue = newPosition.positionMs
            }
        }

        mediaController?.addListener(listener)

        // Initial check in case state changed before listener was attached or for initial setup
        isPlaying = mediaController?.isPlaying ?: false
        currentValue = mediaController?.currentPosition ?: 0L

        onDispose {
            mediaController?.removeListener(listener)
        }
    }

    Column(
        Modifier.focusable(false)
    ) {
        Slider(
            enabled = metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    focused.value = it.isFocused
                }
                .onKeyEvent { keyEvent ->
                    when {
                        keyEvent.key == Key.DirectionRight && keyEvent.type == KeyEventType.KeyDown -> {
                            currentValue = (currentValue + 5000)
                            mediaController?.seekTo(currentValue)
                            true
                        }
                        keyEvent.key == Key.DirectionLeft && keyEvent.type == KeyEventType.KeyDown -> {
                            currentValue = (currentValue - 5000)
                            mediaController?.seekTo(currentValue)
                            true
                        }
                        else -> false
                    }
                },
            value = animatedValue,
            onValueChange = {
                isInteracting = true
                currentValue = it.toLong()
            },
            onValueChangeFinished = {
                isInteracting = false
                mediaController?.seekTo(currentValue)
            },
            valueRange = 0f..(currentDuration?.toFloat() ?: 0f),
            interactionSource = interactionSource,
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(4.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = color,
                        inactiveTrackColor = color.copy(alpha = 0.25f),
                    ),
                    thumbTrackGapSize = 0.dp,
                    trackInsideCornerSize = 0.dp,
                    drawStopIndicator = null
                )
            },
            thumb = {
                val size by animateDpAsState(
                    targetValue = if (interactionSource.collectIsPressedAsState().value) 18.dp else 14.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "thumb size"
                )
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(RoundedCornerShape(50))
                        .background(color)
                )
            }
        )

        // Time thingies
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = remember(currentValue) { formatMilliseconds(currentValue.toInt() / 1000) },
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Start,
                color = color.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(64.dp),
                maxLines = 1
            )
            Text(
                text = remember(currentDuration) { formatMilliseconds(currentDuration?.toInt()?.div(1000) ?: (currentValue/1000).toInt()) },
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End,
                color = color.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(64.dp),
                maxLines = 1
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun PreviousSongButton(player: Player, color: Color, modifier: Modifier = Modifier) {
    val state = rememberPreviousButtonState(player)
    IconButton(onClick = state::onClick, modifier = modifier
        .bounceClick(state.isEnabled)
        .moveClick(false, state.isEnabled), enabled = state.isEnabled) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.skip_back_light), contentDescription = "Previous song", modifier = Modifier.size(38.dp), tint = if (state.isEnabled) color else color.copy(0.5f))
        }
    }


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PlayPauseButton(player: Player, color: Color, modifier: Modifier = Modifier) {
    val state = rememberPlayPauseButtonState(player)
    val icon = if (state.showPlay) R.drawable.play_light else R.drawable.pause_light
    val contentDescription =
        if (state.showPlay) "play"
        else "pause"

    IconButton(onClick = state::onClick, modifier = modifier.bounceClick(state.isEnabled), enabled = state.isEnabled) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = ImageVector.vectorResource(icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(68.dp),
                tint = if (state.isEnabled) color else color.copy(0.5f)
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun NextSongButton(player: Player, color: Color, modifier: Modifier = Modifier) {
    val state = rememberNextButtonState(player)
    IconButton(onClick = state::onClick, modifier = modifier
        .bounceClick(state.isEnabled)
        .moveClick(true, state.isEnabled), enabled = state.isEnabled) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.skip_forward_light), contentDescription = "Next song", modifier = Modifier.size(38.dp), tint = if (state.isEnabled) color else color.copy(0.5f))
    }
}

/*
@Composable
fun PlayQueueButton(
    color: Color = Color.Black,
    size: Dp = 64.dp,
    onClick: () -> Unit = {}
){
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(6.dp),
        modifier = Modifier.bounceClick().size(size + 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = color.copy(1f),
            disabledContentColor = color.copy(0.5f)
        )
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.queue_light),
            contentDescription = "Queue",
            modifier = Modifier.size(32.dp)
        )
    }
}
*/
/*
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun DownloadButton(color: Color, size: Dp, metadata: MediaMetadata?, enabled: Boolean) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Button(
        onClick = {
            coroutineScope.launch {
                metadata?.let {
                    downloadNavidromeSong(context, it)
                }
            }
        },
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        modifier = if (enabled)
            Modifier
                .bounceClick()
                .size(size + 12.dp)
        else
            Modifier
                .size(size + 12.dp),
        contentPadding = PaddingValues(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = color.copy(alpha = 1f),
            disabledContentColor = color.copy(alpha = 0.25f)
        )
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.download_simple_light),
            contentDescription = "Download",
            modifier = Modifier.size(32.dp)
        )
    }
}
*/
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun ShuffleButton(player: Player, color: Color, modifier: Modifier = Modifier) {
    val state = rememberShuffleButtonState(player)
    IconButton(
        onClick = state::onClick,
        modifier = modifier.bounceClick(),
        enabled = state.isEnabled
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.shuffle_angular_light),
            contentDescription = "Shuffle",
            modifier = Modifier.size(32.dp),
            tint = color.copy(if (state.shuffleOn) 1f else 0.75f)
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun RepeatButton(player: Player, color: Color, modifier: Modifier = Modifier) {
    val state = rememberRepeatButtonState(player)
    val icon = repeatModeIcon(state.repeatModeState)
    IconButton(
        onClick = state::onClick,
        modifier = modifier.bounceClick(),
        enabled = state.isEnabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Repeat",
            modifier = Modifier.size(22.dp),
            tint = color.copy(if (state.repeatModeState == Player.REPEAT_MODE_OFF) 0.75f else 1f)
        )
    }
}

@Composable
private fun repeatModeIcon(repeatMode: @Player.RepeatMode Int): ImageVector {
    return when (repeatMode) {
        Player.REPEAT_MODE_OFF -> ImageVector.vectorResource(R.drawable.repeat_light)
        Player.REPEAT_MODE_ONE -> ImageVector.vectorResource(R.drawable.repeat_once_light)
        else -> ImageVector.vectorResource(R.drawable.repeat_light)
    }
}
/*
@Composable
fun CollectionButton(
    color: Color,
    size: Dp,
    mediaController: MediaController? = null,
    metadata: MediaMetadata? = null,
    onMessage: (String) -> Unit = {}
) {
    val collectionViewModel: CollectionViewModel = hiltViewModel()
    val navidromeId = remember(metadata) {
        mediaController?.currentMediaItem?.mediaMetadata?.extras?.getString("navidromeID") ?: ""
    }

    val dbState by remember(navidromeId) {
        if (navidromeId.isEmpty()) flowOf(false)
        else collectionViewModel.isInCollection(navidromeId)
    }.collectAsStateWithLifecycle(initialValue = false)

    val inCollection = dbState

    Button(
        onClick = {
            val item = mediaController?.currentMediaItem ?: return@Button

            collectionViewModel.toggleInCollection(
                CollectionItem(
                    id = navidromeId,
                    type = CollectionItemType.SONG,
                    title = item.mediaMetadata.title.toString(),
                    subtitle = item.mediaMetadata.artist.toString(),
                    coverArtUrl = item.mediaMetadata.artworkUri.toString(),
                    streamUrl = item.mediaId.removeSuffix("&estimateContentLength=true"),
                    duration = item.mediaMetadata.extras?.getInt("duration"),
                    trackNumber = item.mediaMetadata.trackNumber,
                    year = item.mediaMetadata.recordingYear,
                    album = item.mediaMetadata.albumTitle.toString()
                )
            )

            onMessage(
                if (inCollection) "${item.mediaMetadata.title} removed from Library"
                else "${item.mediaMetadata.title} added to Library"
            )
        },
        shape = RoundedCornerShape(2.dp),
        modifier = Modifier.bounceClick().size(size + 12.dp),
        contentPadding = PaddingValues(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = if (inCollection) color else color.copy(1f)
        )
    ) {
        Icon(
            painter = if (inCollection)
                painterResource(R.drawable.heart_straight_fill)
            else
                painterResource(R.drawable.heart_straight_light),
            contentDescription = "Library",
            modifier = Modifier.size(28.dp)
        )
    }
}
*/
@Composable
@ReadOnlyComposable
fun dpToPx(dp: Int): Int {
    return (dp * LocalDensity.current.density).toInt()
}

@Composable
@ReadOnlyComposable
fun dpToPx(dp: Float): Int {
    return (dp * LocalDensity.current.density).toInt()
}


/*
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BackgroundStyleButton(
    color: Color,
    size: Dp,
    currentStyle: NowPlayingBackground,
    onStyleChange: (NowPlayingBackground) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    ImageVector.vectorResource(R.drawable.s_a_background),
                    contentDescription = "Background Style",
                    modifier = Modifier.size(28.dp),
                    tint = color
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = Color(0xFF000000),
                shape = RoundedCornerShape(2.dp)
            ) {
                NowPlayingBackground.entries.forEach { style ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = when (style) {
                                    NowPlayingBackground.BLACK -> "Black"
                                    NowPlayingBackground.PISSGRIND -> "PissGrind"
                                    NowPlayingBackground.GRAIN -> "Grain"
                                },
                                fontWeight = if (style == currentStyle) FontWeight.Bold else FontWeight.Normal,
                                color = Color.White
                            )
                        },
                        leadingIcon = {
                            if (style == currentStyle) {
                                Icon(FeatherIcons.Check, contentDescription = null, tint = color)
                            }
                        },
                        onClick = {
                            onStyleChange(style)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
 */