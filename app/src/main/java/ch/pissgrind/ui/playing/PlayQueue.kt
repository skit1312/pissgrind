package ch.pissgrind.ui.playing

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import compose.icons.FeatherIcons
import compose.icons.feathericons.*

private data class QueueEntry(
    val uid: String,
    val mediaItem: MediaItem
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayQueueContent(
    mediaController: MediaController?,
    modifier: Modifier = Modifier
) {
    if (mediaController == null)
        return
    val currentList = remember { mutableStateListOf<QueueEntry>() }

    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var dragCurrentIndex by remember { mutableIntStateOf(-1) }

    var currentIndex by remember { mutableIntStateOf(mediaController.currentMediaItemIndex) }

    val haptic = LocalHapticFeedback.current

    DisposableEffect(mediaController) {
        fun syncList() {
            val timeline = mediaController.currentTimeline
            val window = Timeline.Window()
            val newEntries = ArrayList<QueueEntry>(timeline.windowCount)
            val seen = HashMap<String, Int>()
            for (i in 0 until timeline.windowCount) {
                timeline.getWindow(i, window)
                val base = window.mediaItem.mediaId
                val n = seen.getOrDefault(base, 0)
                seen[base] = n + 1
                newEntries.add(
                    QueueEntry(uid = "$base#$n", mediaItem = window.mediaItem)
                )
            }
            currentList.clear()
            currentList.addAll(newEntries)
        }

        val listener = object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                syncList()
                currentIndex = mediaController.currentMediaItemIndex
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentIndex = mediaController.currentMediaItemIndex
            }
        }

        // Initial load
        syncList()
        currentIndex = mediaController.currentMediaItemIndex
        mediaController.addListener(listener)

        onDispose { mediaController.removeListener(listener) }
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val idx = mediaController.currentMediaItemIndex
        if (idx in currentList.indices) {
            lazyListState.scrollToItem(idx)
        }
    }

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (dragStartIndex == -1) dragStartIndex = from.index
        currentList.add(to.index, currentList.removeAt(from.index))
        dragCurrentIndex = to.index
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(currentList, key = { _, entry -> entry.uid }) { index, entry ->
            val item = entry.mediaItem
            ReorderableItem(
                state = reorderableState,
                key = entry.uid,
                animateItemModifier = Modifier.animateItem(
                    placementSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
                )
            ) { draggingThis ->
                val elevation by animateDpAsState(
                    targetValue = if (draggingThis) 6.dp else 0.dp,
                    label = "queue_item_elevation"
                )
                val isCurrentItem = index == currentIndex

                Surface(
                    tonalElevation = elevation,
                    shadowElevation = elevation,
                    color = when {
                        draggingThis -> MaterialTheme.colorScheme.surfaceContainerHighest
                        isCurrentItem -> MaterialTheme.colorScheme.secondaryContainer
                        else -> BottomSheetDefaults.ContainerColor
                    },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().clickable {
                        mediaController.seekTo(index, 0)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCurrentItem && !draggingThis) {
                                Icon(
                                    imageVector = FeatherIcons.Play,
                                    contentDescription = "Now playing",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Title + artist
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = item.mediaMetadata.title?.toString() ?: "Unknown",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCurrentItem) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            item.mediaMetadata.artist?.toString()?.let { artist ->
                                Text(
                                    text = artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = (
                                            if (isCurrentItem) MaterialTheme.colorScheme.onSecondaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                            ).copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Drag handle
                        Icon(
                            imageVector = FeatherIcons.Menu,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(24.dp)
                                .draggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = {
                                        if (dragStartIndex != -1 && dragCurrentIndex != -1 &&
                                            dragStartIndex != dragCurrentIndex
                                        ) {
                                            mediaController.moveMediaItem(dragStartIndex, dragCurrentIndex)
                                        }
                                        dragStartIndex = -1
                                        dragCurrentIndex = -1
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}
