package com.wakeup.alarm.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopwatchTest {

    @Test
    fun formatsMinutesSecondsAndHundredths() {
        assertEquals("00:00.00", StopwatchFormat.format(0))
        assertEquals("00:09.99", StopwatchFormat.format(9_999))
        assertEquals("01:23.45", StopwatchFormat.format(83_450))
        assertEquals("59:59.99", StopwatchFormat.format(3_599_999))
    }

    @Test
    fun formatsHoursAndIgnoresNegativeValues() {
        assertEquals("1:02:03.45", StopwatchFormat.format(3_723_450))
        assertEquals("00:00.00", StopwatchFormat.format(-5))
    }

    @Test
    fun runsPausesAndResumesWithoutLosingTime() {
        var s = StopwatchState().started(now = 1_000)
        assertTrue(s.isRunning)
        assertEquals(500L, s.elapsed(now = 1_500))
        s = s.paused(now = 2_000)
        assertFalse(s.isRunning)
        assertEquals(1_000L, s.elapsed(now = 9_999))   // paused: time stands still
        s = s.started(now = 10_000)
        assertEquals(1_300L, s.elapsed(now = 10_300))
    }

    @Test
    fun startAndPauseAreIdempotent() {
        val running = StopwatchState().started(100)
        assertEquals(running, running.started(500))
        val paused = running.paused(200)
        assertEquals(paused, paused.paused(900))
    }

    @Test
    fun lapsRecordCumulativeTimeAndLapLength() {
        var s = StopwatchState().started(0)
        s = s.lapped(1_000).lapped(2_500).lapped(2_600)
        assertEquals(listOf(1_000L, 2_500L, 2_600L), s.laps)
        assertEquals(1_000L, s.lapDuration(0))
        assertEquals(1_500L, s.lapDuration(1))
        assertEquals(100L, s.lapDuration(2))
    }

    @Test
    fun lapIsIgnoredWhilePausedAndResetClearsEverything() {
        val paused = StopwatchState().started(0).paused(500)
        assertEquals(paused, paused.lapped(900))
        assertEquals(StopwatchState(), StopwatchState())
        assertFalse(StopwatchState().hasTime)
        assertTrue(paused.hasTime)
    }
}
