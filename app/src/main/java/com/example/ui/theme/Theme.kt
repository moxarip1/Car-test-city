package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HypercarColorScheme = darkColorScheme(
    primary = SunsetGold,
    secondary = ElectricCyan,
    tertiary = DriftAmber,
    background = ObsidianSurface,
    surface = ObsidianCard,
    onPrimary = ObsidianSurface,
    onSecondary = ObsidianSurface,
    onBackground = PearlText,
    onSurface = PearlText,
    error = BrakeCrimson
)

@Composable
fun HypercarResortTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HypercarColorScheme,
        typography = Typography,
        content = content
    )
}
