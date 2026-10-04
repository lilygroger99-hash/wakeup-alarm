package com.wakeup.alarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import com.wakeup.alarm.MainActivity
import com.wakeup.alarm.domain.AlarmCalculator
import com.wakeup.alarm.domain.model.Alarm
import java.time.ZoneId

/**
 * Talks to [AlarmManager]. One PendingIntent per alarm id; scheduling again with the same id REPLACES the previous
 * one, so alarms can never be duplicated (including after reboot re-scheduling).
 *
 * Why setAlarmClock(): it is the API Android designates for user-visible alarms. It fires at the exact time,
 * is exempt from Doze and App Standby, makes the system show the alarm icon / "next alarm" on the lock screen, and
 * lets the system start our foreground ringing service. It requires the exact-alarm permission
 * (USE_EXACT_ALARM on 13+, SCHEDULE_EXACT_ALARM on 12/12L). If that permission is missing we fall back to
 * setAndAllowWhileIdle() (inexact, may be delayed) instead of crashing or silently dropping the alarm.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? = context.getSystemService(AlarmManager::class.java)

    /** True when exact alarms are allowed on this device right now. */
    fun canScheduleExact(): Boolean {
        val manager = alarmManager ?: return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
    }

    /**
     * Arms (or cancels, if disabled) the system alarm for [alarm].
     * @return the scheduled trigger time in epoch millis, or `null` if nothing is scheduled.
     */
    fun schedule(alarm: Alarm, nowMillis: Long = System.currentTimeMillis()): Long? {
        val manager = alarmManager
        if (manager == null) {
            Log.w(TAG, "AlarmManager unavailable; cannot schedule alarm ${alarm.id}")
            return null
        }
        if (!alarm.enabled || alarm.id == 0L) {
            cancel(alarm.id)
            return null
        }
        val triggerAt = AlarmCalculator.nextTriggerMillis(alarm, nowMillis, ZoneId.systemDefault())
        if (triggerAt == null) {
            cancel(alarm.id)
            return null
        }

        val operation = firePendingIntent(alarm.id)
        try {
            if (canScheduleExact()) {
                manager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showAppPendingIntent()), operation)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            }
        } catch (e: SecurityException) {
            // Permission revoked between the check and the call. Degrade gracefully.
            Log.w(TAG, "Exact alarm denied, using inexact alarm", e)
            runCatching { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation) }
        }
        return triggerAt
    }

    fun cancel(alarmId: Long) {
        val manager = alarmManager ?: return
        val operation = firePendingIntent(alarmId)
        manager.cancel(operation)
        operation.cancel()
    }

    private fun firePendingIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE)
            // The data URI makes every alarm's Intent unique (PendingIntent identity ignores extras).
            .setData("wakeup://alarm/$alarmId".toUri())
            .putExtra(EXTRA_ALARM_ID, alarmId)
        return PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun showAppPendingIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val EXTRA_ALARM_ID = "com.wakeup.alarm.extra.ALARM_ID"
        private const val TAG = "AlarmScheduler"
    }
}
