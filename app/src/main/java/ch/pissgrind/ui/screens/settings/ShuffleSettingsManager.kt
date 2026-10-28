package ch.pissgrind.managers.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import ch.pissgrind.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShuffleSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val PLAYLIST_SIZE_OPTIONS = listOf(100, 200, 500, 1000)
        const val DEFAULT_PLAYLIST_SIZE = 200
        private val PLAYLIST_SIZE_KEY = intPreferencesKey("playlist_size")
    }

    val playlistSizeFlow: Flow<Int> = context.dataStore.data
        .map { it[PLAYLIST_SIZE_KEY] ?: DEFAULT_PLAYLIST_SIZE }

    suspend fun setPlaylistSize(size: Int) {
        context.dataStore.edit { it[PLAYLIST_SIZE_KEY] = size }
    }
}

