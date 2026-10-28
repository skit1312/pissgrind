package ch.pissgrind.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import ch.pissgrind.managers.settings.AudioSettingsManager
import ch.pissgrind.managers.settings.ShuffleSettingsManager
import ch.pissgrind.ui.theme.LocalIsDark
import ch.pissgrind.ui.theme.LocalLightBackground
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen
import kotlinx.coroutines.launch

@Composable
fun S_ShuffleScreen(
    navHostController: NavHostController
) {
    val context = LocalContext.current
    val shuffleSettingsManager = remember { ShuffleSettingsManager(context.applicationContext) }
    val coroutineScope = rememberCoroutineScope()
    val isDark = LocalIsDark.current

    // Colors -> Future Update
    val accent = if (isDark) PissGreen else PissBlack
    val surfaceCol = if (isDark) PissBlack else LocalLightBackground.current
    val subtleColor = accent.copy(alpha = 0.7f)

    val playlistSize by shuffleSettingsManager.playlistSizeFlow
        .collectAsStateWithLifecycle(ShuffleSettingsManager.DEFAULT_PLAYLIST_SIZE)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
    ) {
        // ====== Header =======
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
                    tint = accent
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Shuffle & Queue",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }

        // ===== CONTENT ======
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Playlist Size",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "How many songs 'Shuffle All' puts in the queue. The whole library is always used as the source.",
                style = MaterialTheme.typography.bodySmall,
                color = subtleColor
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .border(1.dp, accent, RoundedCornerShape(2.dp))
            ) {
                ShuffleSettingsManager.PLAYLIST_SIZE_OPTIONS.forEachIndexed { index, size ->
                    val selected = playlistSize == size
                    val bg by animateColorAsState(
                        targetValue = if (selected) accent else surfaceCol,
                        animationSpec = tween(200),
                        label = "size bg"
                    )
                    val fg by animateColorAsState(
                        targetValue = if (selected) surfaceCol else accent,
                        animationSpec = tween(200),
                        label = "size fg"
                    )

                    if (index > 0) {
                        Box(Modifier.width(1.dp).fillMaxHeight().background(accent))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(bg)
                            .clickable {
                                coroutineScope.launch { shuffleSettingsManager.setPlaylistSize(size) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = size.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = fg
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(thickness = 1.dp, color = accent.copy(alpha = 0.5f))
        }
    }
}
