package com.wakeup.alarm.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Base = Typography()

/**
 * Material 3 type scale with a lighter, airier display style for clock digits and slightly stronger titles.
 * All sizes are in `sp`, so they scale with the user's system font size.
 */
internal val WakeUpTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontWeight = FontWeight.Light),
    displayMedium = Base.displayMedium.copy(fontWeight = FontWeight.Light),
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.Light),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.Normal),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Medium),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.Medium),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.Medium),
)

/** Big clock digits on alarm cards. Tabular figures keep digits from jumping when values change. */
val AlarmTimeTextStyle = TextStyle(
    fontSize = 48.sp,
    lineHeight = 52.sp,
    fontWeight = FontWeight.Light,
    letterSpacing = (-1).sp,
    fontFeatureSettings = "tnum",
)

/** Even bigger digits for the full-screen ringing UI. */
val RingTimeTextStyle = TextStyle(
    fontSize = 88.sp,
    lineHeight = 92.sp,
    fontWeight = FontWeight.Light,
    letterSpacing = (-2).sp,
    fontFeatureSettings = "tnum",
)
