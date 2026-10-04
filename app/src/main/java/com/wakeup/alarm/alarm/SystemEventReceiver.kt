package com.wakeup.alarm.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wakeup.alarm.container
import com.wakeup.alarm.util.TimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms all alarms after anything that clears or invalidates AlarmManager's schedule:
 *  - device reboot (locked boot, so alarms work even before the first unlock; and normal boot),
 *  - the wall clock or time zone changing (alarms are wall-clock based and must be re-resolved),
 *  - the app being updated (the system removes its alarms),
 *  - the user granting or revoking the exact-alarm permission on Android 12.
 *
 * Scheduling is idempotent (one PendingIntent per alarm id), so receiving several of these in a row is harmless.
 * After a reboot or update, alarms that should have rung while the phone was off are reported as "missed".
 */
class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) return

        val detectMissed = action in MISSED_DETECTION_ACTIONS
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = appContext.container
                val missed = container.alarmRepository.rescheduleAll(detectMissed = detectMissed)
                if (missed.isNotEmpty()) {
                    val is24 = TimeFormatter.is24Hour(appContext, container.settingsRepository.current().timeFormat)
                    missed.forEach { AlarmNotifications.showMissed(appContext, it, is24) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Re-scheduling after $action failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "SystemEventReceiver"

        val MISSED_DETECTION_ACTIONS = setOf(
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )

        val HANDLED_ACTIONS = MISSED_DETECTION_ACTIONS + setOf(
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
