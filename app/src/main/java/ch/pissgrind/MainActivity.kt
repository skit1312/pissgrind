package ch.pissgrind

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.NotificationUtil.IMPORTANCE_LOW
import androidx.media3.common.util.NotificationUtil.createNotificationChannel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.pissgrind.data.BottomNavItem
import ch.pissgrind.data.model.Screen
import ch.pissgrind.data.recommendation.TrainingDataManager
import ch.pissgrind.data.recommendation.TrainingModeManager
import ch.pissgrind.managers.LocalProviderManager
import ch.pissgrind.managers.NavidromeManager
import ch.pissgrind.managers.settings.AppearanceSettingsManager
import ch.pissgrind.player.ChoraMediaLibraryService
import ch.pissgrind.player.rememberManagedMediaController
import ch.pissgrind.ui.elements.dialogs.OnboardingDialog
import ch.pissgrind.ui.playing.NowPlayingContent
import ch.pissgrind.ui.playing.NowPlayingMiniPlayer
import ch.pissgrind.ui.playing.NowPlayingViewModel
import ch.pissgrind.ui.playing.dpToPx
import ch.pissgrind.ui.screens.QuickTrainScreen
import ch.pissgrind.ui.theme.MusicPlayerTheme
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen
import ch.pissgrind.updater.UpdateAvailableDialog
import ch.pissgrind.updater.UpdateChecker
import ch.pissgrind.updater.UpdateInfo
import ch.pissgrind.updater.WhatsNewDialog
import com.gigamole.composefadingedges.FadingEdgesGravity
import com.gigamole.composefadingedges.content.FadingEdgesContentType
import com.gigamole.composefadingedges.content.scrollconfig.FadingEdgesScrollConfig
import com.gigamole.composefadingedges.verticalFadingEdges
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import ch.pissgrind.ui.screens.CollectionViewModel
import ch.pissgrind.data.local.entity.CollectionItem
import ch.pissgrind.data.repository.CollectionRepository
import ch.pissgrind.data.local.entity.CollectionItemType
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.draw.clipToBounds
import androidx.core.view.WindowCompat
import ch.pissgrind.ui.theme.PissWhite
import ch.pissgrind.ui.theme.LocalIsDark

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var trainingDataManager: TrainingDataManager
    @Inject
    lateinit var collectionRepository: CollectionRepository

    @SuppressLint("UnrememberedMutableState", "SuspiciousIndentation")
    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceIntent = Intent(applicationContext, ChoraMediaLibraryService::class.java)
        this@MainActivity.startService(serviceIntent)

        TrainingModeManager.init(applicationContext)

        lifecycleScope.launch {
            kotlinx.coroutines.delay(2000)
            if (NavidromeManager.checkActiveServers()) {
                trainingDataManager.loadFromServer()
            }
        }
        enableEdgeToEdge()

        setContent {
            val selectedThemeName by AppearanceSettingsManager(this).appTheme.collectAsState(
                AppearanceSettingsManager.Companion.AppTheme.DARK.name   //change in appearancesettingsmanager.kt to prevent ui error :~172
            )
            val darkTheme = when (selectedThemeName) {
                AppearanceSettingsManager.Companion.AppTheme.LIGHT.name -> false
                else -> true
            }
            val showRegisteredPopup = mutableStateOf(false)

            if (intent?.data?.scheme == "pissgrind" && intent?.data?.host == "registered") {
                showRegisteredPopup.value = true
            }
            if (showRegisteredPopup.value) {
                AlertDialog(
                    onDismissRequest = { showRegisteredPopup.value = false },
                    confirmButton = {
                        TextButton(onClick = { showRegisteredPopup.value = false }) {
                            Text("OK")
                        }
                    },
                    title = { Text("Registration submitted!") },
                    text = { Text("Your account will be reviewed and approved shortly. You'll receive an email once it's activated.") }
                )
            }

            // Update Check
            var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
            var showWhatsNew by remember { mutableStateOf(false) }
            var whatsNewChangelog by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                val prefs = getSharedPreferences("pissgrind_prefs", Context.MODE_PRIVATE)
                val lastSeenVersion = prefs.getString("last_seen_version", "") ?: ""
                val currentVersion = BuildConfig.VERSION_NAME

                if (lastSeenVersion.isNotEmpty() && lastSeenVersion != currentVersion) {
                    val result = UpdateChecker.checkForUpdate(lastSeenVersion)
                    if (!result.changelog.isNullOrBlank()) {
                        whatsNewChangelog = result.changelog
                        showWhatsNew = true
                    }
                }
                prefs.edit().putString("last_seen_version", currentVersion).apply()

                val lastCheckTime = prefs.getLong("last_update_check", 0L)
                val oneDayMs = 24 * 60 * 60 * 1000L
                if (System.currentTimeMillis() - lastCheckTime > oneDayMs) {
                    val result = UpdateChecker.checkForUpdate(currentVersion)
                    if (result.success) {
                        if (result.hasUpdate) {
                            updateInfo = result
                        }
                        prefs.edit().putLong("last_update_check", System.currentTimeMillis()).apply()
                    }
                }
            }

            updateInfo?.let { info ->
                UpdateAvailableDialog(
                    updateInfo = info,
                    onDismiss = { updateInfo = null }
                )
            }

            if (showWhatsNew) {
                WhatsNewDialog(
                    changelog = whatsNewChangelog,
                    version = BuildConfig.VERSION_NAME,
                    onDismiss = { showWhatsNew = false }
                )
            }

            val lightBgHex by AppearanceSettingsManager(this).lightBackgroundColorFlow
                .collectAsState("#E6E6E6")
            val lightBackground = remember(lightBgHex) {
                try { Color(android.graphics.Color.parseColor(lightBgHex)) } catch (e: Exception) { PissWhite }
            }

            MusicPlayerTheme(darkTheme, lightBackground = lightBackground) {
                val navController = rememberNavController()
                val mediaController by rememberManagedMediaController()
                var metadata by remember { mutableStateOf<MediaMetadata?>(null) }
                val coroutineScope = rememberCoroutineScope()

                // ===== QUICK TRAIN STATE =====
                var showQuickTrain by remember { mutableStateOf(false) }
                var quickTrainPlaylist by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

                val nowPlayingViewModel: NowPlayingViewModel = hiltViewModel()
                val collectionViewModel: CollectionViewModel = hiltViewModel()

                LaunchedEffect(mediaController) {
                    if (mediaController?.currentMediaItem != null) {
                        metadata = mediaController?.currentMediaItem?.mediaMetadata
                    }
                }
                DisposableEffect(mediaController) {
                    val listener = object : Player.Listener {
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            super.onMediaItemTransition(mediaItem, reason)
                            metadata = mediaController?.currentMediaItem?.mediaMetadata
                        }
                    }
                    mediaController?.addListener(listener)
                    onDispose { mediaController?.removeListener(listener) }
                }

                val positionalThreshold = dpToPx(56F).toFloat()
                val velocityThreshold = dpToPx(125F).toFloat()

                val snackbarHostState = remember { SnackbarHostState() }
                val scaffoldState = remember {
                    BottomSheetScaffoldState(
                        bottomSheetState = SheetState(
                            skipPartiallyExpanded = false,
                            initialValue = SheetValue.PartiallyExpanded,
                            skipHiddenState = true,
                            velocityThreshold = { positionalThreshold },
                            positionalThreshold = { velocityThreshold }
                        ), snackbarHostState = snackbarHostState
                    )
                }
                val peekHeight by animateDpAsState(
                    targetValue = if (metadata?.title != null) 72.dp else 0.dp,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "sheetPeekAnimation"
                )

                window.decorView.setBackgroundColor(MaterialTheme.colorScheme.background.toArgb())

                val backCallback = object : OnBackPressedCallback(false) {
                    override fun handleOnBackPressed() {
                        coroutineScope.launch {
                            scaffoldState.bottomSheetState.partialExpand()
                        }
                    }
                }
                onBackPressedDispatcher.addCallback(this, backCallback)

                // ===== MAIN UI =====
                val expanded by remember {
                    derivedStateOf { scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded }
                }

                val view = LocalView.current
                val isDark = LocalIsDark.current

                LaunchedEffect(expanded, isDark) {
                    val window = (view.context as Activity).window
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                        if (expanded) false
                        else !isDark
                }

                Box(modifier = Modifier.clipToBounds()) {
                    Scaffold(
                        snackbarHost = {
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier.padding(bottom = 70.dp)
                            ) { data ->
                                Snackbar(
                                    snackbarData = data,
                                    containerColor = PissGreen,
                                    contentColor = PissBlack,
                                    shape = RoundedCornerShape(2.dp)
                                )
                            }
                        },
                        bottomBar = {
                            AnimatedBottomNavBar(navController, scaffoldState)
                        },
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        containerColor = Color.Transparent
                    ) { paddingValues ->

                        if (LocalWindowInfo.current.containerSize.width < dpToPx(640F)) {
                            BottomSheetScaffold(
                                sheetContainerColor = Color.Transparent,
                                containerColor = Color.Transparent,
                                sheetPeekHeight = peekHeight + 80.dp + WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding(),
                                sheetShape = RoundedCornerShape(0.dp),
                                sheetDragHandle = { },
                                scaffoldState = scaffoldState,
                                sheetContent = {
                                    val sheetScope = rememberCoroutineScope()

                                    Box {
                                        NowPlayingMiniPlayer(
                                            scaffoldState = scaffoldState,
                                            metadata = metadata,
                                            mediaController = mediaController,
                                            snackbarHostState = snackbarHostState,
                                            onClick = {
                                                sheetScope.launch {
                                                    scaffoldState.bottomSheetState.expand()
                                                }
                                            }
                                        )

                                        NowPlayingContent(
                                            mediaController = mediaController,
                                            metadata = metadata,
                                            onNavigateToAlbum = { albumId, imageUrl ->
                                                val encodedImage = java.net.URLEncoder.encode(imageUrl, "UTF-8")
                                                sheetScope.launch {
                                                    scaffoldState.bottomSheetState.partialExpand()
                                                }.invokeOnCompletion {
                                                    navController.navigate(Screen.AlbumDetails.route + "/${albumId}/${encodedImage}") {
                                                        launchSingleTop = true
                                                    }
                                                }
                                            },
                                            onNavigateToArtist = { artistId, artistName ->
                                                sheetScope.launch {
                                                    scaffoldState.bottomSheetState.partialExpand()
                                                }.invokeOnCompletion {
                                                    navController.navigate(
                                                        Screen.ArtistDetails.route + "?artistId=${artistId}&artistName=${
                                                            java.net.URLEncoder.encode(artistName, "UTF-8")
                                                        }"
                                                    ) { launchSingleTop = true }
                                                }
                                            }
                                        )
                                    }

                                    val currentView = LocalView.current
                                    DisposableEffect(scaffoldState.bottomSheetState.targetValue) {
                                        if (scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded) {
                                            currentView.keepScreenOn = true
                                            backCallback.isEnabled = true
                                        } else {
                                            currentView.keepScreenOn = false
                                            backCallback.isEnabled = false
                                        }
                                        onDispose {
                                            currentView.keepScreenOn = false
                                            backCallback.isEnabled = false
                                        }
                                    }
                                }
                            ) {
                                SetupNavGraph(
                                    navController,
                                    peekHeight + paddingValues.calculateBottomPadding(),
                                    mediaController,
                                    scaffoldState = scaffoldState,
                                    onQuickTrainClick = { songs ->
                                        quickTrainPlaylist = songs
                                        showQuickTrain = true
                                    }
                                )
                            }
                        } else {
                            SetupNavGraph(
                                navController,
                                0.dp,
                                mediaController,
                                scaffoldState = scaffoldState,
                                onQuickTrainClick = { songs ->
                                    quickTrainPlaylist = songs
                                    showQuickTrain = true
                                }
                            )
                        }
                    }

                    // ===== QUICK TRAIN OVERLAY =====
                    if (showQuickTrain && quickTrainPlaylist.isNotEmpty()) {
                        QuickTrainScreen(
                            mediaController = mediaController,
                            playlist = quickTrainPlaylist,
                            onExit = { showQuickTrain = false },
                            onThumbsUp = { meta ->
                                val albumId = meta.extras?.getString("albumId") ?: ""
                                val songId = meta.extras?.getString("navidromeID") ?: ""
                                val artistId = meta.extras?.getString("artistId") ?: ""
                                val artistName = meta.artist?.toString() ?: ""
                                val albumName = meta.albumTitle?.toString() ?: ""
                                val genres = meta.genre?.toString()
                                    ?.split(",", "/")
                                    ?.map { it.trim() }
                                    ?.filter { it.isNotBlank() }
                                    ?: emptyList()
                                nowPlayingViewModel.submitFeedback(
                                    albumId, albumName, artistName, genres,
                                    liked = true, artistId = artistId, songId = songId
                                )
                            },
                            onThumbsDown = { meta ->
                                val albumId = meta.extras?.getString("albumId") ?: ""
                                val songId = meta.extras?.getString("navidromeID") ?: ""
                                val artistId = meta.extras?.getString("artistId") ?: ""
                                val artistName = meta.artist?.toString() ?: ""
                                val albumName = meta.albumTitle?.toString() ?: ""
                                val genres = meta.genre?.toString()
                                    ?.split(",", "/")
                                    ?.map { it.trim() }
                                    ?.filter { it.isNotBlank() }
                                    ?: emptyList()
                                nowPlayingViewModel.submitFeedback(
                                    albumId, albumName, artistName, genres,
                                    liked = false, artistId = artistId, songId = songId
                                )
                            },
                            onAddToLibrary = { type, id ->
                                val meta = mediaController?.currentMediaItem?.mediaMetadata ?: return@QuickTrainScreen
                                coroutineScope.launch {
                                    val item = CollectionItem(
                                        id = id,
                                        type = CollectionItemType.valueOf(type),
                                        title = when (type) {
                                            "SONG" -> meta.title?.toString() ?: ""
                                            "ALBUM" -> meta.albumTitle?.toString() ?: ""
                                            "ARTIST" -> meta.artist?.toString() ?: ""
                                            else -> ""
                                        },
                                        subtitle = when (type) {
                                            "SONG", "ALBUM" -> meta.artist?.toString() ?: ""
                                            else -> ""
                                        },
                                        coverArtUrl = meta.artworkUri?.toString() ?: "",
                                        streamUrl = if (type == "SONG") meta.extras?.getString("streamUrl") ?: "" else "",
                                        duration = if (type == "SONG") (mediaController?.duration?.toInt() ?: 0) else 0,
                                        trackNumber = if (type == "SONG") (meta.trackNumber ?: 0) else 0,
                                        year = meta.releaseYear ?: 0,
                                        album = meta.albumTitle?.toString() ?: "",
                                        addedAt = System.currentTimeMillis()
                                    )
                                    collectionRepository.addItem(item)
                                }
                            },
                                    onRemoveFromLibrary = { type, id ->
                                coroutineScope.launch {
                                    collectionRepository.removeItem(id)
                                }
                            }
                        )
                    }
                }

                var showNoProvidersDialog by rememberSaveable { mutableStateOf(false) }
                val allServers by NavidromeManager.allServers.collectAsStateWithLifecycle()

                LaunchedEffect(allServers) {
                    val folders = LocalProviderManager.getAllFolders()
                    showNoProvidersDialog = !(folders.isEmpty() && allServers.isEmpty())
                }

                if (!showNoProvidersDialog) {
                    OnboardingDialog(setShowDialog = { showNoProvidersDialog = it })
                }
            }
        }

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.entries.forEach { permission ->
                Log.d("PERMISSIONS", "Is '${permission.key}' permission granted? ${permission.value}")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS)
            )
        } else {
            requestPermissionLauncher.launch(
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            )
        }

        createNotificationChannel(
            this,
            "download_channel",
            R.string.Notification_Download_Name,
            R.string.Notification_Download_Desc,
            IMPORTANCE_LOW
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityPreStopped(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

                @androidx.annotation.OptIn(UnstableApi::class)
                override fun onActivityDestroyed(activity: Activity) {
                    ChoraMediaLibraryService.getInstance()?.saveState()
                    this@MainActivity.stopService(serviceIntent)
                }
            })
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @Stable
    fun AnimatedBottomNavBar(
        navController: NavHostController,
        scaffoldState: BottomSheetScaffoldState
    ) {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        val isDark = LocalIsDark.current
        val orderedNavItems = AppearanceSettingsManager(context).bottomNavItemsFlow.collectAsState(
            initial = listOf(
                BottomNavItem("Home", R.drawable.house_light, "home_screen"),
                BottomNavItem("Search", R.drawable.magnifying_glass_light, "library_screen"),
                BottomNavItem("Collection", R.drawable.books_light, "collection_screen"),
            )
        ).value

        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            val expanded by remember { derivedStateOf { scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded } }

            val yTrans by animateIntAsState(
                targetValue = if (expanded) dpToPx(
                    -80 - WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding().value.toInt()
                ) else 0,
                label = "Fullscreen Translation"
            )

            Column {
                val dividerAlpha by animateFloatAsState(
                    targetValue = if (expanded) 0f else 1f,
                    animationSpec = tween(durationMillis = 300),
                    label = "Divider Fade"
                )
                HorizontalDivider(
                    color = (if (isDark) PissGreen else PissBlack).copy(alpha = dividerAlpha),
                    thickness = 1.5.dp
                )

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier.offset { IntOffset(x = 0, y = -yTrans) }
                ) {

                    orderedNavItems.forEachIndexed { _, item ->
                        if (!item.enabled) return@forEachIndexed

                        val icon = when (item.screenRoute) {
                            "home_screen" -> R.drawable.house_light
                            "library_screen" -> R.drawable.magnifying_glass_light
                            "collection_screen" -> R.drawable.books_light
                            "radio_screen" -> R.drawable.music_notes_plus_light
                            else -> R.drawable.placeholder
                        }

                        NavigationBarItem(
                            selected = item.screenRoute == backStackEntry?.destination?.route,
                            onClick = {
                                if (item.screenRoute == backStackEntry?.destination?.route) return@NavigationBarItem
                                navController.navigate(item.screenRoute) { launchSingleTop = true }
                                coroutineScope.launch {
                                    if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded)
                                        scaffoldState.bottomSheetState.partialExpand()
                                }
                            },
                            label = { Text(text = item.title) },
                            alwaysShowLabel = false,
                            icon = { Icon(ImageVector.vectorResource(icon), contentDescription = null, modifier = Modifier.size(24.dp)) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent,
                                selectedIconColor = if (isDark) PissGreen else PissBlack,
                                selectedTextColor = if (isDark) PissGreen else PissBlack,
                                unselectedIconColor = if (isDark) PissGreen.copy(alpha = 0.5f) else PissBlack.copy(alpha = 0.5f),
                                unselectedTextColor = if (isDark) PissGreen.copy(alpha = 0.5f) else PissBlack.copy(alpha = 0.5f)
                            )
                        )
                    }
                    if (LocalWindowInfo.current.containerSize.width > dpToPx(640))
                        NavigationBarItem(
                            selected = Screen.NowPlayingLandscape.route == backStackEntry?.destination?.route,
                            onClick = {
                                if (Screen.NowPlayingLandscape.route == backStackEntry?.destination?.route) return@NavigationBarItem
                                navController.navigate(Screen.NowPlayingLandscape.route) { launchSingleTop = true }
                                coroutineScope.launch {
                                    if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded)
                                        scaffoldState.bottomSheetState.partialExpand()
                                }
                            },
                            label = { Text(text = "Playing") },
                            alwaysShowLabel = false,
                            icon = { Icon(ImageVector.vectorResource(R.drawable.caret_circle_double_right_light), contentDescription = "Playing", modifier = Modifier.size(24.dp)) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent,
                                selectedIconColor = if (isDark) PissGreen else PissBlack,
                                selectedTextColor = if (isDark) PissGreen else PissBlack,
                                unselectedIconColor = if (isDark) PissGreen.copy(alpha = 0.5f) else PissBlack.copy(alpha = 0.5f),
                                unselectedTextColor = if (isDark) PissGreen.copy(alpha = 0.5f) else PissBlack.copy(alpha = 0.5f)
                            )
                        )
                }
            }
        } else {
            val lazyColumnState = rememberLazyListState()
            NavigationRail {
                LazyColumn(
                    state = lazyColumnState,
                    modifier = Modifier
                        .weight(1f)
                        .verticalFadingEdges(
                            FadingEdgesContentType.Dynamic.Lazy.List(
                                FadingEdgesScrollConfig.Dynamic(), lazyColumnState
                            ), FadingEdgesGravity.All, 64.dp
                        )
                ) {
                    items(orderedNavItems) { item ->
                        if (!item.enabled) return@items
                        val icon = when (item.screenRoute) {
                            "home_screen" -> R.drawable.house_light
                            "library_screen" -> R.drawable.magnifying_glass_light
                            "collection_screen" -> R.drawable.books_light
                            "radio_screen" -> R.drawable.music_notes_plus_light
                            else -> R.drawable.placeholder
                        }
                        NavigationRailItem(
                            selected = item.screenRoute == backStackEntry?.destination?.route,
                            onClick = {
                                if (item.screenRoute == backStackEntry?.destination?.route) return@NavigationRailItem
                                navController.navigate(item.screenRoute) { launchSingleTop = true }
                                coroutineScope.launch {
                                    if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded)
                                        scaffoldState.bottomSheetState.partialExpand()
                                }
                            },
                            label = { Text(text = item.title) },
                            alwaysShowLabel = false,
                            icon = { Icon(ImageVector.vectorResource(icon), contentDescription = null) }
                        )
                    }
                    item {
                        NavigationRailItem(
                            selected = Screen.NowPlayingLandscape.route == backStackEntry?.destination?.route,
                            onClick = {
                                if (Screen.NowPlayingLandscape.route == backStackEntry?.destination?.route) return@NavigationRailItem
                                navController.navigate(Screen.NowPlayingLandscape.route) { launchSingleTop = true }
                                coroutineScope.launch {
                                    if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded)
                                        scaffoldState.bottomSheetState.partialExpand()
                                }
                            },
                            label = { Text(text = "Playing") },
                            alwaysShowLabel = false,
                            icon = { Icon(ImageVector.vectorResource(R.drawable.caret_circle_double_right_light), contentDescription = "Playing") }
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            trainingDataManager.syncToServer()
        }
    }
}

// ===== TOP-LEVEL FUNCTIONS =====
fun formatMilliseconds(seconds: Int): String {
    return String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
}

fun Modifier.fadingEdge(brush: Brush) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }
