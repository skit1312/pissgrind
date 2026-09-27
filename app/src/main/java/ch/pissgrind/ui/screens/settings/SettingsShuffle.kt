package ch.pissgrind.ui.screens.settings

import ch.pissgrind.ui.theme.LocalIsDark
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import ch.pissgrind.managers.settings.AudioSettingsManager
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen
import ch.pissgrind.ui.theme.PissWhite
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun S_ShuffleScreen(
    navHostController: NavHostController,
    audioSettingsManager: AudioSettingsManager
) {
    val coroutineScope = rememberCoroutineScope()
    val shuffleRange by audioSettingsManager.shuffleRangeFlow.collectAsStateWithLifecycle(initialValue = 5000)
    val isDark = LocalIsDark.current

    // Theme colors
    val textColor = if (isDark) PissGreen else PissBlack
    val subtleColor = if (isDark) PissGreen.copy(alpha = 0.9f) else PissBlack.copy(alpha = 0.9f)
    val borderColor = if (isDark) PissGreen.copy(alpha = 0.9f) else PissBlack.copy(alpha = 0.9f)
    val dividerColor = if (isDark) PissGreen.copy(alpha = 0.9f) else PissBlack.copy(alpha = 0.9f)
    val sliderActive = if (isDark) PissGreen else PissBlack
    val sliderInactive = if (isDark) PissGreen.copy(alpha = 0.9f) else PissBlack.copy(alpha = 0.9f)

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
                text = "Shuffle",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Shuffle Pool Size",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Determines how many Songs the Randomizer picks from when creating Shuffle All, Train Playlists and Discovery Playlists. A higher Number gives more Variety but takes longer to load. A lower Number loads faster but may repeat Songs sooner.",
                style = MaterialTheme.typography.bodySmall,
                color = subtleColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Current value display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderColor, RoundedCornerShape(2.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (shuffleRange == AudioSettingsManager.SHUFFLE_RANGE_ALL) "All Songs" else "$shuffleRange songs",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Slider
            val sliderPosition = remember(shuffleRange) {
                if (shuffleRange == AudioSettingsManager.SHUFFLE_RANGE_ALL) 10f
                else ((shuffleRange - 1000) / 1000).toFloat().coerceIn(0f, 9f)
            }

            var currentSliderPosition by remember(sliderPosition) { mutableFloatStateOf(sliderPosition) }

            Slider(
                value = currentSliderPosition,
                onValueChange = { currentSliderPosition = it },
                onValueChangeFinished = {
                    val newRange = if (currentSliderPosition.roundToInt() >= 10) {
                        AudioSettingsManager.SHUFFLE_RANGE_ALL
                    } else {
                        (currentSliderPosition.roundToInt() * 1000) + 1000
                    }
                    coroutineScope.launch {
                        audioSettingsManager.setShuffleRange(newRange)
                    }
                },
                valueRange = 0f..10f,
                steps = 9,
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.height(6.dp),
                        colors = SliderDefaults.colors(
                            activeTrackColor = sliderActive,
                            inactiveTrackColor = sliderInactive.copy(alpha = 0.2f),
                            activeTickColor = sliderActive,
                            inactiveTickColor = sliderInactive.copy(alpha = 0.3f)
                        ),
                        thumbTrackGapSize = 0.dp,
                        trackInsideCornerSize = 2.dp,
                        drawStopIndicator = null
                    )
                },
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        colors = SliderDefaults.colors(thumbColor = sliderActive),
                        thumbSize = DpSize(16.dp, 16.dp)
                    )
                }
            )

            // Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "1000",
                    style = MaterialTheme.typography.labelSmall,
                    color = subtleColor
                )
                Text(
                    text = "All",
                    style = MaterialTheme.typography.labelSmall,
                    color = subtleColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = dividerColor)
            Spacer(modifier = Modifier.height(16.dp))

            // Info box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderColor, RoundedCornerShape(2.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "How it Works",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = subtleColor
                )
                Text(
                    text = "• Lower Range (1000–3000): Fast Loading, relatively low Variety. Good for slow Connections.",
                    style = MaterialTheme.typography.bodySmall,
                    color = subtleColor
                )
                Text(
                    text = "• Higher range (5000–10000): Slower Loading, more Variety.",
                    style = MaterialTheme.typography.bodySmall,
                    color = subtleColor
                )
                Text(
                    text = "• All: Picks from the entire Pissgrind Library. Best Variety but slowest to load. A stable 5G Connection is recommended if using Mobile Data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = subtleColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
