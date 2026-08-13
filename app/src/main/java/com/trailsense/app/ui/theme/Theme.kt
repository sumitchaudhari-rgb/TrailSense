package com.trailsense.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = ShelterGreen,
    secondary = WaterBlue,
    tertiary = ExitAmber,
    background = MockupBg,
    surface = CardBg
)

@Composable
fun TrailSenseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
