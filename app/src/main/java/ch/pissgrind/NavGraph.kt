package ch.pissgrind

import ch.pissgrind.ui.screens.settings.S_AccountScreen
import android.content.res.Configuration
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import ch.pissgrind.data.model.MediaData
import ch.pissgrind.data.model.Screen
import ch.pissgrind.data.model.playlistList
import ch.pissgrind.data.repository.CollectionRepository
import ch.pissgrind.managers.settings.LocalDataSettingsManager
import ch.pissgrind.managers.settings.ShuffleSettingsManager
import ch.pissgrind.ui.screens.AlbumDetails
import ch.pissgrind.ui.screens.AlbumScreen
import ch.pissgrind.ui.screens.ArtistDetails
import ch.pissgrind.ui.screens.CollectionScreen
import ch.pissgrind.ui.screens.GenreBrowseScreen
import ch.pissgrind.ui.screens.HomeListsScreen
import ch.pissgrind.ui.screens.HomeScreen
import ch.pissgrind.ui.screens.PlaylistDetails
import ch.pissgrind.ui.screens.PlaylistScreen
import ch.pissgrind.ui.screens.RequestScreen
import ch.pissgrind.ui.screens.SettingScreen
import ch.pissgrind.ui.screens.SongsScreen
import ch.pissgrind.ui.screens.settings.S_AppearanceScreen
import ch.pissgrind.ui.screens.settings.S_ShuffleScreen
import ch.pissgrind.ui.viewmodels.AlbumScreenViewModel
import ch.pissgrind.ui.viewmodels.ArtistsScreenViewModel
import ch.pissgrind.ui.viewmodels.HomeScreenViewModel
import ch.pissgrind.ui.viewmodels.PlaylistScreenViewModel
import ch.pissgrind.ui.viewmodels.SongsScreenViewModel
import java.net.URLDecoder
import ch.pissgrind.ui.screens.LibraryScreen
import ch.pissgrind.ui.screens.RecommendationsScreen
import ch.pissgrind.ui.screens.settings.AlgorithmSettingsScreen
import ch.pissgrind.ui.viewmodels.AlgorithmSettingsViewModel
import ch.pissgrind.ui.viewmodels.LibraryScreenViewModel
import ch.pissgrind.ui.viewmodels.ShuffleSettingsViewModel


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun SetupNavGraph(
    navController: NavHostController,
    bottomPadding: Dp,
    mediaController: MediaController?,
    scaffoldState: BottomSheetScaffoldState,
    onQuickTrainClick: (List<MediaItem>) -> Unit,
    collectionRepository: CollectionRepository? = null,
) {

    val context = LocalContext.current

    playlistList =
        LocalDataSettingsManager(context).localPlaylists.collectAsStateWithLifecycle(mutableListOf()).value

    val leftPadding =
        if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp + WindowInsets.safeDrawing.asPaddingValues()
            .calculateLeftPadding(LayoutDirection.Ltr)

    val animationSpec = MaterialTheme.LocalMotionScheme.current.slowSpatialSpec<Float>()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.padding(bottom = bottomPadding, start = leftPadding),
        enterTransition = { fadeIn(animationSpec) },
        exitTransition = { fadeOut(animationSpec) },
        popEnterTransition = { fadeIn(animationSpec) },
        popExitTransition = { fadeOut(animationSpec) },
        route = "main_graph"
    ) {
        println("Recomposing NavHost!")

        // ===== HOME =====
        composable(route = Screen.Home.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry("main_graph")
            }
            val viewModel: HomeScreenViewModel = hiltViewModel(parentEntry)
            val libraryPreload: LibraryScreenViewModel = hiltViewModel(parentEntry)
            HomeScreen(
                navHostController = navController,
                mediaController = mediaController,
                viewModel = viewModel,
                scaffoldState = scaffoldState,
                onQuickTrainClick = onQuickTrainClick,
                collectionRepository = collectionRepository
            )
        }

        composable(
            route = Screen.HomeLists.route + "/{category}",
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry("main_graph")
            }
            val viewModel: HomeScreenViewModel = hiltViewModel(parentEntry)
            val category = backStackEntry.arguments?.getString("category") ?: "recently_played"
            val albums = when (category) {
                "recently_played" -> viewModel.recentlyPlayedAlbums.collectAsStateWithLifecycle().value
                "recently_added" -> viewModel.recentAlbums.collectAsStateWithLifecycle().value
                "most_played" -> viewModel.mostPlayedAlbums.collectAsStateWithLifecycle().value
                "random_songs" -> viewModel.shuffledAlbums.collectAsStateWithLifecycle().value
                else -> emptyList()
            }
            HomeListsScreen(
                albums = albums,
                viewModel = viewModel,
                categoryKey = category,
                navHostController = navController,
            )
        }

        // ===== SONGS =====
        composable(route = Screen.Song.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry("main_graph")
            }
            val viewModel: SongsScreenViewModel = hiltViewModel(parentEntry)
            SongsScreen(mediaController, viewModel)
        }

        composable(route = Screen.Radio.route) {
            RequestScreen()
        }

        // ===== LIBRARY =====
        composable(route = Screen.Library.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry("main_graph")
            }
            val artistsViewModel: ArtistsScreenViewModel = hiltViewModel(parentEntry)
            val libraryViewModel: LibraryScreenViewModel =
                hiltViewModel(parentEntry)

            LibraryScreen(
                navHostController = navController,
                mediaController = mediaController,
                viewModel = libraryViewModel,
                artistsViewModel = artistsViewModel
            )
        }

        // ===== ARTIST DETAILS =====
        composable(
            route = Screen.ArtistDetails.route + "?artistId={artistId}&artistName={artistName}",
            arguments = listOf(
                navArgument("artistId") { type = NavType.StringType; defaultValue = "" },
                navArgument("artistName") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry("main_graph")
            }
            val viewModel: ArtistsScreenViewModel = hiltViewModel(parentEntry)
            val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
            val artistName = URLDecoder.decode(
                backStackEntry.arguments?.getString("artistName") ?: "", "UTF-8"
            )

            LaunchedEffect(artistId) {
                if (artistId.isNotEmpty() && viewModel.selectedArtist.value?.navidromeID != artistId) {
                    viewModel.setSelectedArtist(
                        MediaData.Artist(navidromeID = artistId, name = artistName)
                    )
                }
            }

            ArtistDetails(navController, mediaController, viewModel)
        }

        // ===== ALBUMS =====
        composable(route = Screen.Albums.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry("main_graph")
            }
            val viewModel: AlbumScreenViewModel = hiltViewModel(parentEntry)
            AlbumScreen(navController, mediaController, viewModel)
        }

        composable(
            route = Screen.AlbumDetails.route + "/{album}/{image}",
            arguments = listOf(
                navArgument("album") { type = NavType.StringType },
                navArgument("image") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString("album") ?: ""
            val albumImageUri =
                URLDecoder.decode(backStackEntry.arguments?.getString("image"), "UTF-8")
                    .replace("size=128", "size=500")
                    .replace("size=300", "size=500")
                    .replace("size=500", "size=500")
            AlbumDetails(
                albumId,
                albumImageUri.toUri(),
                navController,
                mediaController,
            )
        }

        // ===== ARTISTS GRAPH =====
        navigation(startDestination = Screen.Artists.route, route = "artists_graph") {
            composable(
                route = Screen.Artists.route
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("main_graph")
                }
                val viewModel: ArtistsScreenViewModel = hiltViewModel(parentEntry)
            }
        }

        // ===== PLAYLISTS =====
        navigation(startDestination = Screen.Playlists.route, route = "playlists_graph") {
            composable(route = Screen.Playlists.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("main_graph")
                }
                val viewModel: PlaylistScreenViewModel = hiltViewModel(parentEntry)
                PlaylistScreen(navController, viewModel)
            }
            composable(route = Screen.PlaylistDetails.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("main_graph")
                }
                val viewModel: PlaylistScreenViewModel = hiltViewModel(parentEntry)
                PlaylistDetails(navController, mediaController, viewModel,)
            }
        }

        // ===== SETTINGS =====
        navigation(startDestination = Screen.Setting.route, route = "settings_graph") {
            composable(route = Screen.Setting.route) {
                SettingScreen(navController)
            }
            composable(
                route = Screen.S_Appearance.route,
                enterTransition = {
                    slideInHorizontally(animationSpec = tween(durationMillis = 300)) { it / 4 } + fadeIn(
                        animationSpec
                    )
                },
                exitTransition = {
                    slideOutHorizontally(animationSpec = tween(durationMillis = 300)) { it / 4 } + fadeOut(
                        animationSpec
                    )
                }
            ) {
                S_AppearanceScreen(navController)
            }

            composable(
                route = Screen.S_Shuffle.route,
                enterTransition = {
                    slideInHorizontally(animationSpec = tween(durationMillis = 300)) { it / 4 } + fadeIn(
                        animationSpec
                    )
                },
                exitTransition = {
                    slideOutHorizontally(animationSpec = tween(durationMillis = 300)) { it / 4 } + fadeOut(
                    animationSpec
                    )
                }
            ) {
                S_ShuffleScreen(navController)
            }

            composable(route = Screen.S_Account.route) {
                S_AccountScreen(navController)
            }
            composable(
                route = Screen.S_Algorithm.route,
                enterTransition = {
                    slideInHorizontally(animationSpec = tween(durationMillis = 300)) { it / 4 } + fadeIn(
                        tween(300)
                    )
                },
                exitTransition = {
                    slideOutHorizontally(animationSpec = tween(durationMillis = 300)) { it / 4 } + fadeOut(
                        tween(300)
                    )
                }
            ) {
                val viewModel = hiltViewModel<AlgorithmSettingsViewModel>()
                AlgorithmSettingsScreen(
                    navHostController = navController,
                    algorithmLogger = viewModel.algorithmLogger,
                    trainingDataManager = viewModel.trainingDataManager,
                    recommendationCache = viewModel.recommendationCache,
                    excludedAlbumsManager = viewModel.excludedAlbumsManager
                )
            }


            // ===== SEARCH =====
            composable(route = Screen.Search.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("main_graph")
                }
                val albumViewModel: AlbumScreenViewModel = hiltViewModel(parentEntry)
                val songViewModel: SongsScreenViewModel = hiltViewModel(parentEntry)
                val artistViewModel: ArtistsScreenViewModel = hiltViewModel(parentEntry)
            }

            composable(route = Screen.Request.route) {
                RequestScreen()
            }

            composable(
                route = Screen.GenreBrowse.route + "?category={category}",
                arguments = listOf(navArgument("category") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) {
                GenreBrowseScreen(navHostController = navController)
            }

            // ===== COLLECTION / LIBRARY =====
            composable(route = Screen.Collection.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("main_graph")
                }
                val artistsViewModel: ArtistsScreenViewModel = hiltViewModel(parentEntry)
                CollectionScreen(
                    navHostController = navController,
                    mediaController = mediaController,
                    artistsViewModel = artistsViewModel
                )
            }
            composable(route = Screen.Recommendations.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("main_graph")
                }
                val viewModel: HomeScreenViewModel = hiltViewModel(parentEntry)
                RecommendationsScreen(
                    navHostController = navController,
                    mediaController = mediaController,
                    viewModel = viewModel
                )
            }
        }
    }
}