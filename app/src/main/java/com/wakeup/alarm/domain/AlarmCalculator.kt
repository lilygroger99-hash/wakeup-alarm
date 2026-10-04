package com.wakeup.alarm.domain

import com.wakeup.alarm.domain.model.Alarm
import com.wakeup.alarm.domain.model.RepeatDays
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure time logic: when does an alarm ring next?
 *
 * Alarms store a wall-clock time (hour:minute) and are resolved against the CURRENT time zone every time they are
 * scheduled. That is why [com.wakeup.alarm.alarm.SystemEventReceiver] re-schedules everything when the time zone or
 * the clock changes. Daylight-saving transitions are handled by [ZonedDateTime.of]: a time that does not exist
 * (spring-forward gap) is moved forward by the length of the gap, an ambiguous time (fall-back overlap) uses the
 * earlier instant.
 */
object AlarmCalculator {

    /**
     * Next time [alarm] should ring strictly after [now], or `null` if it has none (never happens for valid data).
     * A pending snooze that is still in the future wins over the regular schedule.
     */
    fun nextTrigger(alarm: Alarm, now: ZonedDateTime): ZonedDateTime? {
        val nowMillis = now.toInstant().toEpochMilli()
        if (alarm.snoozedUntilMillis > nowMillis) {
            return Instant.ofEpochMilli(alarm.snoozedUntilMillis).atZone(now.zone)
        }

        val time = LocalTime.of(alarm.hour.coerceIn(0, 23), alarm.minute.coerceIn(0, 59))
        val today = now.toLocalDate()
        // Offsets 0..7 cover a full week plus "same weekday next week" for repeating alarms that already rang today.
        for (offset in 0..7) {
            val date = today.plusDays(offset.toLong())
            if (alarm.isRepeating && !RepeatDays.contains(alarm.repeatDays, date.dayOfWeek)) continue
            val candidate = ZonedDateTime.of(date, time, now.zone)
            if (candidate.isAfter(now)) return candidate
        }
        return null
    }

    /** Convenience overload working with epoch millis. */
    fun nextTriggerMillis(
        alarm: Alarm,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long? = nextTrigger(alarm, Instant.ofEpochMilli(nowMillis).atZone(zone))?.toInstant()?.toEpochMilli()

    /** The enabled alarm that rings first, with its trigger time, or `null` if none is enabled. */
    fun nextAmong(
        alarms: List<Alarm>,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Pair<Alarm, Long>? = alarms
        .filter { it.enabled }
        .mapNotNull { alarm -> nextTriggerMillis(alarm, nowMillis, zone)?.let { alarm to it } }
        .minByOrNull { it.second }
}
