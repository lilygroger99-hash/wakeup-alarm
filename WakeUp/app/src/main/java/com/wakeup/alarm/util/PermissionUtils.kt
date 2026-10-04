package com.wakeup.alarm.util

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri

/** What the alarm needs from the user's permission settings. */
data class PermissionStatus(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val fullScreen: Boolean,
) {
    val allGranted: Boolean get() = notifications && exactAlarms && fullScreen
}

/**
 * Permission checks and shortcuts to the right system settings page.
 *
 * Only genuinely needed access is requested:
 *  - Notifications (Android 13+ runtime permission): the ringing notification with STOP / SNOOZE.
 *  - Exact alarms: pre-granted on Android 13+ through USE_EXACT_ALARM (alarm app); on Android 12/12L the user
 *    grants "Alarms & reminders" in settings.
 *  - Full-screen intent (Android 14+): lets the ringing screen appear over the lock screen.
 */
object PermissionUtils {

    fun status(context: Context): PermissionStatus = PermissionStatus(
        notifications = notificationsAllowed(context),
        exactAlarms = exactAlarmsAllowed(context),
        fullScreen = fullScreenAllowed(context),
    )

    fun notificationsAllowed(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun exactAlarmsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(AlarmManager::class.java) ?: return false
        return manager.canScheduleExactAlarms()
    }

    fun fullScreenAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return manager.canUseFullScreenIntent()
    }

    /** True on Android 13+, where POST_NOTIFICATIONS must be requested at runtime. */
    val notificationsNeedRuntimePermission: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        startSafely(context, intent)
    }

    @SuppressLint("InlinedApi")
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())
        startSafely(context, intent)
    }

    @SuppressLint("InlinedApi")
    fun openFullScreenIntentSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, "package:${context.packageName}".toUri())
        startSafely(context, intent)
    }

    private fun startSafely(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Settings page unavailable, opening app details instead", e)
            try {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()),
                )
            } catch (e2: Exception) {
                Log.e(TAG, "No settings screen could be opened", e2)
            }
        }
    }

    private const val TAG = "PermissionUtils"
}
