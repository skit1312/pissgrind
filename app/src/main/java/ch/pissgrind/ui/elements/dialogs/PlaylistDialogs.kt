package ch.pissgrind.ui.elements.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import ch.pissgrind.R
import ch.pissgrind.fadingEdge
import ch.pissgrind.ui.elements.bounceClick
import ch.pissgrind.ui.viewmodels.PlaylistScreenViewModel
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import ch.pissgrind.ui.theme.LocalIsDark
import ch.pissgrind.ui.theme.LocalLightBackground
import ch.pissgrind.ui.theme.PissGreen
import ch.pissgrind.ui.theme.PissBlack
import kotlinx.coroutines.launch

@Preview(showBackground = true)
@Composable
fun PreviewAddToPlaylistDialog(){
    AddSongToPlaylist(setShowDialog = {})
}

@Preview(showBackground = true)
@Composable
fun PreviewNewPlaylistDialog(){
    NewPlaylist(hiltViewModel(),setShowDialog = {})
}

@Preview(showBackground = true)
@Composable
fun PreviewDeletePlaylistDialog(){
    DeletePlaylist(setShowDialog = {})
}

var showAddSongToPlaylistDialog = mutableStateOf(false)
var showNewPlaylistDialog = mutableStateOf(false)
var songToAddToPlaylist = mutableStateOf(MediaItem.EMPTY)
var showDeletePlaylistDialog = mutableStateOf(false)
var playlistToDelete = mutableStateOf("")

@Composable
fun AddSongToPlaylist(
    setShowDialog: (Boolean) -> Unit,
    viewModel: PlaylistScreenViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = LocalIsDark.current
    val accent = if (isDark) PissGreen else PissBlack
    val lightBg = LocalLightBackground.current
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()

    val song = songToAddToPlaylist.value
    val songId = song.mediaMetadata.extras?.getString("navidromeID") ?: ""
    val songTitle = song.mediaMetadata.title?.toString()?.take(30) ?: "Song"

    val playlistSongIds = remember { mutableStateMapOf<String, List<String>>() }
    val busy = remember { mutableStateListOf<String>() }

    LaunchedEffect(playlists, songId) {
        playlists.forEach { pl ->
            val plId = pl.mediaMetadata.extras?.getString("navidromeID") ?: return@forEach
            if (plId in playlistSongIds || plId in busy) return@forEach
            busy.add(plId)
            launch {
                playlistSongIds[plId] = try {
                    viewModel.getPlaylistSongIds(plId)
                } catch (e: Exception) {
                    emptyList()
                }
                busy.remove(plId)
            }
        }
    }

    fun toggle(plId: String, plName: String) {
        if (songId.isBlank() || plId in busy) return
        val ids = playlistSongIds[plId] ?: return
        busy.add(plId)
        coroutineScope.launch {
            val wasInPlaylist = songId in ids
            try {
                if (wasInPlaylist) {
                    viewModel.removeSongFromPlaylistAwait(plId, ids.lastIndexOf(songId), songId)
                } else {
                    viewModel.addSongToPlaylistAwait(plId, songId)
                }
                playlistSongIds[plId] = viewModel.getPlaylistSongIds(plId)

                //Toast.makeText(
                  //  context,
                  //  if (wasInPlaylist) "$songTitle removed from $plName" else "$songTitle added to $plName",
                  //  Toast.LENGTH_SHORT
               // ).show()
          //  } catch (e: Exception) {
            //    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
              //  runCatching { playlistSongIds[plId] = viewModel.getPlaylistSongIds(plId) }
            } finally {
                busy.remove(plId)
            }
        }
    }
    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(2.dp),
            color = if (isDark) PissBlack else lightBg,
            modifier = Modifier.border(1.dp, accent, RoundedCornerShape(2.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ========= Header + X =======
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.Dialog_Add_To_Playlist).split("/")[0])
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(song.mediaMetadata.title)
                            }
                            append(stringResource(R.string.Dialog_Add_To_Playlist).split("/")[1])
                        },
                        style = TextStyle(
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        ),
                        color = accent,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { setShowDialog(false) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.x_light),
                            contentDescription = "Close",
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ==== List ====
                //val listFadingEdge =
                  //  Brush.verticalGradient(0.75f to Color.Red, 1f to Color.Transparent)

                Column(
                    modifier = Modifier
                        .height(256.dp)
                       // .fadingEdge(listFadingEdge)
                        .verticalScroll(rememberScrollState())
                ) {
                    for (playlist in playlists) {
                        val plId = playlist.mediaMetadata.extras?.getString("navidromeID") ?: continue
                        val plName = playlist.mediaMetadata.title.toString()

                        val disabled = songId.startsWith("Local_") xor plId.startsWith("Local_")
                        val isBusy = plId in busy
                        val isChecked = playlistSongIds[plId]?.contains(songId) == true

                        Row(
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .height(64.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(2.dp))
                                .clickable(enabled = !disabled && !isBusy) { toggle(plId, plName) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val artwork = if (plId.startsWith("Local"))
                                playlist.mediaMetadata.artworkData
                            else
                                playlist.mediaMetadata.artworkUri

                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(artwork)
                                    .crossfade(true)
                                    .diskCacheKey(plId)
                                    .build(),
                                contentScale = ContentScale.FillHeight,
                                contentDescription = "Playlist Image",
                                alpha = if (disabled) 0.5f else 1f,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                            Text(
                                text = plName,
                                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                color = if (disabled) accent.copy(0.5f) else accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .weight(1f)
                            )

                            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                if (isBusy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = accent,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { toggle(plId, plName) },
                                        enabled = !disabled,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = accent,
                                            uncheckedColor = accent,
                                            checkmarkColor = if (isDark) PissBlack else lightBg,
                                            disabledCheckedColor = accent.copy(0.3f),
                                            disabledUncheckedColor = accent.copy(0.3f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { showNewPlaylistDialog.value = true },
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .fillMaxWidth()
                        .height(50.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = if (isDark) PissBlack else lightBg
                    )
                ) {
                    Text(stringResource(R.string.Dialog_New_Playlist))
                }

                if (showNewPlaylistDialog.value) {
                    NewPlaylist(viewModel) { showNewPlaylistDialog.value = it }
                }
            }
        }
    }
}

@Composable
fun NewPlaylist(
    viewModel: PlaylistScreenViewModel,
    setShowDialog: (Boolean) -> Unit
) {
    var name: String by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(2.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.Dialog_New_Playlist),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(bottom = 24.dp)
                            .fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.Label_Playlist_Name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val context = LocalContext.current
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                viewModel.createPlaylist(name, "", context)
                                setShowDialog(false)
                            }
                        },
                        modifier = Modifier
                            .padding(top = 24.dp, start = 40.dp, end = 40.dp)
                            .height(50.dp)
                            .fillMaxWidth()
                            .bounceClick(),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            stringResource(R.string.Action_CreatePlaylist),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeletePlaylist(
    setShowDialog: (Boolean) -> Unit,
    viewModel: PlaylistScreenViewModel = hiltViewModel()
) {
    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(2.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.Dialog_Delete_Playlist),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Column {

                        Text(
                            text = stringResource(R.string.Label_Confirm_Delete_Playlist),
                            fontWeight = FontWeight.Normal,
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.deletePlaylist(playlistToDelete.value)
                                setShowDialog(false)
                            },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 24.dp, start = 40.dp, end = 40.dp)
                                .height(50.dp)
                                .fillMaxWidth()
                                .bounceClick(),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                stringResource(R.string.Action_Remove),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }
            }

        }
    }
}