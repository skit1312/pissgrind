package ch.skit.pissgrind.ui.theme

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.tv.material3.MaterialTheme

private val PissColorScheme = darkColorScheme(
    primary = PissGreen,
    onPrimary = PissBlack,
    primaryContainer = PissGreenDark,
    onPrimaryContainer = PissGreen,

    secondary = PissGreen,
    onSecondary = PissBlack,
    secondaryContainer = PissGrey,
    onSecondaryContainer = PissGreen,

    tertiary = PissGreen,
    onTertiary = PissBlack,
    tertiaryContainer = PissGrey,
    onTertiaryContainer = PissGreen,

    background = PissBlack,
    onBackground = PissGreen,

    surface = PissBlack,
    onSurface = PissGreen,
    surfaceVariant = PissGrey,
    onSurfaceVariant = PissGreenLight,

    outline = PissGreenDark,
    outlineVariant = PissGrey,

    inverseSurface = PissGreen,
    inverseOnSurface = PissBlack,
    inversePrimary = PissBlack,

    error = Color(0xFFFF5555),
    onError = PissBlack,
    errorContainer = Color(0xFF3D0000),
    onErrorContainer = Color(0xFFFF5555),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MusicPlayerTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isTv = LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    if (isTv) {
        val colorScheme = androidx.tv.material3.darkColorScheme()

        MaterialTheme(
            colorScheme = colorScheme,
            typography = androidx.tv.material3.Typography(),
            content = content
        )
    } else {
        MaterialExpressiveTheme(
            colorScheme = PissColorScheme,
            typography = Typography,
            content = content,
            motionScheme = MotionScheme.expressive()
        )
    }
    val PissColorScheme = darkColorScheme(
        surface = PissBlack,
        onSurface = PissGreen,
        surfaceVariant = PissGrey,
        onSurfaceVariant = PissGreenLight,
        surfaceContainer = PissBlack,  // NEU – Bottom NavBar Hintergrund
    )
}
