package com.wakeup.alarm.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * WakeUp palette: deep twilight indigo as the brand colour with a warm sunrise amber accent (tertiary).
 * Text/background pairs are chosen to meet WCAG AA contrast (4.5:1) for body text.
 */
internal val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF3D4FA0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDEE1FF),
    onPrimaryContainer = Color(0xFF0A1458),
    secondary = Color(0xFF5B5D72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E1F9),
    onSecondaryContainer = Color(0xFF181A2C),
    tertiary = Color(0xFF8B5000),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCBD),
    onTertiaryContainer = Color(0xFF2C1600),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFF777680),
    outlineVariant = Color(0xFFC7C5D0),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF2EFF7),
    inversePrimary = Color(0xFFB9C3FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F2FA),
    surfaceContainer = Color(0xFFEFEDF4),
    surfaceContainerHigh = Color(0xFFE9E7EF),
    surfaceContainerHighest = Color(0xFFE3E1E9),
)

internal val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFB9C3FF),
    onPrimary = Color(0xFF1D2D77),
    primaryContainer = Color(0xFF35458F),
    onPrimaryContainer = Color(0xFFDEE1FF),
    secondary = Color(0xFFC3C5DD),
    onSecondary = Color(0xFF2C2F42),
    secondaryContainer = Color(0xFF424659),
    onSecondaryContainer = Color(0xFFE0E1F9),
    tertiary = Color(0xFFFFB86A),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF6A3C00),
    onTertiaryContainer = Color(0xFFFFDCBD),
    background = Color(0xFF12131A),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF12131A),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    outline = Color(0xFF918F9A),
    outlineVariant = Color(0xFF46464F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE4E1E9),
    inverseOnSurface = Color(0xFF303036),
    inversePrimary = Color(0xFF3D4FA0),
    surfaceContainerLowest = Color(0xFF0D0E15),
    surfaceContainerLow = Color(0xFF1B1B21),
    surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A),
)
