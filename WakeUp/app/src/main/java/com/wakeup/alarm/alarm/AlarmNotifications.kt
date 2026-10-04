package com.wakeup.alarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.wakeup.alarm.MainActivity
import com.wakeup.alarm.R
import com.wakeup.alarm.domain.model.Alarm
import com.wakeup.alarm.util.TimeFormatter

/**
 * All notification channels and notifications of the app.
 *
 * - Ringing: IMPORTANCE_HIGH + category ALARM + full-screen intent. The channel itself is silent because the sound and
 *   vibration are played by [AlarmPlayer] (looping, alarm audio stream, works with Do Not Disturb "alarms allowed").
 * - Snoozed: low importance, informs the user when the alarm will ring again and allows cancelling the snooze.
 * - Missed: shown when an alarm rang for 10 minutes without being touched.
 */
object AlarmNotifications {

    const val CHANNEL_RINGING = "alarm_ringing"
    const val CHANNEL_SNOOZED = "alarm_snoozed"
    const val CHANNEL_MISSED = "alarm_missed"

    const val ID_RINGING = 1001

    private fun snoozedId(alarmId: Long) = 20_000 + (alarmId % 10_000).toInt()
    private fun missedId(alarmId: Long) = 30_000 + (alarmId % 10_000).toInt()

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val ringing = NotificationChannel(
            CHANNEL_RINGING,
            context.getString(R.string.channel_ringing_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_ringing_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val snoozed = NotificationChannel(
            CHANNEL_SNOOZED,
            context.getString(R.string.channel_snoozed_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_snoozed_description)
            setShowBadge(false)
        }

        val missed = NotificationChannel(
            CHANNEL_MISSED,
            context.getString(R.string.channel_missed_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_missed_description)
        }

        manager.createNotificationChannels(listOf(ringing, snoozed, missed))
    }

    /**
     * Notification for the foreground ringing service. [label] and [timeText] may be null while the alarm is still
     * being loaded; the same notification id is updated afterwards.
     */
    fun buildRinging(context: Context, alarmId: Long, label: String?, timeText: String?): Notification {
        val ringScreen = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            RingActivity.createIntent(context, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = label?.takeIf { it.isNotBlank() } ?: context.getString(R.string.ring_default_title)

        return NotificationCompat.Builder(context, CHANNEL_RINGING)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(title)
            .setContentText(timeText)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(ringScreen)
            .setFullScreenIntent(ringScreen, true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, context.getString(R.string.action_snooze), serviceAction(context, AlarmService.ACTION_SNOOZE, alarmId))
            .addAction(0, context.getString(R.string.action_stop), serviceAction(context, AlarmService.ACTION_STOP, alarmId))
            .build()
    }

    fun showSnoozed(context: Context, alarm: Alarm, is24Hour: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val until = TimeFormatter.timeTextFromMillis(alarm.snoozedUntilMillis, is24Hour)
        val cancel = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            Intent(context, AlarmActionReceiver::class.java)
                .setAction(AlarmActionReceiver.ACTION_CANCEL_SNOOZE)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_SNOOZED)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(
                alarm.label.takeIf { it.isNotBlank() }
                    ?.let { context.getString(R.string.notification_snoozed_title_with_label, it) }
                    ?: context.getString(R.string.notification_snoozed_title),
            )
            .setContentText(context.getString(R.string.notification_snoozed_text, until))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(alarm.snoozedUntilMillis)
            .setContentIntent(openAppIntent(context))
            .addAction(0, context.getString(R.string.action_cancel_snooze), cancel)
            .build()
        manager.notify(snoozedId(alarm.id), notification)
    }

    fun cancelSnoozed(context: Context, alarmId: Long) {
        context.getSystemService(NotificationManager::class.java)?.cancel(snoozedId(alarmId))
    }

    fun showMissed(context: Context, alarm: Alarm, is24Hour: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val time = TimeFormatter.timeText(alarm.hour, alarm.minute, is24Hour)
        val text = alarm.label.takeIf { it.isNotBlank() }?.let { "$time · $it" } ?: time
        val notification = NotificationCompat.Builder(context, CHANNEL_MISSED)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(context.getString(R.string.notification_missed_title))
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        manager.notify(missedId(alarm.id), notification)
    }

    private fun serviceAction(context: Context, action: String, alarmId: Long): PendingIntent =
        PendingIntent.getService(
            context,
            alarmId.toInt(),
            Intent(context, AlarmService::class.java)
                .setAction(action)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
