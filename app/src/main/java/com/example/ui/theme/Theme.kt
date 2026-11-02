package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val KuroDarkColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = OnCrimsonPrimary,
    primaryContainer = CrimsonContainer,
    onPrimaryContainer = OnCrimsonContainer,
    secondary = ElectricCyan,
    onSecondary = OnCyanSecondary,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = OnCyanContainer,
    tertiary = AmberStar,
    onTertiary = Color(0xFF241A00),
    background = ObsidianBlack,
    onBackground = TextHigh,
    surface = ObsidianSurface,
    onSurface = TextHigh,
    surfaceVariant = ObsidianSurfaceContainer,
    onSurfaceVariant = TextMedium,
    surfaceContainerLowest = Color(0xFF07090E),
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceContainer,
    surfaceContainerHigh = ObsidianSurfaceBright,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF22283A)
)

private val KuroLightColorScheme = lightColorScheme(
    primary = CrimsonLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9DE),
    onPrimaryContainer = Color(0xFF3F0010),
    secondary = CyanLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC4F6FF),
    onSecondaryContainer = Color(0xFF001F24),
    tertiary = Color(0xFFB28700),
    background = LightBackground,
    onBackground = Color(0xFF121622),
    surface = LightSurface,
    onSurface = Color(0xFF121622),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475069),
    outline = Color(0xFFC5CEE0)
)

val KuroShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KuroStreamTheme(
    darkTheme: Boolean = true, // Cinema experience defaults to rich dark mode
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) KuroDarkColorScheme else KuroLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = KuroShapes,
        content = content
    )
}
