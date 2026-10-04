package com.wakeup.alarm.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wakeup.alarm.domain.model.Alarm

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val hour: Int,
    val minute: Int,
    val label: String,
    val enabled: Boolean,
    @ColumnInfo(name = "repeat_days") val repeatDays: Int,
    @ColumnInfo(name = "sound_uri") val soundUri: String,
    val vibrate: Boolean,
    @ColumnInfo(name = "snooze_minutes") val snoozeMinutes: Int,
    @ColumnInfo(name = "snoozed_until") val snoozedUntilMillis: Long,
    @ColumnInfo(name = "next_trigger") val nextTriggerMillis: Long,
)

fun AlarmEntity.toDomain(): Alarm = Alarm(
    id = id,
    hour = hour,
    minute = minute,
    label = label,
    enabled = enabled,
    repeatDays = repeatDays,
    soundUri = soundUri,
    vibrate = vibrate,
    snoozeMinutes = snoozeMinutes,
    snoozedUntilMillis = snoozedUntilMillis,
    nextTriggerMillis = nextTriggerMillis,
).sanitized()

fun Alarm.toEntity(): AlarmEntity = sanitized().let {
    AlarmEntity(
        id = it.id,
        hour = it.hour,
        minute = it.minute,
        label = it.label,
        enabled = it.enabled,
        repeatDays = it.repeatDays,
        soundUri = it.soundUri,
        vibrate = it.vibrate,
        snoozeMinutes = it.snoozeMinutes,
        snoozedUntilMillis = it.snoozedUntilMillis,
        nextTriggerMillis = it.nextTriggerMillis,
    )
}
