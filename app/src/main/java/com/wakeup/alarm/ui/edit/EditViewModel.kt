package com.wakeup.alarm.ui.edit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeup.alarm.data.AlarmRepository
import com.wakeup.alarm.data.settings.SettingsRepository
import com.wakeup.alarm.data.settings.TimeFormatPreference
import com.wakeup.alarm.domain.model.Alarm
import com.wakeup.alarm.domain.model.RepeatDays
import com.wakeup.alarm.ui.common.launchSafely
import com.wakeup.alarm.util.SoundInfo
import com.wakeup.alarm.util.SoundUtils
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * State holder of the create/edit screen. The hour and minute are owned by the time picker widget and handed over
 * in [save]; everything else is edited here as a draft that is only persisted on Save.
 */
class EditViewModel(
    private val alarmRepository: AlarmRepository,
    private val settingsRepository: SettingsRepository,
    private val appContext: Context,
    private val alarmId: Long,
) : ViewModel() {

    private val _draft = MutableStateFlow<Alarm?>(null)

    /** The alarm being edited, or `null` while it is loading. */
    val draft: StateFlow<Alarm?> = _draft.asStateFlow()

    private val _timeFormat = MutableStateFlow(TimeFormatPreference.SYSTEM)
    val timeFormat: StateFlow<TimeFormatPreference> = _timeFormat.asStateFlow()

    val soundInfo: StateFlow<SoundInfo> = _draft
        .filterNotNull()
        .map { it.soundUri }
        .distinctUntilChanged()
        .map { SoundUtils.describe(appContext, it) }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SoundInfo())

    private var saving = false

    init {
        launchSafely { load() }
    }

    private suspend fun load() {
        val settings = settingsRepository.current()
        val existing = if (alarmId > 0L) alarmRepository.get(alarmId) else null
        val base = existing ?: Alarm(
            // New alarms start at the next full hour, with the user's defaults.
            hour = LocalTime.now().plusHours(1).hour,
            minute = 0,
            vibrate = settings.defaultVibrate,
            snoozeMinutes = settings.defaultSnoozeMinutes,
            soundUri = settings.defaultSoundUri,
            enabled = true,
        )
        _timeFormat.value = settings.timeFormat
        _draft.value = base
    }

    private fun update(block: (Alarm) -> Alarm) {
        _draft.value = _draft.value?.let(block)
    }

    fun setLabel(label: String) = update { it.copy(label = label.take(Alarm.MAX_LABEL_LENGTH)) }

    fun toggleDay(day: DayOfWeek) = update { it.copy(repeatDays = RepeatDays.toggle(it.repeatDays, day)) }

    fun setRepeatDays(mask: Int) = update { it.copy(repeatDays = mask and RepeatDays.ALL) }

    fun setSound(uri: String) = update { it.copy(soundUri = uri) }

    fun setVibrate(vibrate: Boolean) = update { it.copy(vibrate = vibrate) }

    fun setSnoozeMinutes(minutes: Int) = update { it.copy(snoozeMinutes = minutes) }

    fun setEnabled(enabled: Boolean) = update { it.copy(enabled = enabled) }

    /** Saves the alarm with the picked time. [onDone] runs on the main thread after the alarm is stored and armed. */
    fun save(hour: Int, minute: Int, onDone: () -> Unit) {
        val current = _draft.value ?: return
        if (saving) return // ignore double taps (would otherwise create two alarms)
        saving = true
        launchSafely {
            try {
                alarmRepository.save(current.copy(hour = hour, minute = minute))
                onDone()
            } finally {
                saving = false
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = _draft.value?.id ?: return
        launchSafely {
            if (id > 0L) alarmRepository.delete(id)
            onDone()
        }
    }
}
