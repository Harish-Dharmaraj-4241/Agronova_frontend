package com.simats.agronova.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AgroGreen,
    secondary = AgroLightGreen,
    tertiary = AgroAccent,
    background = AgroBackground,
    surface = AgroCard,
    onPrimary = AgroCard,
    onBackground = AgroTextPrimary,
    onSurface = AgroTextPrimary
)

@Composable
fun AgronovaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}