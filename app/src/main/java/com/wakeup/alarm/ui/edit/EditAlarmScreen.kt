package com.wakeup.alarm.ui.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeup.alarm.R
import com.wakeup.alarm.data.settings.TimeFormatPreference
import com.wakeup.alarm.domain.model.Alarm
import com.wakeup.alarm.domain.model.RepeatDays
import com.wakeup.alarm.ui.common.ChoiceDialog
import com.wakeup.alarm.ui.common.Formatters
import com.wakeup.alarm.ui.common.RowDivider
import com.wakeup.alarm.ui.common.SettingsGroup
import com.wakeup.alarm.ui.common.SettingsRow
import com.wakeup.alarm.ui.common.SwitchRow
import com.wakeup.alarm.ui.common.containerViewModel
import com.wakeup.alarm.ui.common.rememberResources
import com.wakeup.alarm.ui.common.rememberSoundPicker
import com.wakeup.alarm.util.SoundInfo
import com.wakeup.alarm.util.TimeFormatter
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Create ([alarmId] == 0) or edit an alarm. [onClose] is called after Save, Delete or Cancel. */
@Composable
fun EditAlarmScreen(alarmId: Long, onClose: () -> Unit) {
    val viewModel = containerViewModel("edit-$alarmId") {
        EditViewModel(it.alarmRepository, it.settingsRepository, it.appContext, alarmId)
    }
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val soundInfo by viewModel.soundInfo.collectAsStateWithLifecycle()
    val timeFormat by viewModel.timeFormat.collectAsStateWithLifecycle()

    val current = draft
    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val loadingText = stringResource(R.string.edit_loading)
            CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loadingText })
        }
    } else {
        EditContent(
            draft = current,
            soundInfo = soundInfo,
            timeFormat = timeFormat,
            viewModel = viewModel,
            onClose = onClose,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditContent(
    draft: Alarm,
    soundInfo: SoundInfo,
    timeFormat: TimeFormatPreference,
    viewModel: EditViewModel,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val resources = rememberResources()
    val is24Hour = TimeFormatter.is24Hour(context, timeFormat)

    // Created once from the loaded alarm; the picker then owns hour and minute until Save.
    val timeState = rememberTimePickerState(
        initialHour = draft.hour,
        initialMinute = draft.minute,
        is24Hour = is24Hour,
    )
    var useKeyboard by rememberSaveable { mutableStateOf(false) }
    var showSnoozeDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val pickSound = rememberSoundPicker(draft.soundUri) { viewModel.setSound(it) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (draft.id > 0L) R.string.edit_title_edit else R.string.edit_title_new),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
                actions = {
                    if (draft.id > 0L) {
                        IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                    Button(
                        onClick = { viewModel.save(timeState.hour, timeState.minute, onClose) },
                        modifier = Modifier
                            .padding(start = 4.dp, end = 12.dp)
                            .heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            if (useKeyboard) TimeInput(state = timeState) else TimePicker(state = timeState)
            TextButton(onClick = { useKeyboard = !useKeyboard }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(if (useKeyboard) R.string.edit_time_use_dial else R.string.edit_time_use_keyboard))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = draft.label,
                    onValueChange = viewModel::setLabel,
                    label = { Text(stringResource(R.string.edit_label)) },
                    placeholder = { Text(stringResource(R.string.edit_label_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                SettingsGroup {
                    RepeatSection(
                        mask = draft.repeatDays,
                        onPreset = viewModel::setRepeatDays,
                        onToggleDay = viewModel::toggleDay,
                    )
                    RowDivider()
                    SettingsRow(
                        title = stringResource(R.string.edit_sound),
                        summary = Formatters.soundSummary(resources, soundInfo),
                        onClick = pickSound,
                    )
                    RowDivider()
                    SwitchRow(
                        title = stringResource(R.string.edit_vibrate),
                        checked = draft.vibrate,
                        onCheckedChange = viewModel::setVibrate,
                    )
                    RowDivider()
                    SettingsRow(
                        title = stringResource(R.string.edit_snooze),
                        summary = resources.getQuantityString(
                            R.plurals.duration_minutes,
                            draft.snoozeMinutes,
                            draft.snoozeMinutes,
                        ),
                        onClick = { showSnoozeDialog = true },
                    )
                    RowDivider()
                    SwitchRow(
                        title = stringResource(R.string.edit_enabled),
                        checked = draft.enabled,
                        onCheckedChange = viewModel::setEnabled,
                    )
                }
            }
        }
    }

    if (showSnoozeDialog) {
        val options = Alarm.SNOOZE_OPTIONS.map { minutes ->
            minutes to resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes)
        }
        ChoiceDialog(
            title = stringResource(R.string.edit_snooze),
            options = options,
            selected = draft.snoozeMinutes,
            onSelect = {
                viewModel.setSnoozeMinutes(it)
                showSnoozeDialog = false
            },
            onDismiss = { showSnoozeDialog = false },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.edit_delete_title)) },
            text = { Text(stringResource(R.string.edit_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete(onClose)
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun RepeatSection(
    mask: Int,
    onPreset: (Int) -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
) {
    val resources = rememberResources()
    val presets = listOf(
        RepeatDays.NONE to R.string.repeat_once,
        RepeatDays.ALL to R.string.repeat_every_day,
        RepeatDays.WEEKDAYS to R.string.repeat_weekdays,
        RepeatDays.WEEKENDS to R.string.repeat_weekends,
    )
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(text = stringResource(R.string.edit_repeat), style = MaterialTheme.typography.titleMedium)
        Text(
            text = Formatters.repeatSummary(resources, mask),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presets.forEach { (presetMask, label) ->
                FilterChip(
                    selected = (mask and RepeatDays.ALL) == presetMask,
                    onClick = { onPreset(presetMask) },
                    label = { Text(stringResource(label)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        DayToggleRow(mask = mask, onToggle = onToggleDay)
    }
}

/** Seven round Monday...Sunday toggles. Each one is announced with the full weekday name. */
@Composable
private fun DayToggleRow(mask: Int, onToggle: (DayOfWeek) -> Unit) {
    val locale = Locale.getDefault()
    val colors = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        RepeatDays.ORDERED.forEach { day ->
            val selected = RepeatDays.contains(mask, day)
            val fullName = day.getDisplayName(TextStyle.FULL, locale)
            val narrowName = day.getDisplayName(TextStyle.NARROW, locale)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle(day) })
                    .semantics { contentDescription = fullName },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (selected) colors.primary else Color.Transparent,
                    border = if (selected) null else BorderStroke(1.dp, colors.outline),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = narrowName,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) colors.onPrimary else colors.onSurface,
                            // The parent already announces the full day name.
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                    }
                }
            }
        }
    }
}
