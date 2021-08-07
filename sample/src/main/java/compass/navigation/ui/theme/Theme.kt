package compass.navigation.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable

private val DarkColorPalette = darkColors(
    primary = BlueJeans700,
    primaryVariant = BlueJeans900,
    secondary = CandyPink700,
    secondaryVariant = CandyPink900,
    background = GunMetal900,
    surface = GunMetal900,
    onBackground = CadetBlueCrayola,
    onSurface = CadetBlueCrayola,
)

@Composable
fun CompassTheme(content: @Composable() () -> Unit) {
    val colors = DarkColorPalette

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}