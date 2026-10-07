package com.wakeup.alarm.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * WakeUp palette, taken from the app logo: electric blue-violet as the brand colour (primary) with a hot pink accent
 * (tertiary). Neutrals carry a faint violet tint so surfaces feel part of the same family.
 * Text/background pairs are chosen to meet WCAG AA contrast (4.5:1) for body text.
 */
internal val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF4A2FE0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4DFFF),
    onPrimaryContainer = Color(0xFF1A0B6B),
    secondary = Color(0xFF5C5878),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3DFF5),
    onSecondaryContainer = Color(0xFF1B1830),
    tertiary = Color(0xFFB0128F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8F0),
    onTertiaryContainer = Color(0xFF3A0030),
    background = Color(0xFFFBF9FF),
    onBackground = Color(0xFF1C1B24),
    surface = Color(0xFFFBF9FF),
    onSurface = Color(0xFF1C1B24),
    surfaceVariant = Color(0xFFE5E0F2),
    onSurfaceVariant = Color(0xFF48455A),
    outline = Color(0xFF797590),
    outlineVariant = Color(0xFFC9C4DC),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF312F3B),
    inverseOnSurface = Color(0xFFF3F0FF),
    inversePrimary = Color(0xFFC6BFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F2FC),
    surfaceContainer = Color(0xFFEFECF8),
    surfaceContainerHigh = Color(0xFFE9E6F3),
    surfaceContainerHighest = Color(0xFFE3E0ED),
)

internal val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFC6BFFF),
    onPrimary = Color(0xFF2A12A8),
    primaryContainer = Color(0xFF3A2AB0),
    onPrimaryContainer = Color(0xFFE4DFFF),
    secondary = Color(0xFFC8C3E0),
    onSecondary = Color(0xFF2E2A46),
    secondaryContainer = Color(0xFF444060),
    onSecondaryContainer = Color(0xFFE3DFF5),
    tertiary = Color(0xFFFFABE6),
    onTertiary = Color(0xFF5C0048),
    tertiaryContainer = Color(0xFF84006A),
    onTertiaryContainer = Color(0xFFFFD8F0),
    background = Color(0xFF0F0D1A),
    onBackground = Color(0xFFE6E2F2),
    surface = Color(0xFF0F0D1A),
    onSurface = Color(0xFFE6E2F2),
    surfaceVariant = Color(0xFF47445A),
    onSurfaceVariant = Color(0xFFC9C5DC),
    outline = Color(0xFF938FA8),
    outlineVariant = Color(0xFF47445A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE6E2F2),
    inverseOnSurface = Color(0xFF312F3B),
    inversePrimary = Color(0xFF4A2FE0),
    surfaceContainerLowest = Color(0xFF0A0914),
    surfaceContainerLow = Color(0xFF181627),
    surfaceContainer = Color(0xFF1D1B2E),
    surfaceContainerHigh = Color(0xFF282539),
    surfaceContainerHighest = Color(0xFF333045),
)

/** The logo's blue -> violet -> magenta sweep. Text on top of it is always white (4.5:1 or better on every stop used). */
val BrandGradient: Brush = Brush.linearGradient(
    colors = listOf(Color(0xFF2F4BFF), Color(0xFF6A2CFF), Color(0xFFB02BD8)),
)

/** Deep background of the full-screen ringing UI: easy on the eyes when it wakes you in a dark room. */
val RingBackground: Brush = Brush.verticalGradient(
    colors = listOf(Color(0xFF3A2BC4), Color(0xFF241A7A), Color(0xFF0F0D1A)),
)
