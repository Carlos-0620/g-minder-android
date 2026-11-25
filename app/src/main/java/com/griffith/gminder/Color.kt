package com.griffith.gminder

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val LightColors = lightColorScheme(
    primary = Color(0xFFB94E20),
    secondary = Color(0xFF536653),
    background = Color(0xFFFFF9F4),
    surface = Color(0xFFFFF9F4),
    surfaceVariant = Color(0xFFF2E9E1),
    onSurfaceVariant = Color(0xFF57483F),
    secondaryContainer = Color(0xFFFFDBC9),
    onSecondaryContainer = Color(0xFF3C1608)
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB596),
    secondary = Color(0xFFB8CDB4),
    background = Color(0xFF1B1512),
    surface = Color(0xFF1B1512),
    surfaceVariant = Color(0xFF352B25),
    onSurfaceVariant = Color(0xFFE4D6CC),
    secondaryContainer = Color(0xFF63341F),
    onSecondaryContainer = Color(0xFFFFDBC9)
)
