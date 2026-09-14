package com.okan.strudelmobile.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Amber = Color(0xFFFFD166)
val Coral = Color(0xFFEF476F)
val Mint = Color(0xFF06D6A0)
val Sky = Color(0xFF4CC9F0)
val Ink = Color(0xFF0F1222)
val InkRaised = Color(0xFF1B1F3B)
val InkLine = Color(0xFF2C3160)

private val Scheme = darkColorScheme(
    primary = Amber,
    onPrimary = Ink,
    secondary = Mint,
    onSecondary = Ink,
    tertiary = Sky,
    error = Coral,
    background = Ink,
    onBackground = Color(0xFFE8E9F3),
    surface = Ink,
    onSurface = Color(0xFFE8E9F3),
    surfaceVariant = InkRaised,
    onSurfaceVariant = Color(0xFFB8BBD3),
    outline = InkLine,
)

@Composable
fun StrudelTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme) {
        // Surface sets LocalContentColor; without it icons and text default to black.
        Surface(color = Scheme.background, contentColor = Scheme.onBackground, content = content)
    }
}
