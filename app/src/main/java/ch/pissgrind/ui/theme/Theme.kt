package ch.pissgrind.ui.theme

import androidx.compose.material3.ColorScheme
import android.app.Activity
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import ch.pissgrind.ui.theme.LocalIsDark

private fun buildLightColorScheme(bg: Color): ColorScheme = lightColorScheme(
    primary = PissBlack,
    onPrimary = bg,
    primaryContainer = PissGreen.copy(alpha = 0.2f),
    onPrimaryContainer = PissBlack,
    secondary = PissSecondaryGreen,
    onSecondary = bg,
    secondaryContainer = bg,
    onSecondaryContainer = PissBlack,
    background = bg,
    onBackground = PissBlack,
    surface = bg,
    onSurface = PissBlack,
    surfaceVariant = bg,
    tertiary = bg,
    onSurfaceVariant = PissBlack,
    outline = PissBlack,
    outlineVariant = PissGreen.copy(alpha = 0.3f),
    surfaceContainer = bg,
    surfaceContainerHigh = bg,
    surfaceContainerHighest = bg,
    surfaceContainerLow = bg,
    surfaceContainerLowest = bg,
    surfaceBright = bg,
    surfaceDim = bg,
)

val PissGrindShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(2.dp)
)
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
    surfaceContainer = PissBlack,
    surfaceContainerHigh = PissBlack,
    surfaceContainerHighest = PissBlack,
    surfaceContainerLow = PissBlack,
    surfaceContainerLowest = PissBlack,
    surfaceBright = PissBlack,
    surfaceDim = PissBlack,


    outline = PissGreen,
    outlineVariant = PissGrey,

    inverseSurface = PissGreen,
    inverseOnSurface = PissBlack,
    inversePrimary = PissBlack,

    error = Color(0xFFFF5555),
    onError = PissBlack,
    errorContainer = Color(0xFF3D0000),
    onErrorContainer = Color(0xFFFF5555),
)

val LocalLightBackground = staticCompositionLocalOf { PissWhite }
val LocalIsDark = staticCompositionLocalOf { true }
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MusicPlayerTheme(
    darkTheme: Boolean = true,
    lightBackground: Color = PissWhite,
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = if (darkTheme) PissColorScheme else buildLightColorScheme(lightBackground)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    CompositionLocalProvider(
    LocalLightBackground provides lightBackground,
    LocalIsDark provides darkTheme
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = PissGrindShapes,
            content = content,
            motionScheme = MotionScheme.expressive()
        )
    }
}

