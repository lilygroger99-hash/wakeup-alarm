package com.wakeup.alarm.alarm

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import com.wakeup.alarm.container
import com.wakeup.alarm.domain.model.Alarm
import com.wakeup.alarm.util.TimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground service that owns a ringing alarm: sound, vibration, wake lock and the ringing notification.
 *
 * It is started only by [AlarmReceiver] (a system alarm just fired, which Android allows to start a foreground
 * service) and is stopped as soon as the alarm is stopped, snoozed or has rung for [RING_TIMEOUT_MS].
 * The service never runs while no alarm is ringing, so there is no permanent background service.
 */
class AlarmService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: AlarmPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var timeoutJob: Job? = null
    private var actionInProgress = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, NO_ID) ?: NO_ID
        when (intent?.action) {
            ACTION_FIRE -> onFire(alarmId)
            ACTION_STOP -> onStop(alarmId)
            ACTION_SNOOZE -> onSnooze(alarmId)
            else -> if (_ringingAlarmId.value == null) stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        silence()
        _ringingAlarmId.value = null
        scope.cancel()
        super.onDestroy()
    }

    // ---- commands ----------------------------------------------------------------------------------------------

    private fun onFire(alarmId: Long) {
        if (alarmId == NO_ID) {
            // Required after startForegroundService(): promote briefly, then leave.
            enterForeground(AlarmNotifications.buildRinging(this, 0L, null, null))
            endRinging()
            return
        }
        // A different alarm is already ringing: silence it, the new one takes over.
        if (_ringingAlarmId.value != null && _ringingAlarmId.value != alarmId) silence()
        actionInProgress = false
        // Publish the id first so RingActivity (launched by the full-screen intent) knows an alarm is ringing.
        _ringingAlarmId.value = alarmId
        // Must be called within seconds of startForegroundService(); the full-screen intent is already attached.
        enterForeground(AlarmNotifications.buildRinging(this, alarmId, null, null))

        scope.launch {
            val container = this@AlarmService.container
            val alarm: Alarm? = try {
                container.alarmRepository.onAlarmFired(alarmId)
            } catch (e: Exception) {
                // Storage problem: still ring with defaults rather than staying silent.
                Log.e(TAG, "Could not load alarm $alarmId; ringing with defaults", e)
                Alarm(id = alarmId)
            }
            if (alarm == null) {
                // Deleted or disabled in the meantime: stale trigger.
                endRinging()
                return@launch
            }
            // The user may have pressed STOP while the alarm was loading.
            if (_ringingAlarmId.value != alarmId) return@launch

            val is24 = TimeFormatter.is24Hour(this@AlarmService, container.settingsRepository.current().timeFormat)
            val timeText = TimeFormatter.timeText(alarm.hour, alarm.minute, is24)
            getSystemService(NotificationManager::class.java)
                ?.notify(AlarmNotifications.ID_RINGING, AlarmNotifications.buildRinging(this@AlarmService, alarmId, alarm.label, timeText))
            AlarmNotifications.cancelSnoozed(this@AlarmService, alarmId)

            acquireWakeLock()
            player?.release()
            player = AlarmPlayer(this@AlarmService).also { it.start(alarm.soundUri, alarm.vibrate) }

            timeoutJob?.cancel()
            timeoutJob = launch {
                delay(RING_TIMEOUT_MS)
                AlarmNotifications.showMissed(this@AlarmService, alarm, is24)
                endRinging()
            }
        }
    }

    private fun onStop(alarmId: Long) {
        if (_ringingAlarmId.value != alarmId) {
            if (_ringingAlarmId.value == null) stopSelf()
            return
        }
        endRinging()
    }

    private fun onSnooze(alarmId: Long) {
        if (_ringingAlarmId.value != alarmId) {
            if (_ringingAlarmId.value == null) stopSelf()
            return
        }
        if (actionInProgress) return
        actionInProgress = true
        silence() // stop the noise immediately, persist afterwards
        scope.launch {
            try {
                val container = this@AlarmService.container
                val snoozed = container.alarmRepository.snooze(alarmId)
                if (snoozed != null) {
                    val is24 = TimeFormatter.is24Hour(this@AlarmService, container.settingsRepository.current().timeFormat)
                    AlarmNotifications.showSnoozed(this@AlarmService, snoozed, is24)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Snooze failed for alarm $alarmId", e)
            } finally {
                endRinging()
            }
        }
    }

    // ---- helpers -----------------------------------------------------------------------------------------------

    @SuppressLint("InlinedApi")
    private fun enterForeground(notification: Notification) {
        try {
            ServiceCompat.startForeground(
                this,
                AlarmNotifications.ID_RINGING,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } catch (e: Exception) {
            // The alarm must still ring even if the system refuses foreground promotion.
            Log.w(TAG, "startForeground failed", e)
        }
    }

    /** Stops sound, vibration and the wake lock but keeps the service alive. */
    private fun silence() {
        timeoutJob?.cancel()
        timeoutJob = null
        player?.release()
        player = null
        releaseWakeLock()
    }

    /** Fully ends the ringing state and the service. */
    private fun endRinging() {
        silence()
        _ringingAlarmId.value = null
        actionInProgress = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(PowerManager::class.java) ?: return
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeUp:ringing").apply {
            setReferenceCounted(false)
            acquire(RING_TIMEOUT_MS + 30_000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { lock -> if (lock.isHeld) runCatching { lock.release() } }
        wakeLock = null
    }

    companion object {
        const val ACTION_FIRE = "com.wakeup.alarm.action.FIRE"
        const val ACTION_STOP = "com.wakeup.alarm.action.STOP"
        const val ACTION_SNOOZE = "com.wakeup.alarm.action.SNOOZE"

        /** An alarm that nobody touches stops by itself after 10 minutes. */
        const val RING_TIMEOUT_MS = 10 * 60 * 1000L

        private const val NO_ID = -1L
        private const val TAG = "AlarmService"

        private val _ringingAlarmId = MutableStateFlow<Long?>(null)

        /** Id of the alarm that is ringing right now, or `null`. Observed by [RingActivity] and MainActivity. */
        val ringingAlarmId: StateFlow<Long?> = _ringingAlarmId.asStateFlow()
    }
}
