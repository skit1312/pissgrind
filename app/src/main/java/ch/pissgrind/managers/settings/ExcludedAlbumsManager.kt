package ch.pissgrind.managers.settings

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import ch.pissgrind.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ExcludedAlbumsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val EXCLUDED_ALBUMS_KEY = stringSetPreferencesKey("excluded_album_ids")

    val excludedAlbumIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[EXCLUDED_ALBUMS_KEY] ?: emptySet()
    }

    suspend fun excludeAlbum(albumId: String) {
        context.dataStore.edit { prefs: MutablePreferences ->
            val current = prefs[EXCLUDED_ALBUMS_KEY] ?: emptySet()
            prefs[EXCLUDED_ALBUMS_KEY] = current + albumId
        }
    }

    suspend fun restoreAlbum(albumId: String) {
        context.dataStore.edit { prefs: MutablePreferences ->
            val current = prefs[EXCLUDED_ALBUMS_KEY] ?: emptySet()
            prefs[EXCLUDED_ALBUMS_KEY] = current - albumId
        }
    }
    suspend fun clearAll() {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs.remove(EXCLUDED_ALBUMS_KEY)
        }
    }
}

