package com.wakeup.alarm.ui.common

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import com.wakeup.alarm.R
import com.wakeup.alarm.util.SoundUtils

/**
 * Returns a function that opens the system alarm-sound picker. The result is reported as a stored sound string:
 * "" for the default alarm sound, otherwise the content URI of the chosen sound.
 */
@Composable
fun rememberSoundPicker(currentUri: String, onPicked: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val picked = result.data?.let {
                IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            }
            val defaultUri = SoundUtils.defaultAlarmUri()
            // null = "silent", which an alarm must never be; treat it like the default sound.
            onPicked(if (picked == null || picked == defaultUri) "" else picked.toString())
        }
    }
    return {
        val defaultUri = SoundUtils.defaultAlarmUri()
        val existing: Uri? = if (currentUri.isBlank()) defaultUri else runCatching { currentUri.toUri() }.getOrNull()
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.pick_sound_title))
            defaultUri?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, it) }
            existing?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it) }
        }
        try {
            launcher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.sound_picker_unavailable, Toast.LENGTH_LONG).show()
        }
    }
}
