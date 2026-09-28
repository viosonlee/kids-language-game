package com.kidslanguage.companion.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Primary700,
    onPrimary = Neutral0,
    primaryContainer = Primary100,
    onPrimaryContainer = Primary800,
    secondary = AccentYellow,
    onSecondary = Neutral900,
    secondaryContainer = SuccessBg,
    tertiary = AccentPurple,
    onTertiary = Neutral900,
    background = Neutral50,
    onBackground = Neutral900,
    surface = Neutral0,
    onSurface = Neutral900,
    surfaceVariant = Neutral100,
    onSurfaceVariant = Neutral700,
    outline = Neutral300
)

val AppShapes = Shapes(
    small = RoundedCornerShape(AppRadius.button),
    medium = RoundedCornerShape(AppRadius.optionCard),
    large = RoundedCornerShape(AppRadius.card),
    extraLarge = RoundedCornerShape(AppRadius.dialog)
)

@Composable
fun KidsLanguageTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
