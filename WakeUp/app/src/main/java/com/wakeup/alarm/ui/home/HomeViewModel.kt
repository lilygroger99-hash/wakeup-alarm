package com.wakeup.alarm.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeup.alarm.data.AlarmRepository
import com.wakeup.alarm.data.settings.AppSettings
import com.wakeup.alarm.data.settings.SettingsRepository
import com.wakeup.alarm.domain.AlarmCalculator
import com.wakeup.alarm.domain.model.Alarm
import com.wakeup.alarm.ui.common.launchSafely
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class NextAlarm(val alarm: Alarm, val triggerMillis: Long)

data class HomeUiState(
    val loading: Boolean = true,
    val alarms: List<Alarm> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val next: NextAlarm? = null,
    val nowMillis: Long = 0L,
)

class HomeViewModel(
    private val alarmRepository: AlarmRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Emits the current time now and every 15 s so "rings in ..." texts stay fresh while the screen is visible. */
    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(TICK_MILLIS)
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        alarmRepository.alarms,
        settingsRepository.settings,
        ticker,
    ) { alarms, settings, now ->
        val next = AlarmCalculator.nextAmong(alarms, now)?.let { (alarm, trigger) -> NextAlarm(alarm, trigger) }
        HomeUiState(loading = false, alarms = alarms, settings = settings, next = next, nowMillis = now)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setEnabled(alarmId: Long, enabled: Boolean) {
        launchSafely { alarmRepository.setEnabled(alarmId, enabled) }
    }

    private companion object {
        const val TICK_MILLIS = 15_000L
    }
}
