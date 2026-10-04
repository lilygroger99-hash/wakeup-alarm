package com.wakeup.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wakeup.alarm.container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles notification buttons that do not need a screen, currently only "Cancel snooze". */
class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CANCEL_SNOOZE) return
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0L) return

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                appContext.container.alarmRepository.cancelSnooze(alarmId)
                AlarmNotifications.cancelSnoozed(appContext, alarmId)
            } catch (e: Exception) {
                Log.e(TAG, "Cancel snooze failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_CANCEL_SNOOZE = "com.wakeup.alarm.action.CANCEL_SNOOZE"
        private const val TAG = "AlarmActionReceiver"
    }
}
