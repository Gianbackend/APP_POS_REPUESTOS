package com.example.posapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = BlueGrey80,
    tertiary = Accent80
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),               // ← AZUL para TopBar y Botones
    onPrimary = Color.White,

    primaryContainer = Color(0xFFB4B4B4),      // Cards GRIS CLARO
    onPrimaryContainer = Color(0xFF424242),

    secondary = BlueGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEEEEE),    // Chips/Filtros GRIS
    onSecondaryContainer = Color(0xFF424242),

    tertiary = Accent40,

    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1C1B1F),

    surface = Color.White,//surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),

    surfaceVariant = Color(0xFFF0F0F0),        // Cards GRIS
    onSurfaceVariant = Color(0xFF424242),

    outline = Color(0xFFBDBDBD),

    error = Color(0xFFB00020),
    onError = Color.White
)



@Composable
fun POSAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
