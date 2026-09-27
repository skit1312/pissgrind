package ch.pissgrind.ui.screens

import ch.pissgrind.ui.theme.LocalLightBackground
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import ch.pissgrind.R
import ch.pissgrind.data.model.Screen
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen
import ch.pissgrind.ui.theme.LocalIsDark

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    showSystemUi = false, showBackground = true, wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)

@Composable
fun SettingScreen(
    navHostController: NavHostController = rememberNavController()
) {
    val isDark = LocalIsDark.current
    var showQrOverlay by remember { mutableStateOf(false) }
    val lightBg = LocalLightBackground.current
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.settings)) },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                navHostController.navigate(Screen.Home.route) {
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
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    SettingsButton(
                        Screen.S_Appearance.route,
                        R.drawable.circle_half_tilt_light,
                        R.string.Settings_Header_Appearance,
                        navHostController
                    )
                    SettingsButton(
                        Screen.S_Shuffle.route,
                        R.drawable.shuffle_angular_light,
                        R.string.Settings_Header_Shuffle,
                        navHostController
                    )
                    SettingsButton(
                        Screen.S_Algorithm.route,
                        R.drawable.info_light,
                        R.string.Settings_Header_Algorithm,
                        navHostController
                    )
                    SettingsButton(
                        Screen.S_Account.route,
                        R.drawable.letter_circle_p_light,
                        R.string.Settings_Header_Account,
                        navHostController
                    )
                }

                // Share Button
                Button(
                    onClick = { showQrOverlay = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) PissGreen else PissBlack,
                        contentColor = if (isDark) PissBlack else lightBg
                    ),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                        .padding(top = 280.dp) // Adjust Accordingly to Number of Setting Tabs
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.qr_code_light),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Pissgrind", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Overlay for QR-Code
        if (showQrOverlay) {
            ShareQrOverlay(onDismiss = { showQrOverlay = false })
        }
    }
}

@Composable
private fun SettingsButton(route: String, icon: Int, text: Int, navHostController: NavHostController){
    val lightBg = LocalLightBackground.current
    val isDark = LocalIsDark.current
    Button(
        onClick = { navHostController.navigate(route) {
            launchSingleTop = true
        } },
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDark) PissBlack else lightBg,
            contentColor = if (isDark) PissGreen else PissBlack
        ),
        shape = RoundedCornerShape(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ){
            Icon(
                imageVector = ImageVector.vectorResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(start = 20.dp, end = 16.dp)
                    .size(32.dp)
            )
            Text(
                text = stringResource(text),
                color = if (isDark) PissGreen else PissBlack,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 20.dp).weight(1f)
            )
        }
    }
}
@Composable
fun ShareQrOverlay(onDismiss: () -> Unit) {
    val lightBg = LocalLightBackground.current
    val isDark = LocalIsDark.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(32.dp)
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(2.dp))
                .border(1.dp, if (isDark) PissGreen else PissBlack, RoundedCornerShape(2.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "pissgrind",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isDark) PissGreen else PissBlack
            )

            Spacer(modifier = Modifier.height(16.dp))

            // QR Code Image
            Image(
                painter = painterResource(R.drawable.qrcode),
                contentDescription = "QR Code to pissgrind.ch",
                modifier = Modifier
                    .size(250.dp)
                    .clip(RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "www.pissgrind.ch",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) PissGreen else PissBlack,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) PissGreen else PissBlack,
                    contentColor = if (isDark) PissBlack else lightBg
                ),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    }
}

