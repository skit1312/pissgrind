package ch.pissgrind.ui.playing

import android.content.Context
import androidx.media3.common.MediaMetadata
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.palette.graphics.Palette
import ch.pissgrind.data.local.entity.CollectionItem
import ch.pissgrind.data.local.entity.CollectionItemType
import ch.pissgrind.data.recommendation.AlgorithmLogger
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import ch.pissgrind.data.recommendation.RecommendationRepository
import ch.pissgrind.data.recommendation.TrainingDataManager
import ch.pissgrind.data.recommendation.TrainingModeManager
import ch.pissgrind.data.repository.CollectionRepository
import ch.pissgrind.data.repository.PlaylistRepository
import ch.pissgrind.data.repository.SongRepository
import ch.pissgrind.managers.settings.AppearanceSettingsManager
import ch.pissgrind.managers.settings.AudioSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ch.pissgrind.data.recommendation.TrainingDataEvent


@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val songRepository: SongRepository,
    val appearanceSettingsManager: AppearanceSettingsManager,
    val playbackSettingsManager: AudioSettingsManager,
    private val trainingDataManager: TrainingDataManager,
    private val recommendationRepository: RecommendationRepository,
    private val algorithmLogger: AlgorithmLogger,
    private val playlistRepository: PlaylistRepository,
    private val collectionRepository: CollectionRepository
) : ViewModel() {

    private val _isCurrentSongInFavourites = MutableStateFlow(false)
    val isCurrentSongInFavourites: StateFlow<Boolean> = _isCurrentSongInFavourites

    private val _playQueueOpen = MutableStateFlow(false)

    val playQueueOpen = _playQueueOpen.asStateFlow()

    private val _detailsOpen = MutableStateFlow(false)

    val detailsOpen = _detailsOpen.asStateFlow()

    fun setPlayQueueOpen(open: Boolean) {
        _playQueueOpen.value = open
    }

    fun setDetailsOpen(open: Boolean) {
        _detailsOpen.value = open
    }

    val backgroundStyle = appearanceSettingsManager.npBackgroundFlow

    private val _paletteColors = MutableStateFlow<List<Color>>(emptyList())
    val paletteColors = _paletteColors.asStateFlow()

    private val _iconTextColor = MutableStateFlow<Color>(Color.White)
    val iconTextColor = _iconTextColor.asStateFlow()

    private val _isBackgroundDark = MutableStateFlow(false)
    val isBackgroundDark = _isBackgroundDark.asStateFlow()

    private val _meta = MutableStateFlow(MediaItem.EMPTY)
    val metadata = _meta.asStateFlow()

// ===== TRAINING MODE =====

    // Combined -> show buttons if either is active
    val shouldShowFeedback = combine(
        TrainingModeManager.isTrainingPlaylist,
        TrainingModeManager.isPersistentTraining
    ) { trainingPlaylist, persistent -> trainingPlaylist || persistent }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun stopTrainingMode() {
        TrainingModeManager.stopTrainingPlaylist()
        _feedbackGiven.value = emptySet()
    }

    private val _feedbackGiven = MutableStateFlow<Set<String>>(emptySet())
    val feedbackGiven = _feedbackGiven.asStateFlow()


    /**
     * Get Genres for the current Album from Recommendations Cache.
     */
    fun getGenresForAlbum(albumId: String): List<String> {
        // Try to find in cached Recommendations
        return try {
            // Simple Lookup -> Genres are stored in the ScoredAlbum
            emptyList() // Will be populated from metadata extras
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ===== END TRAINING MODE =====

    fun updatePaletteFromUri(
        uri: Uri?,
        currentBackgroundStyle: NowPlayingBackground,
        isSystemDark: Boolean
    ) {
        _paletteColors.value = listOf(Color(0xFF70DD00), Color(0xFF66CC00), Color(0xFF336600))
        _isBackgroundDark.value = true
        _iconTextColor.value = Color.White
    }

    private suspend fun extractColorsFromUri(uri: String, context: Context): List<Color?> =
        coroutineScope {
            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(uri.replace("size=128", "size=32"))
                .allowHardware(false)
                .diskCachePolicy(CachePolicy.DISABLED)
                .build()

            val result = (loader.execute(request) as? SuccessResult)?.drawable
            val bitmap = result?.toBitmap()

            bitmap?.let { bitmapImage ->
                withContext(Dispatchers.Default) {
                    val palette = Palette.Builder(bitmapImage).generate()

                    val swatches = mapOf(
                        "Muted" to palette.mutedSwatch,
                        "Dark Vibrant" to palette.darkVibrantSwatch,
                        "Light Vibrant" to palette.lightVibrantSwatch,
                        "Vibrant" to palette.vibrantSwatch,
                        "Dominant" to palette.dominantSwatch,
                        "Light Muted" to palette.lightMutedSwatch
                    )

                    swatches.forEach { (name, swatch) ->
                        swatch?.let {
                            val hex = Integer.toHexString(it.rgb).uppercase()
                            Log.d(
                                "PaletteColor",
                                "$name: #$hex | Body Text Color: ${Integer.toHexString(it.bodyTextColor)}"
                            )
                        }
                    }

                    listOf(
                        palette.vibrantSwatch?.rgb?.let { Color(it) },
                        palette.darkVibrantSwatch?.rgb?.let { Color(it) },
                        palette.dominantSwatch?.rgb?.let { Color(it) },
                        palette.lightVibrantSwatch?.rgb?.let { Color(it) },
                        palette.mutedSwatch?.rgb?.let { Color(it) },
                        palette.lightMutedSwatch?.rgb?.let { Color(it) },
                    )
                }
            } ?: listOf()
        }

    fun submitFeedback(
        albumId: String,
        albumName: String,
        artistName: String,
        genres: List<String>,
        liked: Boolean,
        artistId: String = "",
        songId: String = ""
    ) {
        if (albumId.isBlank()) return

        val trackId = songId.ifBlank { albumId }
        if (trackId in _feedbackGiven.value) return

        viewModelScope.launch {
            _feedbackGiven.value = _feedbackGiven.value + trackId
            recommendationRepository.submitFeedback(albumId, genres, liked, artistId, artistName)

            val albumScore = trainingDataManager.getAlbumScore(albumId)

            if (liked) {
                algorithmLogger.logThumbsUp(albumId, albumName, artistName, genres, albumScore)
            } else {
                val artistAdjustments = trainingDataManager.getArtistAdjustments()
                val artistStrikes = when (artistAdjustments[artistId]) {
                    0.75f -> 1
                    0.40f -> 2
                    0.0f -> 3
                    else -> 0
                }
                algorithmLogger.logThumbsDown(
                    albumId,
                    albumName,
                    artistName,
                    genres,
                    albumScore,
                    artistStrikes
                )
            }

            Log.d(
                "NowPlayingVM",
                "Training feedback: albumId=$albumId, liked=$liked, score=$albumScore, artist=$artistName ($artistId)"
            )
            TrainingDataEvent.markChanged()
        }
    }

    private val FAVOURITES_PLAYLIST_NAME = "Favourites"

    fun addCurrentSongToLibrary(metadata: androidx.media3.common.MediaMetadata?) {
        metadata ?: return
        viewModelScope.launch {
            val navidromeId = metadata.extras?.getString("navidromeID") ?: return@launch
            val collectionItem = CollectionItem(
                id = navidromeId,
                type = CollectionItemType.SONG,
                title = metadata.title.toString(),
                subtitle = metadata.artist.toString(),
                coverArtUrl = metadata.artworkUri.toString(),
                streamUrl = "",
                duration = metadata.extras?.getInt("duration"),
                trackNumber = metadata.trackNumber,
                year = metadata.recordingYear,
                album = metadata.albumTitle.toString()
            )
            collectionRepository.addItem(collectionItem)
        }
    }

    fun addCurrentSongToFavourites(metadata: androidx.media3.common.MediaMetadata?) {
        metadata ?: return
        viewModelScope.launch {
            val navidromeId = metadata.extras?.getString("navidromeID") ?: return@launch

            val playlists = playlistRepository.getPlaylists(false)
            val favouritesMediaItem = playlists.find {
                it.mediaMetadata.title?.toString().equals(FAVOURITES_PLAYLIST_NAME, ignoreCase = true)
            }

            val playlistId = if (favouritesMediaItem != null) {
                favouritesMediaItem.mediaMetadata.extras?.getString("navidromeID") ?: favouritesMediaItem.mediaId
            } else {
                val newPlaylist = playlistRepository.createPlaylist(FAVOURITES_PLAYLIST_NAME)
                newPlaylist?.navidromeID ?: return@launch
            }

            playlistRepository.addSongToPlaylist(playlistId, navidromeId)
        }
    }

    fun checkIfInFavourites(navidromeId: String) {
        viewModelScope.launch {
            val playlists = playlistRepository.getPlaylists(false)
            val favourites = playlists.find {
                it.mediaMetadata.title?.toString().equals(FAVOURITES_PLAYLIST_NAME, ignoreCase = true)
            }
            if (favourites != null) {
                val playlistId = favourites.mediaMetadata.extras?.getString("navidromeID") ?: favourites.mediaId
                val songs = playlistRepository.getPlaylistSongs(playlistId)
                _isCurrentSongInFavourites.value = songs.any {
                    it.mediaMetadata.extras?.getString("navidromeID") == navidromeId
                }
            } else {
                _isCurrentSongInFavourites.value = false
            }
        }
    }

    fun removeCurrentSongFromFavourites(metadata: MediaMetadata?) {
        metadata ?: return
        viewModelScope.launch {
            val navidromeId = metadata.extras?.getString("navidromeID") ?: return@launch
            val playlists = playlistRepository.getPlaylists(false)
            val favourites = playlists.find {
                it.mediaMetadata.title?.toString().equals(FAVOURITES_PLAYLIST_NAME, ignoreCase = true)
            } ?: return@launch

            val playlistId = favourites.mediaMetadata.extras?.getString("navidromeID") ?: favourites.mediaId
            val songs = playlistRepository.getPlaylistSongs(playlistId)
            val songIndex = songs.indexOfFirst {
                it.mediaMetadata.extras?.getString("navidromeID") == navidromeId
            }
            if (songIndex >= 0) {
                playlistRepository.removeSongFromPlaylist(playlistId, songIndex, navidromeId)
                _isCurrentSongInFavourites.value = false
            }
        }
    }

    fun removeCurrentSongFromLibrary(metadata: MediaMetadata?) {
        metadata ?: return
        viewModelScope.launch {
            val navidromeId = metadata.extras?.getString("navidromeID") ?: return@launch
            collectionRepository.removeItem(navidromeId)
        }
    }
}

