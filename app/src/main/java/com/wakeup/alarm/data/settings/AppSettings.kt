package com.wakeup.alarm.data.settings

import com.wakeup.alarm.domain.model.Alarm

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** How times are displayed. [SYSTEM] follows the device's 12/24-hour setting. */
enum class TimeFormatPreference { SYSTEM, H12, H24 }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Use Material You wallpaper colors on Android 12+ instead of the WakeUp palette. */
    val dynamicColor: Boolean = false,
    val timeFormat: TimeFormatPreference = TimeFormatPreference.SYSTEM,
    val defaultSnoozeMinutes: Int = Alarm.DEFAULT_SNOOZE_MINUTES,
    val defaultVibrate: Boolean = true,
    /** Empty string = system default alarm sound. */
    val defaultSoundUri: String = "",
)
