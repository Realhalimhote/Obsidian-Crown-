package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

@Immutable
data class SeasonalAccentPalette(
    val primary: Color = RoyalGold,
    val secondary: Color = VioletForge
)

val LocalSeasonalAccents = staticCompositionLocalOf { SeasonalAccentPalette() }

val AppShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp)
)

@Composable
fun ObsidianCrownTheme(
    seasonalAccents: SeasonalAccentPalette = SeasonalAccentPalette(),
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = seasonalAccents.primary,
        onPrimary = ObsidianBg,
        primaryContainer = CharcoalElevated,
        onPrimaryContainer = TextPrimary,
        secondary = EmeraldActive,
        onSecondary = ObsidianBg,
        secondaryContainer = EmeraldDim,
        onSecondaryContainer = TextPrimary,
        tertiary = seasonalAccents.secondary,
        onTertiary = TextPrimary,
        tertiaryContainer = VioletDim,
        onTertiaryContainer = TextPrimary,
        error = CrimsonCombat,
        onError = TextPrimary,
        errorContainer = CrimsonDim,
        onErrorContainer = TextPrimary,
        background = ObsidianBg,
        onBackground = TextPrimary,
        surface = CharcoalSurface,
        onSurface = TextPrimary,
        surfaceVariant = CharcoalElevated,
        onSurfaceVariant = TextSecondary,
        outline = CharcoalBorder,
        outlineVariant = CharcoalDivider
    )

    CompositionLocalProvider(LocalSeasonalAccents provides seasonalAccents) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
