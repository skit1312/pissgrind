package ch.pissgrind.ui.playing.background

import android.util.Log
import ch.pissgrind.ui.theme.LocalIsDark
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import ch.pissgrind.ui.theme.LocalLightBackground

@Stable
@Composable
fun StaticBlurBackground(
    colors: List<Color?>
) {
    Log.d("RECOMPOSITION", "Recomposing Static Blur BG")

    val isDark = LocalIsDark.current
    val lightBg = LocalLightBackground.current
    val topLeftColor = if (isDark) Color(0xFF70DD00) else lightBg
    val bottomRightColor = Color(0xFF000000)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                onDrawBehind {
                    // top left
                    val blackPath = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(blackPath, topLeftColor)

                    // bottom right
                    val greenPath = Path().apply {
                        moveTo(size.width, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(greenPath, bottomRightColor)
                }
            }
    )
}
