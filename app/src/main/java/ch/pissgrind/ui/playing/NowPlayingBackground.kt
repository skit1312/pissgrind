package ch.pissgrind.ui.playing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import ch.pissgrind.ui.playing.background.StaticBlurBackground

enum class NowPlayingBackground {
      PISSGRIND
}

@Preview
@Stable
@Composable
fun NowPlaying_Background(
    colorPalette: List<Color> = emptyList(),
    backgroundStyle: NowPlayingBackground = NowPlayingBackground.PISSGRIND,
    overlayColor: Color = Color.Transparent
) {
    when (backgroundStyle) {
        NowPlayingBackground.PISSGRIND -> StaticBlurBackground(colorPalette)
    }
}


