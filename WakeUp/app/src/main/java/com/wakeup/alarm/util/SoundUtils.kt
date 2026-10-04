package com.wakeup.alarm.util

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.net.toUri

/**
 * Everything about alarm sounds. A stored sound is a URI string; an empty string means "system default alarm sound".
 * Sounds can disappear (SD card removed, ringtone deleted), so playback always goes through [playbackCandidates],
 * which ends with system defaults. If even those fail, [com.wakeup.alarm.alarm.AlarmPlayer] plays a generated beep.
 */
object SoundUtils {

    /** The device's default alarm sound URI (never null on a normal device, but may be on very stripped ones). */
    fun defaultAlarmUri(): Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

    /** URIs to try, in order: the chosen sound, then default alarm, notification and ringtone sounds. */
    fun playbackCandidates(stored: String): List<Uri> {
        val candidates = mutableListOf<Uri>()
        if (stored.isNotBlank()) {
            runCatching { stored.toUri() }.getOrNull()?.let { candidates.add(it) }
        }
        for (type in listOf(RingtoneManager.TYPE_ALARM, RingtoneManager.TYPE_NOTIFICATION, RingtoneManager.TYPE_RINGTONE)) {
            RingtoneManager.getDefaultUri(type)?.let { candidates.add(it) }
        }
        return candidates.distinct()
    }

    /** True if [uriString] can currently be opened. Blocking: call from a background dispatcher. */
    fun isAvailable(context: Context, uriString: String): Boolean {
        if (uriString.isBlank()) return true
        return try {
            context.contentResolver.openAssetFileDescriptor(uriString.toUri(), "r")?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Describes a stored sound for the UI. Blocking: call from a background dispatcher.
     * Never throws; an unreadable sound is reported as [SoundInfo.missing].
     */
    fun describe(context: Context, uriString: String): SoundInfo {
        if (uriString.isBlank()) return SoundInfo(title = null, missing = false, isDefault = true)
        if (!isAvailable(context, uriString)) return SoundInfo(title = null, missing = true, isDefault = false)
        val title = try {
            RingtoneManager.getRingtone(context, uriString.toUri())?.getTitle(context)
        } catch (_: Exception) {
            null
        }
        return SoundInfo(title = title, missing = false, isDefault = false)
    }
}

/** UI description of the sound stored in an alarm or in the settings. */
data class SoundInfo(
    val title: String? = null,
    val missing: Boolean = false,
    val isDefault: Boolean = true,
)
