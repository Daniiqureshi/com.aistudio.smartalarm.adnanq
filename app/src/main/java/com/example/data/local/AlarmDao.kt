package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms ORDER BY triggerTime ASC")
    fun getAllAlarmsFlow(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 AND triggerTime >= :currentTime ORDER BY triggerTime ASC")
    fun getActiveFutureAlarmsFlow(currentTime: Long): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 AND triggerTime >= :currentTime ORDER BY triggerTime ASC LIMIT 1")
    fun getNextUpcomingAlarmFlow(currentTime: Long): Flow<AlarmEntity?>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 ORDER BY triggerTime ASC")
    suspend fun getAllEnabledAlarms(): List<AlarmEntity>

    @Query("SELECT * FROM alarms")
    suspend fun getAllAlarmsList(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alarms: List<AlarmEntity>)

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: Long)

    @Query("UPDATE alarms SET isEnabled = :isEnabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateAlarmStatus(id: Long, isEnabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE alarms SET triggerTime = :newTriggerTime, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTriggerTime(id: Long, newTriggerTime: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM alarms WHERE isSynced = 0")
    suspend fun getUnsyncedAlarms(): List<AlarmEntity>

    @Query("UPDATE alarms SET isSynced = 1, firestoreId = :firestoreId WHERE id = :localId")
    suspend fun markAsSynced(localId: Long, firestoreId: String)
}
