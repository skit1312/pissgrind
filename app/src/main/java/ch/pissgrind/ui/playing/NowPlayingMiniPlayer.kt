package ch.pissgrind.ui.playing

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.material3.HorizontalDivider
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen
import androidx.media3.session.MediaController
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import ch.pissgrind.player.ChoraMediaLibraryService
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import ch.pissgrind.ui.theme.LocalIsDark
import ch.pissgrind.ui.theme.LocalLightBackground
import ch.pissgrind.ui.theme.PissWhite
import kotlinx.coroutines.delay



@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Stable
@Composable
fun NowPlayingMiniPlayer(
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    metadata: MediaMetadata? = null,
    mediaController: MediaController? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onClick: () -> Unit = { }
) {
    val isDark = LocalIsDark.current
    val expanded by remember { derivedStateOf { scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded } }
    val lightBg = LocalLightBackground.current
    val yTrans by animateIntAsState(
        targetValue = if (expanded) dpToPx(72) else 0,
        label = "Fullscreen Translation"
    )
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            val player = ChoraMediaLibraryService.getInstance()?.player
            val duration = player?.duration ?: 0L
            val position = player?.currentPosition ?: 0L
            progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
            delay(100L)
        }
    }

    Column(
        modifier = Modifier
            .offset { IntOffset(x = 0, y = -yTrans) }
            .zIndex(1f)
            .clipToBounds()
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = if (isDark) PissGreen else PissBlack,
            trackColor = if (isDark) PissBlack else lightBg,
            strokeCap = StrokeCap.Round
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(if (isDark) PissBlack else lightBg)
                .height(70.dp)
                .fillMaxWidth()
                .padding(start = 6.dp, end = 14.dp)
                .clickable { onClick.invoke() }
        ) {
            // Album Image
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(metadata?.artworkUri)
                    .diskCacheKey(
                        metadata?.extras?.getString("navidromeID")
                    )
                    .crossfade(true)
                    .build(),
                contentDescription = "Album Cover",
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(2.dp))
            )

            // Title + Artist
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .weight(1f)
            ) {
                Text(
                    text = metadata?.title.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDark) PissGreen else PissBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.width(IntrinsicSize.Max)) {
                    Text(
                        text = metadata?.artist.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Light,
                        color = if (isDark) PissGreen else PissBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                    if (metadata?.recordingYear != 0 && metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION) {
                        Text(
                            text = " • " + metadata?.recordingYear.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Light,
                            color = if (isDark) PissGreen else PissBlack,
                            maxLines = 1,
                            modifier = Modifier.wrapContentWidth()
                        )
                    }
                }
            }

            ChoraMediaLibraryService.getInstance()?.player?.let {
                PlayPauseButton(
                    it,
                    if (isDark) PissGreen else PissBlack,
                    Modifier.size(36.dp)

                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = if (isDark) PissGreen else PissBlack
        )
    }
}

