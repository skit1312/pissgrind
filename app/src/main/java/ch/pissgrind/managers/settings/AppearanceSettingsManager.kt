package ch.pissgrind.managers.settings

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import ch.pissgrind.R
import ch.pissgrind.data.BottomNavItem
import ch.pissgrind.dataStore
import ch.pissgrind.ui.playing.NowPlayingAlignment
import ch.pissgrind.ui.playing.NowPlayingBackground
import ch.pissgrind.ui.screens.HomeItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppearanceSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val NP_BACKGROUND_KEY = stringPreferencesKey("np_background_type")
        private val NP_TITLE_ALIGNMENT = stringPreferencesKey("np_title_alignment")
        private val BOTTOM_NAV_ITEMS_KEY = stringPreferencesKey("bottom_nav_order_v2")
        private val HOME_ITEMS_KEY = stringPreferencesKey("home_items_order")
        private val APP_THEME = stringPreferencesKey("theme")

        enum class AppTheme {
            LIGHT, DARK, SYSTEM
        }
        private val LIGHT_BG_COLOR = stringPreferencesKey("light_bg_color")
        private val SHOW_PROVIDER_DIVIDERS = booleanPreferencesKey("provider_dividers")
        private val USE_REFRESH_ANIMATION = booleanPreferencesKey("use_refresh_animation")
        private val SHOW_TRACK_NUMBERS = booleanPreferencesKey("show_track_numbers")
    }

    val usernameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USERNAME_KEY] ?: "Username"
    }

    suspend fun setUsername(username: String) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[USERNAME_KEY] = username
            }
        }
    }

    val npBackgroundFlow: Flow<NowPlayingBackground> = context.dataStore.data.map { preferences ->
        try {
            NowPlayingBackground.valueOf(
                preferences[NP_BACKGROUND_KEY]
                    ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        NowPlayingBackground.PISSGRIND.name
                    else
                        NowPlayingBackground.PISSGRIND.name
            )
        } catch (e: Exception) {
            println(e.message)
            NowPlayingBackground.PISSGRIND
        }
    }

    suspend fun setBackgroundType(backgroundType: NowPlayingBackground) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[NP_BACKGROUND_KEY] = backgroundType.name
            }
        }
    }

    val homeItemsItemsFlow: Flow<List<HomeItem>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[HOME_ITEMS_KEY]
        val defaultValue = listOf(
            HomeItem("recently_played", true),
            HomeItem("recently_added", true),
            HomeItem("most_played", true),
            HomeItem("random_songs", true)
        )
        try {
            jsonString?.let { Json.decodeFromString<List<HomeItem>>(it) } ?: defaultValue
        } catch (e: Exception) {
            println(e.message)
            defaultValue
        }
    }

    suspend fun setHomeItems(items: List<HomeItem>) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[HOME_ITEMS_KEY] = Json.encodeToString(items)
            }
        }
    }

    val bottomNavItemsFlow: Flow<List<BottomNavItem>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[BOTTOM_NAV_ITEMS_KEY]
        val defaultValue = listOf(
            BottomNavItem("Home", R.drawable.house_light, "home_screen"),
            BottomNavItem("Search", R.drawable.magnifying_glass_light, "library_screen"),
            BottomNavItem("Library", R.drawable.books_light, "collection_screen"),
            BottomNavItem("Request", R.drawable.music_notes_plus_light, "radio_screen"),
        )
        try {
            jsonString?.let {
                val saved = Json.decodeFromString<List<BottomNavItem>>(it)
                // Remap icons from defaults by screenRoute
                saved.map { item ->
                    val default = defaultValue.find { it.screenRoute == item.screenRoute }
                    item.copy(icon = default?.icon ?: item.icon, title = default?.title ?: item.title)
                }
            } ?: defaultValue
        } catch (e: Exception) {
            println(e.message)
            defaultValue
        }
    }

    suspend fun setBottomNavItems(items: List<BottomNavItem>) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[BOTTOM_NAV_ITEMS_KEY] = Json.encodeToString(items)
            }
        }
    }

    val appTheme: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[APP_THEME] ?: "DARK" //change in mainactivity.kt to prevent ui error :~167
    }

    suspend fun setAppTheme(theme: AppTheme) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[APP_THEME] = theme.name
            }
        }
    }
    val lightBackgroundColorFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LIGHT_BG_COLOR] ?: "E6E6E6"
    }
    suspend fun setLightBackgroundColor(hex: String) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[LIGHT_BG_COLOR] = hex
            }
        }
    }

    val showProviderDividersFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_PROVIDER_DIVIDERS] ?: true
    }

    val refreshAnimationFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_REFRESH_ANIMATION]
            ?: (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
    }

    suspend fun setUseRefreshAnimation(useRefreshAnimation: Boolean) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[USE_REFRESH_ANIMATION] = useRefreshAnimation
            }
        }
    }

    val showTrackNumbersFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_TRACK_NUMBERS] ?: true
    }

    suspend fun setShowTrackNumbers(showTrackNumbers: Boolean) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[SHOW_TRACK_NUMBERS] = showTrackNumbers
            }
        }
    }

    val nowPlayingTitleAlignment: Flow<NowPlayingAlignment> =
        context.dataStore.data.map { preferences ->
            NowPlayingAlignment.valueOf(
                preferences[NP_TITLE_ALIGNMENT] ?: NowPlayingAlignment.LEFT.name
            )
        }

    suspend fun setNowPlayingTitleAlignment(nowPlayingTitleAlignment: NowPlayingAlignment) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[NP_TITLE_ALIGNMENT] = nowPlayingTitleAlignment.name
            }
        }
    }
    private val CUSTOM_BG_COLOR = stringPreferencesKey("custom_bg_color")

    val customBackgroundColorFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_BG_COLOR] ?: ""
    }

    suspend fun setCustomBackgroundColor(hex: String) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[CUSTOM_BG_COLOR] = hex
            }
        }
    }
}
