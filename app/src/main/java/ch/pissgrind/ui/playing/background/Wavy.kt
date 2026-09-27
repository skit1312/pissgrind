package ch.pissgrind.ui.playing.background

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import kotlin.math.sin

@Composable
fun WavyBackground(
    colorPalette: List<Color>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wavy")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave phase"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave phase 2"
    )

    val baseColor = colorPalette.getOrNull(0) ?: Color(0xFF70DD00)
    val secondColor = colorPalette.getOrNull(1) ?: Color(0xFF66CC00)

    Canvas(modifier = modifier.background(Color.Black)) {
        val width = size.width
        val height = size.height

        // Wave 1
        val path1 = Path().apply {
            moveTo(0f, height * 0.5f)
            for (x in 0..width.toInt() step 4) {
                val y = height * 0.4f + sin(x * 0.008f + phase) * height * 0.08f +
                        sin(x * 0.004f + phase * 0.7f) * height * 0.05f
                lineTo(x.toFloat(), y)
            }
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(path1, baseColor.copy(alpha = 0.3f), style = Fill)

        // Wave 2
        val path2 = Path().apply {
            moveTo(0f, height * 0.6f)
            for (x in 0..width.toInt() step 4) {
                val y = height * 0.55f + sin(x * 0.006f + phase2) * height * 0.06f +
                        sin(x * 0.012f + phase2 * 1.3f) * height * 0.03f
                lineTo(x.toFloat(), y)
            }
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(path2, secondColor.copy(alpha = 0.2f), style = Fill)

        // Wave 3 - subtle top
        val path3 = Path().apply {
            moveTo(0f, height * 0.3f)
            for (x in 0..width.toInt() step 4) {
                val y = height * 0.25f + sin(x * 0.01f + phase * 1.5f) * height * 0.04f
                lineTo(x.toFloat(), y)
            }
            lineTo(width, 0f)
            lineTo(0f, 0f)
            close()
        }
        drawPath(path3, baseColor.copy(alpha = 0.15f), style = Fill)
    }
}
