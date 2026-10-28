package ch.pissgrind.ui.screens

import ch.pissgrind.ui.theme.LocalLightBackground
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import ch.pissgrind.R
import ch.pissgrind.data.model.Screen
import ch.pissgrind.data.recommendation.CoverArtHelper.getCoverArtUrl
import ch.pissgrind.data.recommendation.ScoredAlbum
import ch.pissgrind.data.recommendation.TrainingModeManager
import ch.pissgrind.player.SongHelper
import ch.pissgrind.ui.elements.RippleEffect
import ch.pissgrind.ui.playing.dpToPx
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen
import ch.pissgrind.ui.viewmodels.HomeScreenViewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.net.URLEncoder
import kotlin.math.roundToInt
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.layout.heightIn
import androidx.tv.material3.MaterialTheme
import ch.pissgrind.data.repository.CollectionRepository
import ch.pissgrind.ui.theme.LocalIsDark
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.draw.drawBehind
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@Stable
@Serializable
data class HomeItem(
    var key: String,
    var enabled: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    viewModel: HomeScreenViewModel = hiltViewModel(),
    scaffoldState: BottomSheetScaffoldState,
    onQuickTrainClick: (List<MediaItem>) -> Unit,
    collectionRepository: CollectionRepository? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lightBg = LocalLightBackground.current
    // State
    val recentlyPlayedAlbums by viewModel.recentlyPlayedAlbums.collectAsStateWithLifecycle()
    val recentAlbums by viewModel.recentAlbums.collectAsStateWithLifecycle()
    val mostPlayedAlbums by viewModel.mostPlayedAlbums.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    //val topGenre by viewModel.topGenre.collectAsStateWithLifecycle()
    val myMix by viewModel.myMix.collectAsStateWithLifecycle()
    //val genreMixCovers by viewModel.genreMixCovers.collectAsStateWithLifecycle()
    val state = rememberPullToRefreshState()
    //val selectedShuffleGenre by viewModel.selectedShuffleGenre.collectAsStateWithLifecycle()
    val isDark = LocalIsDark.current

    var showGenreSheet by remember { mutableStateOf(false) }

    var showRipple by remember { mutableIntStateOf(0) }
    val rippleXOffset = LocalWindowInfo.current.containerSize.width / 2
    val rippleYOffset = dpToPx(WindowInsets.statusBars.asPaddingValues().calculateTopPadding().value.roundToInt())

    //val sliderPagerState = rememberPagerState(pageCount = { 3 })

    val onRefresh: () -> Unit = {
        viewModel.loadHomeScreenData()
        viewModel.loadRecommendations(forceRefresh = true)
        showRipple++
    }

    val lazyListState = rememberLazyListState()

    PullToRefreshBox(
        modifier = Modifier,
        state = state,
        isRefreshing = isLoading,
        onRefresh = onRefresh
    ) {
        Column(modifier = Modifier.fillMaxSize()) {


            // ===== STICKY HEADER =====
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    start = 12.dp,
                    end = 12.dp
                )
            ) {
                Image(
                    painter = if (isDark) painterResource(R.drawable.logofullpgreen) else painterResource(R.drawable.logofullpblack ),
                    contentDescription = "sticky header pissgrind logo",
                    modifier = Modifier
                        .height(78.dp)
                        .offset(x = (-40).dp)
                        .weight(1f),
                    alignment = Alignment.CenterStart
                )
                IconButton(
                    onClick = {
                        navHostController.navigate(Screen.Setting.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.gear_fine_fill),
                        contentDescription = null,
                        tint = if (isDark) PissGreen else PissBlack
                    )
                }
            }

            // ===== SCROLLABLE CONTENT =====
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize()
            ) {

                // ===== RECENTLY ADDED (Stacked Cards Pager) =====
                    item(key = "recently_added") {
                    if (recentAlbums.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Recently Added",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) PissGreen else PissBlack,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )

                        val pagerState =
                            rememberPagerState(pageCount = { minOf(recentAlbums.size, 15) })

                        LaunchedEffect(pagerState) {
                            while (true) {
                                delay(5000.milliseconds)
                                val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
                                pagerState.animateScrollToPage(
                                    nextPage,
                                    animationSpec = tween(1200)
                                )
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            pageSpacing = 12.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) { page ->
                            val album = recentAlbums[page]
                            val albumMeta = album.mediaMetadata

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(2.dp))
                                    .border(
                                        1.dp,
                                        if (isDark) PissGreen else PissBlack,
                                        RoundedCornerShape(2.dp)
                                    )
                                    .clickable {
                                        val navidromeID =
                                            albumMeta.extras?.getString("navidromeID") ?: ""
                                        val coverArt = albumMeta.artworkUri?.toString() ?: ""
                                        val encodedImage = URLEncoder.encode(coverArt, "UTF-8")
                                        navHostController.navigate(Screen.AlbumDetails.route + "/${navidromeID}/$encodedImage") {
                                            launchSingleTop = true
                                        }
                                    }
                            ) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(
                                            albumMeta.artworkUri?.toString()
                                                ?.replace("size=128", "size=128")
                                        )
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Album Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Gradient overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    PissBlack.copy(alpha = 0.9f)
                                                )
                                            )
                                        )
                                )

                                // Album info
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Box {
                                            Text(
                                                text = albumMeta.albumTitle?.toString() ?: "",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    drawStyle = Stroke(width = 4f)
                                                ),
                                                fontWeight = FontWeight.Bold,
                                                color = PissBlack,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = albumMeta.albumTitle?.toString() ?: "",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) PissGreen else lightBg,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = albumMeta.artist?.toString() ?: "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Page indicators
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(minOf(pagerState.pageCount, 20)) { index ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .size(if (pagerState.currentPage % 20 == index) 8.dp else 4.dp)
                                        .background(
                                            if (pagerState.currentPage % 20 == index)
                                                if (isDark) PissGreen else PissBlack
                                            else
                                                (if (isDark) PissGreen else PissBlack).copy(alpha = 0.5f),
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }
                    }
                }
                // ===== ACTION GRID =====
                item(key = "action_grid") {
                    Spacer(modifier = Modifier.height(20.dp))

                    var loadingAction by remember { mutableStateOf<String?>(null) }
                    val accent = if (isDark) PissGreen else PissBlack
                    val surfaceCol = if (isDark) PissBlack else lightBg

                    fun runAction(id: String, block: suspend () -> Unit) {
                        if (loadingAction != null) return
                        loadingAction = id
                        coroutineScope.launch {
                            try { block() } finally { loadingAction = null }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .height(184.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .border(1.dp, accent, RoundedCornerShape(2.dp))
                    ) {
                        // === My Mix ===
                        MyMixTile(
                            modifier = Modifier.weight(2f).fillMaxHeight(),
                            myMix = myMix,
                            filled = false,
                            isLoading = loadingAction == "mymix",
                            enabled = loadingAction == null,
                            accent = accent,
                            surfaceCol = surfaceCol,
                        ) {
                            runAction("mymix") {
                                TrainingModeManager.stopTrainingPlaylist()
                                val albumIds = viewModel.getMyMixAlbumIds().take(50)

                                // Parallel laden, max. 5 gleichzeitig (wie im Repository)
                                val semaphore = Semaphore(5)
                                val songs = coroutineScope {
                                    albumIds.map { id ->
                                        async {
                                            semaphore.withPermit {
                                                runCatching { viewModel.getAlbumSongs(id) }
                                                    .getOrDefault(emptyList())
                                                    .shuffled()
                                                    .take(3)        // max. 3 Songs pro Album
                                            }
                                        }
                                    }.awaitAll().flatten()
                                }

                                if (songs.isNotEmpty()) {
                                    SongHelper.play(
                                        shuffleNoConsecutiveAlbums(songs).take(150),
                                        0,
                                        mediaController
                                    )
                                }
                            }
                        }

                        // === Shuffle + Discover ===
                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            GridActionTile(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                label = "Shuffle All",
                                subtitle = null,
                                iconRes = R.drawable.shuffle_angular_light,
                                filled = false,
                                isLoading = loadingAction == "shuffle",
                                enabled = loadingAction == null,
                                accent = accent,
                                surfaceCol = surfaceCol
                            ) {
                                runAction("shuffle") {
                                    TrainingModeManager.stopTrainingPlaylist()
                                    val songs = viewModel.getAllSongsShuffled()
                                    if (songs.isNotEmpty()) {
                                        SongHelper.play(
                                            shuffleNoConsecutiveAlbums(songs),
                                            0,
                                            mediaController
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(thickness = 1.dp, color = accent)

                            GridActionTile(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                label = "Discover",
                                //subtitle = "",
                                iconRes = R.drawable.arrows_left_right_light,
                                filled = false,
                                isLoading = loadingAction == "discover",
                                enabled = loadingAction == null,
                                accent = accent,
                                surfaceCol = surfaceCol
                            ) {
                                runAction("discover") {
                                    val songs = viewModel.getAllSongsShuffled()
                                    if (songs.isNotEmpty()) {
                                        onQuickTrainClick(songs)
                                    }
                                }
                            }
                        }
                    }
                }

                // ===== RECOMMENDED (LazyRow) =====
                if (recommendations.isNotEmpty()) {
                    item(key = "recommended_header") {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Recommended",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) PissGreen else PissBlack
                                )
                            }
                            Text(
                                text = "Show All ->",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) PissGreen else PissBlack,
                                modifier = Modifier
                                    .clickable {
                                        navHostController.navigate(Screen.Recommendations.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                    .border(
                                        1.dp,
                                        if (isDark) PissGreen else PissBlack,
                                        RoundedCornerShape(2.dp)
                                    )

                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    item(key = "recommended_row") {
                        val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
                        val excludedIds by viewModel.excludedAlbumIds.collectAsStateWithLifecycle()

                        val visibleRecommendations = remember(recommendations, excludedIds) {
                            recommendations.filter { it.albumId !in excludedIds }
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = visibleRecommendations.take(10),
                                key = { "rec_${it.albumId}" }
                            ) { album ->
                                RecommendedAlbumCard(
                                    album = album,
                                    coverArtUrl = album.getCoverArtUrl(),
                                    onClick = {
                                        val encodedImage = URLEncoder.encode(album.getCoverArtUrl(128), "UTF-8")
                                        navHostController.navigate(Screen.AlbumDetails.route + "/${album.albumId}/$encodedImage") {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // ===== MOST PLAYED =====
                item(key = "most_played_header") {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeaderWithAction(
                        title = "Most Played",
                        onSeeAll = {
                            navHostController.navigate(Screen.HomeLists.route + "/most_played") {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                item(key = "most_played_row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = mostPlayedAlbums.take(10),
                            key = { "mp_${it.mediaId}" }
                        ) { album ->
                            AlbumCard(album, context, navHostController)
                        }
                    }
                }

                // ===== RECENTLY PLAYED =====
                item(key = "recently_played_header") {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeaderWithAction(
                        title = "Recently Played",
                        onSeeAll = {
                            navHostController.navigate(Screen.HomeLists.route + "/recently_played") {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                item(key = "recently_played_row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = recentlyPlayedAlbums.take(10),
                            key = { "rp_${it.mediaId}" }
                        ) { album ->
                            AlbumCard(album, context, navHostController)
                        }
                    }
                }

                // Bottom spacing
                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            RippleEffect(
                center = Offset(rippleXOffset.toFloat(), rippleYOffset.toFloat()),
                color = MaterialTheme.colorScheme.surfaceVariant,
                key = showRipple
            )
        }
    }
            // Genre Selection Bottom Sheet
            if (showGenreSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showGenreSheet = false },
                    containerColor = if (isDark) PissBlack else lightBg,
                    shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Shuffle by Genre",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) PissGreen else PissBlack,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        val availableGenres = viewModel.getAvailableGenres()

                        LazyColumn(
                            modifier = Modifier.heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(availableGenres) { genre ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(2.dp))
                                        .clickable {
                                            showGenreSheet = false
                                            viewModel.setShuffleGenre(genre)
                                            coroutineScope.launch {
                                                TrainingModeManager.stopTrainingPlaylist()
                                                val songs = viewModel.getSongsForGenre(genre)
                                                if (songs.isNotEmpty()) {
                                                    SongHelper.play(
                                                        songs.shuffled(),
                                                        0,
                                                        mediaController
                                                    )
                                                }
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = genre,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isDark) PissGreen else PissBlack,
                                    )
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.shuffle_angular_light),
                                        contentDescription = null,
                                        tint = if (isDark) PissGreen else PissBlack,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
}


@Composable
private fun MyMixTile(
    modifier: Modifier = Modifier,
    myMix: List<ScoredAlbum>,
    filled: Boolean = true,
    isLoading: Boolean,
    enabled: Boolean,
    accent: Color,
    surfaceCol: Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val bg = if (filled) accent else surfaceCol
    val fg = if (filled) surfaceCol else accent
    Box(
        modifier = modifier
            .background(bg)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(
                    color = accent,
                    start = Offset(size.width - stroke / 2, 0f),
                    end = Offset(size.width - stroke / 2, size.height),
                    strokeWidth = stroke
                )
            }
            .clickable(enabled = enabled) { onClick() }
            .padding(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center),
                color = fg,
                strokeWidth = 3.dp
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Title + Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "My Mix",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = fg
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.music_notes_light),
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (myMix.isNotEmpty())
                        "Songs from ${myMix.size} Albums\nbased on your Preferences"
                    else
                        "Play some music to\nbuild your Mix",
                    style = MaterialTheme.typography.bodySmall,
                    color = fg.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.weight(1f))

                if (myMix.isNotEmpty()) {
                    Box(modifier = Modifier.height(75.dp).fillMaxWidth()) {   // REMEMBER TO CHANGE FOR PROFILES UPDATE -> BACK TO 56.dp!!!!!!!!!!!!!!!!!
                        myMix.take(10).forEachIndexed { index, album ->
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(album.getCoverArtUrl())
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .offset(x = (index * 18).dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .border(1.dp, fg, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderWithAction(title: String, onSeeAll: () -> Unit) {
    val isDark = LocalIsDark.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) PissGreen else PissBlack
        )
        Text(
            text = "Show All ->",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) PissGreen else PissBlack,
            modifier = Modifier
                .clickable { onSeeAll() }
                .border(
                    1.dp,
                    if (isDark) PissGreen else PissBlack,
                    RoundedCornerShape(2.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun AlbumCard(
    album: MediaItem,
    context: android.content.Context,
    navHostController: NavHostController
) {
    val isDark = LocalIsDark.current
    val albumMeta = album.mediaMetadata
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable {
                val navidromeID = albumMeta.extras?.getString("navidromeID") ?: ""
                val coverArt = albumMeta.artworkUri?.toString() ?: ""
                val encodedImage = URLEncoder.encode(coverArt, "UTF-8")
                navHostController.navigate(Screen.AlbumDetails.route + "/${navidromeID}/$encodedImage") {
                    launchSingleTop = true
                }
            }
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(albumMeta.artworkUri?.toString()?.replace("size=128", "size=64"))
                .crossfade(true)
                .build(),
            contentDescription = "Album Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(2.dp))
                .border(
                    1.dp,
                    if (isDark) PissGreen else PissBlack,
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = albumMeta.albumTitle?.toString() ?: "",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (isDark) PissGreen else PissBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = albumMeta.artist?.toString() ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = if (isDark) PissGreen else PissBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecommendedAlbumCard(
    album: ScoredAlbum,
    coverArtUrl: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isDark = LocalIsDark.current
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(coverArtUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Album Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(2.dp))
                .border(
                    1.dp,
                    if (isDark) PissGreen else PissBlack,
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (isDark) PissGreen else PissBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.labelSmall,
            color = if (isDark) PissGreen else PissBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
/*
@Composable
private fun ShuffleAllSlide() {
    val isDark = LocalIsDark.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.shuffle_angular_light),
                contentDescription = null,
                tint = if (isDark) PissGreen else PissBlack,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Shuffle All",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) PissGreen else PissBlack
            )
            Text(
                text = "Play Everything Randomly",
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) PissGreen else PissBlack
            )
        }
    }
}

@Composable
private fun MyMixSlide(
    myMix: List<ScoredAlbum>,
    context: android.content.Context
) {
    val isDark = LocalIsDark.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Text links
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {


                Text(
                    text = "My Mix",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) PissGreen else PissBlack
                )
                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.shuffle_angular_light),
                    contentDescription = null,
                    tint = if (isDark) PissGreen else PissBlack,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Songs from ${myMix.size} Albums\nbased on your Preferences",
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) PissGreen else PissBlack
            )
        }

        Box(modifier = Modifier.width(110.dp).height(60.dp)) {
            myMix.take(5).forEachIndexed { index, album ->
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(album.getCoverArtUrl())
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .offset(x = (index * 16).dp)
                        .clip(RoundedCornerShape(2.dp))
                        .border(
                            1.dp,
                            if (isDark) PissGreen else PissBlack,
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun GenreMixSlide(
    topGenre: String,
    selectedGenre: String?,
    covers: List<String>,
    context: android.content.Context
) {
    val isDark = LocalIsDark.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (covers.size >= 4) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .border(
                        1.dp,
                        if (isDark) PissGreen else PissBlack,
                        RoundedCornerShape(2.dp)
                    )
            ) {
                Column {
                    Row(modifier = Modifier.weight(1f)) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(covers[0])
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(covers[1])
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    Row(modifier = Modifier.weight(1f)) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(covers[2])
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(covers[3])
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }

        // Text
        Column(verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                Text(
                    text = "Genre Mix",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) PissGreen else PissBlack
                )
                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.music_notes_light),
                    contentDescription = null,
                    tint = if (isDark) PissGreen else PissBlack,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = selectedGenre ?: topGenre.ifBlank { "Select genre" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) PissGreen else PissBlack
            )
            Text(
                text = "Hold for more Genres",
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) PissGreen.copy(alpha = 0.7f) else PissBlack.copy(alpha = 0.7f)
            )
        }
    }
}
*/

private fun shuffleNoConsecutiveAlbums(songs: List<MediaItem>): List<MediaItem> {
    if (songs.size <= 2) return songs.shuffled()

    val buckets = songs
        .groupBy { it.mediaMetadata.albumTitle?.toString() ?: it.mediaId }
        .mapValues { it.value.shuffled().toMutableList() }
        .toMutableMap()

    val result = ArrayList<MediaItem>(songs.size)
    var lastKey: String? = null

    while (buckets.isNotEmpty()) {
        // Alle Alben ausser dem letzten; wenn nur noch eines übrig ist, dieses
        val candidates = buckets.keys.filter { it != lastKey }.ifEmpty { buckets.keys.toList() }

        // Unter den grössten Buckets (±1) zufällig wählen -> gleichmässig UND variabel
        val maxRemaining = candidates.maxOf { buckets.getValue(it).size }
        val key = candidates.filter { buckets.getValue(it).size >= maxRemaining - 1 }.random()

        val bucket = buckets.getValue(key)
        result.add(bucket.removeAt(bucket.lastIndex))
        if (bucket.isEmpty()) buckets.remove(key)
        lastKey = key
    }
    return result
}

@Composable
private fun GridActionTile(
    modifier: Modifier = Modifier,
    label: String,
    subtitle: String? = null,
    iconRes: Int,
    filled: Boolean,
    isLoading: Boolean,
    enabled: Boolean,
    accent: Color,
    surfaceCol: Color,
    onClick: () -> Unit
) {
    val bg = if (filled) accent else surfaceCol
    val fg = if (filled) surfaceCol else accent

    Box(
        modifier = modifier
            .background(bg)
            //.border(1.dp, accent)
            .clickable(enabled = enabled) { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = fg,
                strokeWidth = 3.dp
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(iconRes),
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = fg.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
