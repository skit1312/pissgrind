package ch.pissgrind.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import ch.pissgrind.ui.elements.AlbumRow
import ch.pissgrind.data.model.Screen
import ch.pissgrind.player.SongHelper
import ch.pissgrind.ui.viewmodels.HomeScreenViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalAlbumRow(
    key: String,
    title: Int?,
    albums: List<MediaItem>,
    mediaController: MediaController?,
    navHostController: NavHostController,
    viewModel: HomeScreenViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = if (LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION)
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            else
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .clickable {
                        navHostController.navigate(Screen.HomeLists.route + "/$key") {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
        ) {
            Text(
                text = stringResource(
                    id = title ?: androidx.media3.session.R.string.error_message_fallback
                ),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = MaterialTheme.typography.headlineSmall.fontSize
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(MaterialTheme.typography.headlineSmall.fontSize.value.dp * 1.2f)
            )
        }
        AlbumRow(
            albums,
            onAlbumSelected = { album ->
                val encodedImage = URLEncoder.encode(album.coverArt, "UTF-8")
                navHostController.navigate(Screen.AlbumDetails.route + "/${album.navidromeID}/$encodedImage") {
                    launchSingleTop = true
                }
            },
            onPlay = { album ->
                coroutineScope.launch {
                    val mediaItems = viewModel.getAlbumSongs(
                        album.mediaMetadata.extras?.getString("navidromeID") ?: ""
                    )
                    if (mediaItems.isNotEmpty())
                        SongHelper.play(
                            mediaItems = mediaItems.subList(1, mediaItems.size),
                            index = 0,
                            mediaController = mediaController
                        )
                }
            }
        )
    }
}
