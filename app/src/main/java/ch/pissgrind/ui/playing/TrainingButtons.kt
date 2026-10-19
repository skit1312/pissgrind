package ch.pissgrind.ui.playing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ThumbsDown
import compose.icons.feathericons.ThumbsUp
import compose.icons.feathericons.X
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue


/**
 * Training mode buttons shown in NowPlaying when training mode is active.
 * Thumbs up = like the current album's genres
 * Thumbs down = dislike (and skip to next)
 */
@Composable
fun TrainingButtons(
    isVisible: Boolean,
    currentSongId: String = "",
    currentRating: Boolean? = null,
    color: Color = Color.White,
    onThumbsUp: () -> Unit,
    onThumbsDown: () -> Unit,
    onStopTraining: () -> Unit,
    modifier: Modifier = Modifier
) {
    var thumbsUpPressed by remember { mutableStateOf(false) }
    var thumbsDownPressed by remember { mutableStateOf(false) }

    LaunchedEffect(currentSongId, currentRating) {
        thumbsUpPressed = currentRating == true
        thumbsDownPressed = currentRating == false
    }

    val thumbsUpColor by animateColorAsState(
        targetValue = if (thumbsUpPressed) Color(0xFF70DD00) else color.copy(alpha = 0.8f),
        animationSpec = tween(300),
        label = "thumbs up color"
    )
    val thumbsDownColor by animateColorAsState(
        targetValue = if (thumbsDownPressed) Color(0xFFFF4444) else color.copy(alpha = 0.8f),
        animationSpec = tween(300),
        label = "thumbs down color"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it }
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbs Down
            IconButton(
                onClick = {
                    thumbsDownPressed = true
                    thumbsUpPressed = false
                    onThumbsDown()
                },
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, thumbsDownColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            ) {
                Icon(
                    imageVector = FeatherIcons.ThumbsDown,
                    contentDescription = "Dislike",
                    tint = thumbsDownColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Training label
            Text(
                text = "   //   ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Thumbs Up
            IconButton(
                onClick = {
                    thumbsUpPressed = true
                    thumbsDownPressed = false
                    onThumbsUp()
                },
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, thumbsUpColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            ) {
                Icon(
                    imageVector = FeatherIcons.ThumbsUp,
                    contentDescription = "Like",
                    tint = thumbsUpColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            }
        }
    }
