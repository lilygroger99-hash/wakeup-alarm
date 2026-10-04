package com.wakeup.alarm.util

import android.content.Context
import android.text.format.DateFormat
import com.wakeup.alarm.data.settings.TimeFormatPreference
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formats clock times according to the user's 12/24-hour choice (default: follow the device setting). */
object TimeFormatter {

    fun is24Hour(context: Context, preference: TimeFormatPreference): Boolean = when (preference) {
        TimeFormatPreference.SYSTEM -> DateFormat.is24HourFormat(context)
        TimeFormatPreference.H12 -> false
        TimeFormatPreference.H24 -> true
    }

    /**
     * Splits a time into its main part and an AM/PM marker so the UI can draw the marker smaller.
     * 24-hour mode returns an empty marker. Example: (7, 5, false) -> ("7:05", "AM").
     */
    fun timeParts(hour: Int, minute: Int, is24Hour: Boolean, locale: Locale = Locale.getDefault()): Pair<String, String> {
        val time = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        return if (is24Hour) {
            DateTimeFormatter.ofPattern("HH:mm", locale).format(time) to ""
        } else {
            DateTimeFormatter.ofPattern("h:mm", locale).format(time) to DateTimeFormatter.ofPattern("a", locale).format(time)
        }
    }

    /** Single string such as "7:05 AM" or "07:05". */
    fun timeText(hour: Int, minute: Int, is24Hour: Boolean, locale: Locale = Locale.getDefault()): String {
        val (time, marker) = timeParts(hour, minute, is24Hour, locale)
        return if (marker.isEmpty()) time else "$time $marker"
    }

    /** Formats an absolute moment (epoch millis) as a clock time in the current time zone. */
    fun timeTextFromMillis(millis: Long, is24Hour: Boolean, locale: Locale = Locale.getDefault()): String {
        val local = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
        return timeText(local.hour, local.minute, is24Hour, locale)
    }
}
