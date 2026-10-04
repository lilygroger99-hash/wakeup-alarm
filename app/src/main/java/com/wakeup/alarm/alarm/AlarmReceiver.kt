package com.wakeup.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Receives the broadcast that AlarmManager sends at the alarm time and hands over to [AlarmService].
 * Does no work itself, so it finishes well within the receiver time limit.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0L) return

        val serviceIntent = Intent(context, AlarmService::class.java)
            .setAction(AlarmService.ACTION_FIRE)
            .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not start AlarmService for alarm $alarmId", e)
        }
    }

    companion object {
        const val ACTION_FIRE = "com.wakeup.alarm.action.ALARM_FIRE"
        private const val TAG = "AlarmReceiver"
    }
}
