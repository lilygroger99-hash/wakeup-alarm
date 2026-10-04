package com.wakeup.alarm.data

import com.wakeup.alarm.alarm.AlarmScheduler
import com.wakeup.alarm.data.local.AlarmDao
import com.wakeup.alarm.data.local.toDomain
import com.wakeup.alarm.data.local.toEntity
import com.wakeup.alarm.domain.model.Alarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single source of truth for alarms. Every change is written to the database AND armed in AlarmManager through
 * [persistAndArm], so storage and the system schedule cannot drift apart.
 *
 * One-time alarm life cycle:  ON -> (rings) -> OFF.  Snoozing a one-time alarm turns it ON again with a snooze time.
 * Repeating alarm life cycle: when it rings, the next regular occurrence is armed immediately.
 */
class AlarmRepository(
    private val dao: AlarmDao,
    private val scheduler: AlarmScheduler,
) {
    val alarms: Flow<List<Alarm>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun get(id: Long): Alarm? = dao.getById(id)?.toDomain()

    suspend fun getAll(): List<Alarm> = dao.getAll().map { it.toDomain() }

    /** Creates (id == 0) or updates an alarm and arms it. Editing clears any pending snooze. */
    suspend fun save(alarm: Alarm): Long {
        val clean = alarm.sanitized().copy(snoozedUntilMillis = 0L, nextTriggerMillis = 0L)
        val rowId = dao.upsert(clean.toEntity())
        val saved = if (clean.id == 0L) clean.copy(id = rowId) else clean
        persistAndArm(saved)
        return saved.id
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        val alarm = get(id) ?: return
        persistAndArm(alarm.copy(enabled = enabled, snoozedUntilMillis = 0L))
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
        scheduler.cancel(id)
    }

    /**
     * Called when the system alarm for [id] goes off. Returns the alarm to ring, or `null` if it no longer exists
     * or is disabled (a stale trigger that must be ignored).
     */
    suspend fun onAlarmFired(id: Long, nowMillis: Long = System.currentTimeMillis()): Alarm? {
        val alarm = get(id) ?: return null
        if (!alarm.enabled) return null
        val fired = alarm.copy(snoozedUntilMillis = 0L)
        if (fired.isRepeating) {
            // +30 s guard: never re-arm the occurrence that is ringing right now, even if delivered a bit early.
            persistAndArm(fired, nowMillis + EARLY_DELIVERY_GUARD_MILLIS)
        } else {
            persistAndArm(fired.copy(enabled = false), nowMillis)
        }
        return fired
    }

    /** Snoozes alarm [id] for its configured duration and arms the snooze. Returns the updated alarm. */
    suspend fun snooze(id: Long, nowMillis: Long = System.currentTimeMillis()): Alarm? {
        val alarm = get(id) ?: return null
        val until = nowMillis + alarm.snoozeMinutes * 60_000L
        return persistAndArm(alarm.copy(enabled = true, snoozedUntilMillis = until), nowMillis)
    }

    /** Ends a snooze without ringing: repeating alarms go back to their normal schedule, one-time alarms turn off. */
    suspend fun cancelSnooze(id: Long) {
        val alarm = get(id) ?: return
        persistAndArm(
            alarm.copy(
                snoozedUntilMillis = 0L,
                enabled = alarm.enabled && alarm.isRepeating,
            ),
        )
    }

    /**
     * Re-arms every alarm from persistent storage. Safe to call any number of times (reboot, time change, app update,
     * permission change): PendingIntents are replaced, never duplicated.
     *
     * @param detectMissed true after a reboot or app update. Alarms whose armed time passed while the phone was off
     *   are then "consumed" (one-time alarms turn off, repeating alarms move on) and the ones that were missed
     *   recently are returned so the caller can tell the user. For plain clock/time-zone changes this is false and an
     *   alarm whose time has passed simply rolls to its next occurrence.
     * @return alarms that were missed while the device was off (empty unless [detectMissed]).
     */
    suspend fun rescheduleAll(nowMillis: Long = System.currentTimeMillis(), detectMissed: Boolean = false): List<Alarm> {
        val missed = mutableListOf<Alarm>()
        for (alarm in getAll()) {
            var current = alarm
            val armedFor = current.nextTriggerMillis
            if (current.enabled && armedFor in 1..nowMillis) {
                if (detectMissed) {
                    if (nowMillis - armedFor <= MISSED_WINDOW_MILLIS) missed.add(current)
                    current = current.copy(
                        snoozedUntilMillis = 0L,
                        enabled = current.isRepeating,
                    )
                } else if (current.snoozedUntilMillis in 1..nowMillis) {
                    current = current.copy(snoozedUntilMillis = 0L)
                }
            }
            persistAndArm(current, nowMillis)
        }
        return missed
    }

    /** Arms [alarm] (or cancels it when disabled) and stores it together with the time it is armed for. */
    private suspend fun persistAndArm(alarm: Alarm, nowMillis: Long = System.currentTimeMillis()): Alarm {
        val triggerAt = scheduler.schedule(alarm, nowMillis)
        val stored = alarm.copy(nextTriggerMillis = triggerAt ?: 0L)
        dao.upsert(stored.toEntity())
        return stored
    }

    private companion object {
        const val EARLY_DELIVERY_GUARD_MILLIS = 30_000L

        /** Alarms missed up to 12 hours ago are reported after a reboot; older ones are dropped silently. */
        const val MISSED_WINDOW_MILLIS = 12 * 60 * 60 * 1000L
    }
}
