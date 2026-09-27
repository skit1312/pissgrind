package ch.pissgrind.data.model

sealed class Screen(val route: String) {
    data object Home : Screen("home_screen")
    data object HomeLists : Screen("home_lists")

    data object Song : Screen("songs_screen")
    data object Radio : Screen("radio_screen")

    data object NowPlayingLandscape : Screen("playing_tv_screen")
    data object Search : Screen("search_tv_screen")

    data object Albums : Screen("album_screen")

    data object AlbumDetails : Screen("album_details")

    data object Artists : Screen("artists_screen")
    data object ArtistDetails : Screen("artist_details")
    data object Playlists : Screen("playlist_screen")
    data object PlaylistDetails : Screen("playlist_details")
    data object Setting : Screen("setting_screen")
    data object S_Appearance : Screen("s_appearance_screen")
    data object S_Providers : Screen("s_providers_screen")
    data object S_Shuffle : Screen("s_shuffle_screen")
    data object S_Account : Screen("s_account_screen")
    data object Request : Screen("request_screen")
    data object Library : Screen("library_screen")
    object Collection : Screen("collection_screen")
    object Recommendations : Screen("recommendations_screen")
    object S_Algorithm : Screen("s_algorithm_screen")
    object GenreBrowse : Screen("genre_browse")
}
