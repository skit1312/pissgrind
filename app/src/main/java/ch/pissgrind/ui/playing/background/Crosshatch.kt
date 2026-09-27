package ch.pissgrind.ui.playing.background

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate

@Composable
fun CrosshatchBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "crosshatch")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val lineColor = Color(0xFF70DD00)

    Canvas(modifier = modifier.background(Color.Black)) {
        rotate(rotation) {
            val spacing = 40f
            val extent = size.width.coerceAtLeast(size.height) * 1.5f

            // Horizontal lines
            var y = -extent
            while (y < extent) {
                drawLine(
                    color = lineColor.copy(alpha = 0.12f),
                    start = Offset(-extent, y),
                    end = Offset(extent, y),
                    strokeWidth = 1f
                )
                y += spacing
            }

            // Vertical lines
            var x = -extent
            while (x < extent) {
                drawLine(
                    color = lineColor.copy(alpha = 0.12f),
                    start = Offset(x, -extent),
                    end = Offset(x, extent),
                    strokeWidth = 1f
                )
                x += spacing
            }
        }
    }
}
