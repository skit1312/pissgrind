package ch.pissgrind.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import ch.pissgrind.R
import ch.pissgrind.data.model.Screen
import ch.pissgrind.managers.settings.AppearanceSettingsManager
import ch.pissgrind.ui.elements.dialogs.BackgroundDialog
import ch.pissgrind.ui.elements.dialogs.NowPlayingTitleAlignmentDialog
import ch.pissgrind.ui.elements.dialogs.ThemeDialog
import ch.pissgrind.ui.elements.dialogs.dialogFocusable
import ch.pissgrind.ui.playing.NowPlayingBackground
import ch.pissgrind.ui.playing.NowPlayingAlignment
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen
import ch.pissgrind.ui.theme.lightBg
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalUuidApi::class
)
@Composable
@Preview(showSystemUi = false, showBackground = true)
fun S_AppearanceScreen(navHostController: NavHostController = rememberNavController()) {
    var showBackgroundDialog by remember { mutableStateOf(false) }
    var showThemesDialog by remember { mutableStateOf(false) }
    var showNowPlayingTitleAlignmentDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val focusRequester = remember { FocusRequester() }

    val nowPlayingTitleAlignment by AppearanceSettingsManager(context).nowPlayingTitleAlignment.collectAsState(
        NowPlayingAlignment.LEFT

    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navHostController.navigate(Screen.Setting.route) {
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .dialogFocusable()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.circle_half_tilt_light),
                    contentDescription = "Settings Icon",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.Settings_Header_Appearance),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(
                        onClick = {
                            navHostController.navigate(Screen.Setting.route) {
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier
                            .size(56.dp, 70.dp)
                            .focusRequester(focusRequester)
                            .focusProperties { left = FocusRequester.Cancel }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(
                Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {

                // Theme & Background
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val selectedTheme by AppearanceSettingsManager(context).appTheme.collectAsState(
                        AppearanceSettingsManager.Companion.AppTheme.DARK.name
                    )
                    val themes = listOf(
                        AppearanceSettingsManager.Companion.AppTheme.DARK.name,
                        AppearanceSettingsManager.Companion.AppTheme.LIGHT.name,
                        //AppearanceSettingsManager.Companion.AppTheme.SYSTEM.name
                    )
                    val themeStrings = listOf(
                        R.string.Theme_Dark,
                        R.string.Theme_Light,
                        //R.string.Theme_System
                    )
                    SettingsDialogButton(
                        stringResource(R.string.Dialog_Theme),
                        stringResource(id = themeStrings[themes.indexOf(selectedTheme)]),
                        settingsIcon = ImageVector.vectorResource(R.drawable.drop_half_light),
                        toggleEvent = { showThemesDialog = true }
                    )

                    val backgroundType by AppearanceSettingsManager(context).npBackgroundFlow.collectAsState(
                        NowPlayingBackground.PISSGRIND
                    )
                    val backgroundTypeLabels = mapOf(
                        NowPlayingBackground.PISSGRIND to R.string.Background_PissGrind
                    )
                    val scope = rememberCoroutineScope()
                    val selectedBg by AppearanceSettingsManager(context).lightBackgroundColorFlow
                        .collectAsState("#E6E6E6")

                    if (selectedTheme == AppearanceSettingsManager.Companion.AppTheme.LIGHT.name) {
                        val customHex by AppearanceSettingsManager(context).customBackgroundColorFlow
                            .collectAsState("")
                        var hexInput by remember(selectedBg) {
                            mutableStateOf(selectedBg.removePrefix("#"))
                        }

                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            // Preset-Palette
                            Text(
                                text = "Custom Theme Color",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                lightBg.forEach { hex ->
                                    val color = parseHexOrNull(hex) ?: return@forEach
                                    val isSelected = hex.equals(selectedBg, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) PissBlack
                                                else MaterialTheme.colorScheme.onBackground.copy(
                                                    alpha = 0.3f
                                                ),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                scope.launch {
                                                    AppearanceSettingsManager(context)
                                                        .setLightBackgroundColor(hex)
                                                }
                                            }
                                    )
                                }

                                    // Custom-Slot
                                val customSlotColor: Color? = remember(customHex) { parseHexOrNull(customHex) }
                                if (customSlotColor != null) {
                                    val slotSelected = customHex.equals(selectedBg, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(customSlotColor)
                                            .border(
                                                width = if (slotSelected) 3.dp else 2.dp,
                                                color = if (slotSelected) PissBlack
                                                else MaterialTheme.colorScheme.onBackground,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                scope.launch {
                                                    AppearanceSettingsManager(context).setLightBackgroundColor(customHex)
                                                }
                                            }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ── Custom Hex ──
                            Text(
                                text = "Custom Color (Hex)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val parsedColor =
                                    remember(hexInput) { parseHexOrNull("#$hexInput") }

                                OutlinedTextField(
                                    value = hexInput,
                                    onValueChange = { new ->
                                        val filtered = new.filter { it.isLetterOrDigit() }.take(6)
                                        hexInput = filtered
                                        parseHexOrNull("#$filtered")?.let {
                                            scope.launch {
                                                AppearanceSettingsManager(context).setLightBackgroundColor(
                                                    "#$filtered"
                                                )
                                            }
                                        }
                                    },
                                    prefix = { Text("#") },
                                    placeholder = { Text("RRGGBB") },
                                    singleLine = true,
                                    isError = hexInput.isNotBlank() && parsedColor == null,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                        focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(
                                            alpha = 0.5f
                                        )
                                    ),
                                    shape = RoundedCornerShape(2.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = {
                                        parseHexOrNull("#$hexInput")?.let {
                                            scope.launch {
                                                AppearanceSettingsManager(context).setCustomBackgroundColor(
                                                    "#$hexInput"
                                                )
                                            }
                                        }
                                    },
                                    enabled = parsedColor != null
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.floppy_disk_light),
                                        contentDescription = "Save color",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }

                    // Miscellaneous
                    Column(
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {


                        // Refresh Animation
                        val refreshRipple =
                            AppearanceSettingsManager(context).refreshAnimationFlow.collectAsState(
                                true
                            )
                        SettingsSwitch(
                            refreshRipple.value,
                            stringResource(R.string.Setting_RefreshAnimation),
                            R.drawable.arrow_clockwise_light,
                            toggleEvent = {
                                coroutineScope.launch {
                                    AppearanceSettingsManager(context).setUseRefreshAnimation(!refreshRipple.value)
                                }
                            },
                            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        )

                        // Track Numbers in Album View
                        val showTrackNumbers =
                            AppearanceSettingsManager(context).showTrackNumbersFlow.collectAsState(
                                true
                            )
                        SettingsSwitch(
                            showTrackNumbers.value,
                            stringResource(R.string.Setting_TrackNumbersAlbum),
                            R.drawable.list_numbers_light,
                            toggleEvent = {
                                coroutineScope.launch {
                                    AppearanceSettingsManager(context).setShowTrackNumbers(!showTrackNumbers.value)
                                }
                            }
                        )
                    }
                }
            }

            if (showBackgroundDialog)
                BackgroundDialog(setShowDialog = { showBackgroundDialog = it })

            if (showThemesDialog)
                ThemeDialog(setShowDialog = { showThemesDialog = it })

            if (showNowPlayingTitleAlignmentDialog)
                NowPlayingTitleAlignmentDialog(
                    setShowDialog = { showNowPlayingTitleAlignmentDialog = it },
                    title = stringResource(R.string.Setting_NowPlayingTitleAlignment),
                    selection = nowPlayingTitleAlignment,
                    onSet = {
                        runBlocking {
                            AppearanceSettingsManager(context).setNowPlayingTitleAlignment(it)
                        }
                    }
                )
        }
    }
}
    private fun parseHexOrNull(hex: String): Color? {
        return try {
            val cleaned = if (hex.startsWith("#")) hex else "#$hex"
            if (cleaned.length != 7) return null
            Color(android.graphics.Color.parseColor(cleaned))
        } catch (e: Exception) {
            null
        }
    }





