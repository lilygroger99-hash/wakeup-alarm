package com.wakeup.alarm.domain.model

import java.time.DayOfWeek

/**
 * Plain domain model of one alarm. Contains no Android or database types.
 *
 * @property repeatDays bit mask of weekdays, see [RepeatDays]. `0` means a one-time alarm.
 * @property soundUri content URI of the chosen sound, or an empty string for the system default alarm sound.
 * @property snoozedUntilMillis epoch millis at which a snoozed alarm rings again, `0` when not snoozed.
 * @property nextTriggerMillis epoch millis the system alarm is currently armed for, `0` when nothing is armed.
 *   Stored so that after a reboot we can tell which alarms were missed while the phone was off.
 */
data class Alarm(
    val id: Long = 0L,
    val hour: Int = 7,
    val minute: Int = 0,
    val label: String = "",
    val enabled: Boolean = true,
    val repeatDays: Int = RepeatDays.NONE,
    val soundUri: String = "",
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    val snoozedUntilMillis: Long = 0L,
    val nextTriggerMillis: Long = 0L,
) {
    val isRepeating: Boolean get() = repeatDays != RepeatDays.NONE

    /** True while a snooze is pending at [nowMillis]. */
    fun isSnoozedAt(nowMillis: Long): Boolean = snoozedUntilMillis > nowMillis

    /** Returns a copy with every field forced into its valid range. Used for all data entering or leaving storage. */
    fun sanitized(): Alarm = copy(
        hour = hour.coerceIn(0, 23),
        minute = minute.coerceIn(0, 59),
        label = label.trim().take(MAX_LABEL_LENGTH),
        repeatDays = repeatDays and RepeatDays.ALL,
        snoozeMinutes = snoozeMinutes.coerceIn(1, MAX_SNOOZE_MINUTES),
        snoozedUntilMillis = snoozedUntilMillis.coerceAtLeast(0L),
        nextTriggerMillis = nextTriggerMillis.coerceAtLeast(0L),
    )

    companion object {
        const val MAX_LABEL_LENGTH = 60
        const val DEFAULT_SNOOZE_MINUTES = 5
        const val MAX_SNOOZE_MINUTES = 60

        /** Snooze durations offered in the UI (minutes). */
        val SNOOZE_OPTIONS: List<Int> = listOf(1, 5, 10, 15, 20, 30)
    }
}

/** Helpers for the weekday bit mask: Monday = bit 0 ... Sunday = bit 6. */
object RepeatDays {
    const val NONE = 0
    const val ALL = 0b1111111
    const val WEEKDAYS = 0b0011111
    const val WEEKENDS = 0b1100000

    /** Days in the order they are shown in the UI (Monday first). */
    val ORDERED: List<DayOfWeek> = DayOfWeek.values().toList()

    fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)

    fun contains(mask: Int, day: DayOfWeek): Boolean = mask and bit(day) != 0

    fun toggle(mask: Int, day: DayOfWeek): Int = (mask xor bit(day)) and ALL
}
