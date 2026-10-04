package com.wakeup.alarm.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeup.alarm.data.settings.AppSettings
import com.wakeup.alarm.data.settings.SettingsRepository
import com.wakeup.alarm.data.settings.ThemeMode
import com.wakeup.alarm.data.settings.TimeFormatPreference
import com.wakeup.alarm.ui.common.launchSafely
import com.wakeup.alarm.util.SoundInfo
import com.wakeup.alarm.util.SoundUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val appContext: Context,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val defaultSoundInfo: StateFlow<SoundInfo> = repository.settings
        .map { it.defaultSoundUri }
        .distinctUntilChanged()
        .map { SoundUtils.describe(appContext, it) }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SoundInfo())

    fun setThemeMode(mode: ThemeMode) = launchSafely { repository.setThemeMode(mode) }

    fun setDynamicColor(enabled: Boolean) = launchSafely { repository.setDynamicColor(enabled) }

    fun setTimeFormat(format: TimeFormatPreference) = launchSafely { repository.setTimeFormat(format) }

    fun setDefaultSnoozeMinutes(minutes: Int) = launchSafely { repository.setDefaultSnoozeMinutes(minutes) }

    fun setDefaultVibrate(enabled: Boolean) = launchSafely { repository.setDefaultVibrate(enabled) }

    fun setDefaultSoundUri(uri: String) = launchSafely { repository.setDefaultSoundUri(uri) }
}
