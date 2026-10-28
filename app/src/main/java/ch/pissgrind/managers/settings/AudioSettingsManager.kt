package ch.pissgrind.managers.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import ch.pissgrind.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Hardcoded audio settings
        const val TRANSCODING_BITRATE = "128"
        const val TRANSCODING_FORMAT = "mp3"
        const val SCROBBLE_PERCENT = 2

        // Shuffle range
        private val SHUFFLE_RANGE_KEY = intPreferencesKey("shuffle_range")
        const val SHUFFLE_RANGE_ALL = -1
    }

    // ===== HARDCODED =====

    val wifiTranscodingBitrateFlow: Flow<String> = flowOf(TRANSCODING_BITRATE)
    val mobileDataTranscodingBitrateFlow: Flow<String> = flowOf(TRANSCODING_BITRATE)
    val transcodingFormatFlow: Flow<String> = flowOf(TRANSCODING_FORMAT)
    val scrobblePercentFlow: Flow<Int> = flowOf(SCROBBLE_PERCENT)

}
