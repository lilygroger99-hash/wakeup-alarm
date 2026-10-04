package com.wakeup.alarm.domain

import com.wakeup.alarm.domain.model.Alarm
import com.wakeup.alarm.domain.model.RepeatDays
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmCalculatorTest {

    private val utc = ZoneId.of("UTC")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int, zone: ZoneId = utc) =
        ZonedDateTime.of(LocalDateTime.of(year, month, day, hour, minute), zone)

    // 2026-10-03 is a Saturday.
    @Test
    fun oneTime_laterToday_ringsToday() {
        val alarm = Alarm(hour = 18, minute = 30)
        val next = AlarmCalculator.nextTrigger(alarm, at(2026, 10, 3, 12, 0))
        assertEquals(at(2026, 10, 3, 18, 30), next)
    }

    @Test
    fun oneTime_alreadyPassedToday_ringsTomorrow() {
        val alarm = Alarm(hour = 7, minute = 0)
        val next = AlarmCalculator.nextTrigger(alarm, at(2026, 10, 3, 12, 0))
        assertEquals(at(2026, 10, 4, 7, 0), next)
    }

    @Test
    fun oneTime_exactlyNow_ringsTomorrow() {
        val alarm = Alarm(hour = 12, minute = 0)
        val next = AlarmCalculator.nextTrigger(alarm, at(2026, 10, 3, 12, 0))
        assertEquals(at(2026, 10, 4, 12, 0), next)
    }

    @Test
    fun crossingMidnight_lateNightAlarm_ringsBeforeMidnightOrNextDay() {
        val alarm = Alarm(hour = 0, minute = 15)
        val next = AlarmCalculator.nextTrigger(alarm, at(2026, 10, 3, 23, 50))
        assertEquals(at(2026, 10, 4, 0, 15), next)
    }

    @Test
    fun weekdays_onFridayEvening_ringsMonday() {
        val alarm = Alarm(hour = 7, minute = 0, repeatDays = RepeatDays.WEEKDAYS)
        // 2026-10-02 is a Friday.
        val next = AlarmCalculator.nextTrigger(alarm, at(2026, 10, 2, 18, 0))
        assertEquals(at(2026, 10, 5, 7, 0), next)
        assertEquals(DayOfWeek.MONDAY, next!!.dayOfWeek)
    }

    @Test
    fun singleWeekday_passedToday_ringsNextWeek() {
        val saturdayOnly = RepeatDays.bit(DayOfWeek.SATURDAY)
        val alarm = Alarm(hour = 9, minute = 0, repeatDays = saturdayOnly)
        val next = AlarmCalculator.nextTrigger(alarm, at(2026, 10, 3, 12, 0))
        assertEquals(at(2026, 10, 10, 9, 0), next)
    }

    @Test
    fun everyDay_laterToday_ringsToday() {
        val alarm = Alarm(hour = 20, minute = 0, repeatDays = RepeatDays.ALL)
        assertEquals(at(2026, 10, 3, 20, 0), AlarmCalculator.nextTrigger(alarm, at(2026, 10, 3, 12, 0)))
    }

    @Test
    fun snooze_inFuture_winsOverSchedule() {
        val now = at(2026, 10, 3, 7, 0)
        val until = now.plusMinutes(5).toInstant().toEpochMilli()
        val alarm = Alarm(hour = 7, minute = 0, repeatDays = RepeatDays.ALL, snoozedUntilMillis = until)
        assertEquals(now.plusMinutes(5).toInstant(), AlarmCalculator.nextTrigger(alarm, now)!!.toInstant())
    }

    @Test
    fun snooze_inPast_isIgnored() {
        val now = at(2026, 10, 3, 12, 0)
        val alarm = Alarm(hour = 7, minute = 0, snoozedUntilMillis = now.minusMinutes(1).toInstant().toEpochMilli())
        assertEquals(at(2026, 10, 4, 7, 0), AlarmCalculator.nextTrigger(alarm, now))
    }

    @Test
    fun dstSpringForward_nonExistentTime_movesForward() {
        val newYork = ZoneId.of("America/New_York")
        // 2026-03-08: clocks jump from 02:00 to 03:00, so 02:30 does not exist that day.
        val alarm = Alarm(hour = 2, minute = 30)
        val next = AlarmCalculator.nextTrigger(alarm, at(2026, 3, 8, 0, 30, newYork))
        assertNotNull(next)
        assertEquals(3, next!!.hour)
        assertEquals(30, next.minute)
        assertEquals(8, next.dayOfMonth)
    }

    @Test
    fun differentTimeZone_isResolvedInThatZone() {
        val karachi = ZoneId.of("Asia/Karachi")
        val alarm = Alarm(hour = 6, minute = 0)
        val next = AlarmCalculator.nextTrigger(alarm, at(2026, 10, 3, 23, 0, karachi))
        assertEquals(at(2026, 10, 4, 6, 0, karachi), next)
    }

    @Test
    fun nextAmong_picksEarliestEnabled_ignoresDisabled() {
        val now = at(2026, 10, 3, 12, 0).toInstant().toEpochMilli()
        val early = Alarm(id = 1, hour = 13, minute = 0, enabled = false)
        val a = Alarm(id = 2, hour = 15, minute = 0)
        val b = Alarm(id = 3, hour = 14, minute = 0)
        val result = AlarmCalculator.nextAmong(listOf(early, a, b), now, utc)
        assertEquals(3L, result!!.first.id)
        assertNull(AlarmCalculator.nextAmong(listOf(early), now, utc))
    }

    @Test
    fun sanitized_clampsInvalidValues() {
        val dirty = Alarm(hour = 99, minute = -4, label = "  Wake  ", repeatDays = 0xFFFF, snoozeMinutes = 0)
        val clean = dirty.sanitized()
        assertEquals(23, clean.hour)
        assertEquals(0, clean.minute)
        assertEquals("Wake", clean.label)
        assertEquals(RepeatDays.ALL, clean.repeatDays)
        assertEquals(1, clean.snoozeMinutes)
    }
}
