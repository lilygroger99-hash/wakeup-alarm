package com.wakeup.alarm.ui.common

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.wakeup.alarm.R
import com.wakeup.alarm.domain.model.RepeatDays
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** Returns the Resources and re-composes callers when the configuration (language, font scale...) changes. */
@Composable
fun rememberResources(): Resources {
    LocalConfiguration.current
    return LocalContext.current.resources
}

/** Text helpers that need localized resources. Kept free of Compose so they are easy to test. */
object Formatters {

    /** "Once", "Every day", "Weekdays", "Weekends" or e.g. "Mon, Wed, Fri". */
    fun repeatSummary(resources: Resources, mask: Int, locale: Locale = Locale.getDefault()): String =
        when (mask and RepeatDays.ALL) {
            RepeatDays.NONE -> resources.getString(R.string.repeat_once)
            RepeatDays.ALL -> resources.getString(R.string.repeat_every_day)
            RepeatDays.WEEKDAYS -> resources.getString(R.string.repeat_weekdays)
            RepeatDays.WEEKENDS -> resources.getString(R.string.repeat_weekends)
            else -> RepeatDays.ORDERED
                .filter { RepeatDays.contains(mask, it) }
                .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
        }

    /** "7 hours 30 minutes", "1 day 3 hours", "12 minutes" or "less than a minute". Rounds up to whole minutes. */
    fun duration(resources: Resources, millis: Long): String {
        val totalMinutes = ((millis.coerceAtLeast(0L) + 59_999L) / 60_000L)
        val days = (totalMinutes / (24 * 60)).toInt()
        val hours = ((totalMinutes % (24 * 60)) / 60).toInt()
        val minutes = (totalMinutes % 60).toInt()

        val parts = buildList {
            if (days > 0) add(resources.getQuantityString(R.plurals.duration_days, days, days))
            if (hours > 0) add(resources.getQuantityString(R.plurals.duration_hours, hours, hours))
            // Minutes are noise when the alarm is more than a day away.
            if (minutes > 0 && days == 0) add(resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes))
        }
        return if (parts.isEmpty()) resources.getString(R.string.duration_less_than_minute) else parts.joinToString(" ")
    }

    /** "Today", "Tomorrow" or the weekday name for [triggerMillis], relative to [nowMillis]. */
    fun dayLabel(
        resources: Resources,
        triggerMillis: Long,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): String {
        val triggerDate = Instant.ofEpochMilli(triggerMillis).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        return when (triggerDate) {
            today -> resources.getString(R.string.day_today)
            today.plusDays(1) -> resources.getString(R.string.day_tomorrow)
            else -> triggerDate.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        }
    }

    /** One-line description of a sound for lists and settings rows. */
    fun soundSummary(resources: Resources, info: com.wakeup.alarm.util.SoundInfo): String = when {
        info.missing -> resources.getString(R.string.edit_sound_missing)
        info.isDefault -> resources.getString(R.string.edit_sound_default)
        else -> info.title ?: resources.getString(R.string.edit_sound_custom)
    }

    /** Version name of the installed app, or "?" if it cannot be read. */
    fun appVersionName(context: Context): String = try {
        val packageManager = context.packageManager
        val info = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(context.packageName, 0)
        }
        info.versionName ?: "?"
    } catch (_: Exception) {
        "?"
    }
}
