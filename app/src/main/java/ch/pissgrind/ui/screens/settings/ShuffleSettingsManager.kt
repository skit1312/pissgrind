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
        private val SHUFFLE_RANGE_KEY = intPreferencesKey("shuffle_range")
        const val SHUFFLE_RANGE_ALL = -1
    }

    // Default 5000, range 1000-10000 or ALL (-1)
    val shuffleRangeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SHUFFLE_RANGE_KEY] ?: 5000
    }

    suspend fun setShuffleRange(range: Int) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[SHUFFLE_RANGE_KEY] = range
            }
        }
    }
}
