package com.wakeup.alarm.data.stopwatch

import android.content.Context
import android.os.SystemClock
import com.wakeup.alarm.domain.StopwatchState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds the one stopwatch of the app. It lives in the app container, so it keeps running while the user leaves the
 * screen, and it is saved to a private preferences file so it also survives the process being killed.
 * Only ever called from the main thread.
 */
class StopwatchController(context: Context) {

    private val prefs = context.getSharedPreferences("stopwatch", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<StopwatchState> = _state.asStateFlow()

    fun start() = change { it.started(SystemClock.elapsedRealtime()) }

    fun pause() = change { it.paused(SystemClock.elapsedRealtime()) }

    fun lap() = change { it.lapped(SystemClock.elapsedRealtime()) }

    fun reset() = change { StopwatchState() }

    private fun change(transform: (StopwatchState) -> StopwatchState) {
        _state.update(transform)
        save(_state.value)
    }

    private fun save(state: StopwatchState) {
        prefs.edit()
            .putLong(KEY_ACCUMULATED, state.accumulatedMs)
            .putLong(KEY_RUNNING_SINCE, state.runningSince ?: NOT_RUNNING)
            .putString(KEY_LAPS, state.laps.joinToString(","))
            .apply()
    }

    private fun load(): StopwatchState {
        val accumulated = prefs.getLong(KEY_ACCUMULATED, 0L).coerceAtLeast(0L)
        val since = prefs.getLong(KEY_RUNNING_SINCE, NOT_RUNNING)
        val laps = prefs.getString(KEY_LAPS, "").orEmpty().split(',').mapNotNull { it.toLongOrNull() }
        // The monotonic clock restarts at zero after a reboot; a start time in the "future" means the phone restarted.
        val validSince = since.takeIf { it != NOT_RUNNING && it <= SystemClock.elapsedRealtime() }
        return StopwatchState(accumulatedMs = accumulated, runningSince = validSince, laps = laps)
    }

    private companion object {
        const val KEY_ACCUMULATED = "accumulated"
        const val KEY_RUNNING_SINCE = "running_since"
        const val KEY_LAPS = "laps"
        const val NOT_RUNNING = -1L
    }
}
