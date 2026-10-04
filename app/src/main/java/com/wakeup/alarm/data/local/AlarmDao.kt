package com.wakeup.alarm.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms ORDER BY hour, minute, id")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms ORDER BY hour, minute, id")
    suspend fun getAll(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Long): AlarmEntity?

    /** Inserts a new alarm (id = 0) or replaces the existing row with the same id. Returns the row id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AlarmEntity): Long

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: Long)
}
