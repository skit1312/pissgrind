package ch.pissgrind.ui.playing.background

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

@Composable
fun GrainBackground(
    colorPalette: List<Color>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "grain")
    val tick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grain tick"
    )

    val baseColor = Color(0xFFFFFFFF)
    val grainSeed = remember { Random.nextInt() }

    Canvas(modifier = modifier.background(Color.Black)) {
        val random = Random(grainSeed + tick.toInt())
        val dotCount = 7599

        for (i in 0 until dotCount) {
            val x = random.nextFloat() * size.width
            val y = random.nextFloat() * size.height
            val alpha = random.nextFloat() * 0.5f
            val radius = random.nextFloat() * 1.5f + 0.5f

            drawCircle(
                color = baseColor.copy(alpha = alpha),
                radius = radius,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}
