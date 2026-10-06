package com.wakeup.alarm.ui.common

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import com.wakeup.alarm.R
import com.wakeup.alarm.util.CustomSounds
import com.wakeup.alarm.util.SoundUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Returns a function that opens the sound chooser. The chooser offers the phone's own alarm sounds (system picker),
 * adding the user's own audio file, and the user's saved audio files (which can be deleted again).
 * The result is reported as a stored sound string: "" for the default alarm sound, otherwise a sound URI.
 */
@Composable
fun rememberSoundPicker(currentUri: String, onPicked: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showChooser by remember { mutableStateOf(false) }
    var sounds by remember { mutableStateOf<List<CustomSounds.Entry>>(emptyList()) }
    var pendingDelete by remember { mutableStateOf<CustomSounds.Entry?>(null) }

    suspend fun reload() {
        sounds = withContext(Dispatchers.IO) { CustomSounds.list(context) }
    }

    val systemLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val picked = result.data?.let {
                IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            }
            val defaultUri = SoundUtils.defaultAlarmUri()
            // null = "silent", which an alarm must never be; treat it like the default sound.
            onPicked(if (picked == null || picked == defaultUri) "" else picked.toString())
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) { CustomSounds.import(context, uri) }
                when (result) {
                    is CustomSounds.ImportResult.Added -> {
                        onPicked(result.uriString)
                        reload()
                        showChooser = false
                        toast(context, context.getString(R.string.sound_added, result.name))
                    }
                    CustomSounds.ImportResult.TooBig ->
                        toast(context, context.getString(R.string.sound_too_big, CustomSounds.MAX_MEGABYTES))
                    CustomSounds.ImportResult.TooMany ->
                        toast(context, context.getString(R.string.sound_too_many, CustomSounds.MAX_COUNT))
                    CustomSounds.ImportResult.Unusable ->
                        toast(context, context.getString(R.string.sound_unusable))
                }
            }
        }
    }

    if (showChooser) {
        LaunchedEffect(Unit) { reload() }
        SoundChooserDialog(
            currentUri = currentUri,
            sounds = sounds,
            onSystemSounds = {
                showChooser = false
                launchSystemPicker(context, currentUri) { systemLauncher.launch(it) }
            },
            onAddOwn = {
                try {
                    fileLauncher.launch(arrayOf("audio/*"))
                } catch (_: ActivityNotFoundException) {
                    toast(context, context.getString(R.string.sound_picker_unavailable))
                }
            },
            onChoose = {
                onPicked(it.uriString)
                showChooser = false
            },
            onDelete = { pendingDelete = it },
            onDismiss = { showChooser = false },
        )
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.sound_delete_title)) },
            text = { Text(stringResource(R.string.sound_delete_message, entry.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        scope.launch {
                            withContext(Dispatchers.IO) { CustomSounds.delete(context, entry.uriString) }
                            // If this alarm was using it, fall back to the default sound right away.
                            if (entry.uriString == currentUri) onPicked("")
                            reload()
                        }
                    },
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    return { showChooser = true }
}

@Composable
private fun SoundChooserDialog(
    currentUri: String,
    sounds: List<CustomSounds.Entry>,
    onSystemSounds: () -> Unit,
    onAddOwn: () -> Unit,
    onChoose: (CustomSounds.Entry) -> Unit,
    onDelete: (CustomSounds.Entry) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_sound_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                ActionRow(
                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                    label = stringResource(R.string.sound_chooser_system),
                    onClick = onSystemSounds,
                )
                ActionRow(
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = stringResource(R.string.sound_chooser_add),
                    onClick = onAddOwn,
                )
                if (sounds.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = stringResource(R.string.sound_chooser_mine),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    sounds.forEach { entry ->
                        val selected = entry.uriString == currentUri
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .selectable(selected = selected, role = Role.RadioButton, onClick = { onChoose(entry) }),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(selected = selected, onClick = null)
                            Text(
                                text = entry.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { onDelete(entry) }, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.sound_delete_cd, entry.name),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

@Composable
private fun ActionRow(icon: @Composable () -> Unit, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon()
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun launchSystemPicker(context: Context, currentUri: String, launch: (Intent) -> Unit) {
    val defaultUri = SoundUtils.defaultAlarmUri()
    val existing: Uri? = when {
        currentUri.isBlank() -> defaultUri
        CustomSounds.isCustom(context, currentUri) -> null
        else -> runCatching { currentUri.toUri() }.getOrNull()
    }
    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.pick_sound_title))
        defaultUri?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, it) }
        existing?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it) }
    }
    try {
        launch(intent)
    } catch (_: ActivityNotFoundException) {
        toast(context, context.getString(R.string.sound_picker_unavailable))
    }
}

private fun toast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
}
