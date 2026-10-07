package com.wakeup.alarm.alarm

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wakeup.alarm.container
import com.wakeup.alarm.data.settings.AppSettings
import com.wakeup.alarm.data.settings.ThemeMode
import com.wakeup.alarm.domain.model.Alarm
import com.wakeup.alarm.ui.ring.RingScreen
import com.wakeup.alarm.ui.theme.WakeUpTheme
import com.wakeup.alarm.util.TimeFormatter
import kotlinx.coroutines.launch

/**
 * Full-screen ringing UI. It is launched by the full-screen intent of the ringing notification, shows over the
 * lock screen, turns the screen on, and closes itself as soon as [AlarmService] reports that nothing rings anymore.
 * The big STOP / SNOOZE buttons send commands to the service, which owns the actual alarm state.
 */
class RingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        showOverLockScreen()

        // Close when the alarm is stopped (from here, from the notification, or by the 10 minute timeout).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                AlarmService.ringingAlarmId.collect { id -> if (id == null) finishAndRemoveTask() }
            }
        }

        setContent {
            val ringingId by AlarmService.ringingAlarmId.collectAsStateWithLifecycle()
            val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(AppSettings())

            val alarm by produceState<Alarm?>(initialValue = null, key1 = ringingId) {
                value = ringingId?.let { id -> runCatching { container.alarmRepository.get(id) }.getOrNull() }
            }

            WakeUpTheme(themeMode = ThemeMode.DARK, dynamicColor = false) {
                RingScreen(
                    alarm = alarm,
                    is24Hour = TimeFormatter.is24Hour(this@RingActivity, settings.timeFormat),
                    onStop = { sendToService(AlarmService.ACTION_STOP) },
                    onSnooze = { sendToService(AlarmService.ACTION_SNOOZE) },
                )
            }
        }
    }

    private fun sendToService(action: String) {
        val id = AlarmService.ringingAlarmId.value ?: run {
            finishAndRemoveTask()
            return
        }
        // We are in the foreground, so a plain startService() is allowed; the service is already running.
        startService(
            Intent(this, AlarmService::class.java)
                .setAction(action)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, id),
        )
    }

    @Suppress("DEPRECATION")
    private fun showOverLockScreen() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }

    companion object {
        fun createIntent(context: Context, alarmId: Long): Intent =
            Intent(context, RingActivity::class.java)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }
}
