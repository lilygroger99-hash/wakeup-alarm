package com.wakeup.alarm.domain

import java.util.Locale

/**
 * Pure stopwatch state (no Android types, so it is unit tested). Times are milliseconds on a monotonic clock
 * (`SystemClock.elapsedRealtime()` in the app), which is not affected by the user changing the date or time.
 *
 * @param accumulatedMs time collected before the current run (while paused it is the whole elapsed time)
 * @param runningSince clock value when the current run started, or null when paused
 * @param laps cumulative elapsed time at each lap press, oldest first
 */
data class StopwatchState(
    val accumulatedMs: Long = 0L,
    val runningSince: Long? = null,
    val laps: List<Long> = emptyList(),
) {
    val isRunning: Boolean get() = runningSince != null

    val hasTime: Boolean get() = accumulatedMs > 0L || isRunning

    fun elapsed(now: Long): Long = accumulatedMs + (runningSince?.let { (now - it).coerceAtLeast(0L) } ?: 0L)

    fun started(now: Long): StopwatchState = if (isRunning) this else copy(runningSince = now)

    fun paused(now: Long): StopwatchState =
        if (isRunning) copy(accumulatedMs = elapsed(now), runningSince = null) else this

    fun lapped(now: Long): StopwatchState =
        if (isRunning && laps.size < MAX_LAPS) copy(laps = laps + elapsed(now)) else this

    /** Length of lap number [index] (0 = the first lap). */
    fun lapDuration(index: Int): Long = laps[index] - (if (index == 0) 0L else laps[index - 1])

    companion object {
        const val MAX_LAPS = 999
    }
}

object StopwatchFormat {
    /** 83_450 -> "01:23.45", 3_723_450 -> "1:02:03.45" (hundredths, rounded down). */
    fun format(millis: Long): String {
        val t = millis.coerceAtLeast(0L)
        val hundredths = (t / 10) % 100
        val seconds = (t / 1000) % 60
        val minutes = (t / 60_000) % 60
        val hours = t / 3_600_000
        return if (hours > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d.%02d", hours, minutes, seconds, hundredths)
        } else {
            String.format(Locale.ROOT, "%02d:%02d.%02d", minutes, seconds, hundredths)
        }
    }
}
