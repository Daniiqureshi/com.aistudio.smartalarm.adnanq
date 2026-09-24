package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.AlarmDao
import com.example.data.local.AlarmEntity
import com.example.domain.model.Alarm
import com.example.domain.model.AlarmCategory
import com.example.domain.model.RepeatType
import com.example.domain.model.VibrationPattern
import com.example.domain.scheduler.AlarmScheduler
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AlarmRepository(
    private val context: Context,
    private val alarmDao: AlarmDao,
    private val scheduler: AlarmScheduler
) {

    private val moshi = Moshi.Builder().build()
    private val listType = Types.newParameterizedType(List::class.java, Map::class.java)
    private val jsonAdapter = moshi.adapter<List<Map<String, Any?>>>(listType)

    fun getAllAlarmsFlow(): Flow<List<Alarm>> {
        return alarmDao.getAllAlarmsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getActiveFutureAlarmsFlow(now: Long = System.currentTimeMillis()): Flow<List<Alarm>> {
        return alarmDao.getActiveFutureAlarmsFlow(now).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getNextUpcomingAlarmFlow(now: Long = System.currentTimeMillis()): Flow<Alarm?> {
        return alarmDao.getNextUpcomingAlarmFlow(now).map { entity ->
            entity?.toDomain()
        }
    }

    suspend fun insertAlarm(alarm: Alarm): Long = withContext(Dispatchers.IO) {
        val entity = alarm.toEntity()
        val id = alarmDao.insertAlarm(entity)
        val created = entity.copy(id = id)
        scheduler.schedule(created)

        // Sync with cloud if authenticated
        syncAlarmToCloud(created)
        id
    }

    suspend fun updateAlarm(alarm: Alarm) = withContext(Dispatchers.IO) {
        val entity = alarm.toEntity().copy(updatedAt = System.currentTimeMillis())
        alarmDao.updateAlarm(entity)
        if (entity.isEnabled) {
            scheduler.schedule(entity)
        } else {
            scheduler.cancel(entity.id)
        }
        syncAlarmToCloud(entity)
    }

    suspend fun toggleAlarm(id: Long, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        alarmDao.updateAlarmStatus(id, isEnabled)
        val alarm = alarmDao.getAlarmById(id)
        if (alarm != null) {
            if (isEnabled) {
                scheduler.schedule(alarm.copy(isEnabled = true))
            } else {
                scheduler.cancel(id)
            }
            syncAlarmToCloud(alarm.copy(isEnabled = isEnabled))
        }
    }

    suspend fun deleteAlarm(id: Long) = withContext(Dispatchers.IO) {
        scheduler.cancel(id)
        val entity = alarmDao.getAlarmById(id)
        alarmDao.deleteAlarmById(id)

        // Delete from cloud if present
        if (entity != null && entity.firestoreId.isNotEmpty()) {
            try {
                if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                    val user = Firebase.auth.currentUser
                    if (user != null) {
                        val dbId = context.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(dbId)
                        db.collection("users").document(user.uid)
                            .collection("alarms").document(entity.firestoreId).delete().await()
                    }
                }
            } catch (e: Exception) {
                Log.w("AlarmRepository", "Could not delete from cloud", e)
            }
        }
    }

    suspend fun duplicateAlarm(id: Long): Long? = withContext(Dispatchers.IO) {
        val original = alarmDao.getAlarmById(id) ?: return@withContext null
        val copy = original.copy(
            id = 0,
            title = "${original.title} (Copy)",
            isSynced = false,
            firestoreId = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val newId = alarmDao.insertAlarm(copy)
        if (copy.isEnabled) {
            scheduler.schedule(copy.copy(id = newId))
        }
        newId
    }

    // Export alarms as JSON
    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val alarms = alarmDao.getAllAlarmsList()
        val maps = alarms.map {
            mapOf(
                "title" to it.title,
                "description" to it.description,
                "category" to it.category,
                "triggerTime" to it.triggerTime,
                "timezone" to it.timezone,
                "repeatType" to it.repeatType,
                "repeatIntervalDays" to it.repeatIntervalDays,
                "repeatDaysOfWeek" to it.repeatDaysOfWeek,
                "isEnabled" to it.isEnabled,
                "soundName" to it.soundName,
                "isVibrationEnabled" to it.isVibrationEnabled,
                "vibrationPattern" to it.vibrationPattern
            )
        }
        jsonAdapter.toJson(maps)
    }

    // Import alarms from JSON
    suspend fun importFromJson(json: String): Int = withContext(Dispatchers.IO) {
        val maps = jsonAdapter.fromJson(json) ?: return@withContext 0
        var count = 0
        for (map in maps) {
            val title = map["title"] as? String ?: continue
            val triggerTime = (map["triggerTime"] as? Number)?.toLong() ?: continue
            val entity = AlarmEntity(
                title = title,
                description = map["description"] as? String ?: "",
                category = map["category"] as? String ?: "Personal",
                triggerTime = triggerTime,
                timezone = map["timezone"] as? String ?: java.util.TimeZone.getDefault().id,
                repeatType = map["repeatType"] as? String ?: "NONE",
                repeatIntervalDays = (map["repeatIntervalDays"] as? Number)?.toInt() ?: 1,
                repeatDaysOfWeek = map["repeatDaysOfWeek"] as? String ?: "",
                isEnabled = map["isEnabled"] as? Boolean ?: true,
                soundName = map["soundName"] as? String ?: "Default Alarm",
                isVibrationEnabled = map["isVibrationEnabled"] as? Boolean ?: true,
                vibrationPattern = map["vibrationPattern"] as? String ?: "NORMAL"
            )
            val newId = alarmDao.insertAlarm(entity)
            if (entity.isEnabled) {
                scheduler.schedule(entity.copy(id = newId))
            }
            count++
        }
        count
    }

    // Cloud synchronization
    suspend fun syncWithCloud(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                return@withContext Result.failure(
                    Exception("Firebase is not configured. Please add google-services.json to /app to enable cloud synchronization. Local offline alarms and JSON export/import work completely without Firebase.")
                )
            }
            val user = Firebase.auth.currentUser 
                ?: return@withContext Result.failure(Exception("Not signed in. Please sign in to sync your alarms with Cloud Firestore."))

            val dbId = context.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            val alarmsRef = db.collection("users").document(user.uid).collection("alarms")

            // 1. Push unsynced local alarms
            val unsynced = alarmDao.getUnsyncedAlarms()
            for (alarm in unsynced) {
                val docRef = if (alarm.firestoreId.isNotEmpty()) {
                    alarmsRef.document(alarm.firestoreId)
                } else {
                    alarmsRef.document()
                }

                val payload = mapOf(
                    "id" to docRef.id,
                    "title" to alarm.title,
                    "description" to alarm.description,
                    "category" to alarm.category,
                    "triggerTime" to alarm.triggerTime,
                    "timezone" to alarm.timezone,
                    "repeatType" to alarm.repeatType,
                    "repeatInterval" to alarm.repeatIntervalDays,
                    "repeatDays" to alarm.repeatDaysOfWeek,
                    "enabled" to alarm.isEnabled,
                    "sound" to alarm.soundName,
                    "vibration" to alarm.isVibrationEnabled,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                docRef.set(payload).await()
                alarmDao.markAsSynced(alarm.id, docRef.id)
            }

            // 2. Fetch remote alarms and update local DB if missing
            val snapshot = alarmsRef.get().await()
            var importedCount = 0
            val existing = alarmDao.getAllAlarmsList()
            val existingFirestoreIds = existing.map { it.firestoreId }.toSet()

            for (doc in snapshot.documents) {
                if (!existingFirestoreIds.contains(doc.id)) {
                    val entity = AlarmEntity(
                        title = doc.getString("title") ?: "Alarm",
                        description = doc.getString("description") ?: "",
                        category = doc.getString("category") ?: "Personal",
                        triggerTime = doc.getLong("triggerTime") ?: System.currentTimeMillis(),
                        timezone = doc.getString("timezone") ?: java.util.TimeZone.getDefault().id,
                        repeatType = doc.getString("repeatType") ?: "NONE",
                        repeatIntervalDays = doc.getLong("repeatInterval")?.toInt() ?: 1,
                        repeatDaysOfWeek = doc.getString("repeatDays") ?: "",
                        isEnabled = doc.getBoolean("enabled") ?: true,
                        soundName = doc.getString("sound") ?: "Default Alarm",
                        isVibrationEnabled = doc.getBoolean("vibration") ?: true,
                        isSynced = true,
                        firestoreId = doc.id
                    )
                    val id = alarmDao.insertAlarm(entity)
                    if (entity.isEnabled) {
                        scheduler.schedule(entity.copy(id = id))
                    }
                    importedCount++
                }
            }

            Result.success(unsynced.size + importedCount)
        } catch (e: Exception) {
            Log.e("AlarmRepository", "Cloud sync failed", e)
            Result.failure(e)
        }
    }

    private suspend fun syncAlarmToCloud(entity: AlarmEntity) {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) return
            val user = Firebase.auth.currentUser ?: return
            val dbId = context.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            val alarmsRef = db.collection("users").document(user.uid).collection("alarms")
            val docRef = if (entity.firestoreId.isNotEmpty()) alarmsRef.document(entity.firestoreId) else alarmsRef.document()

            val payload = mapOf(
                "id" to docRef.id,
                "title" to entity.title,
                "description" to entity.description,
                "category" to entity.category,
                "triggerTime" to entity.triggerTime,
                "timezone" to entity.timezone,
                "repeatType" to entity.repeatType,
                "repeatInterval" to entity.repeatIntervalDays,
                "repeatDays" to entity.repeatDaysOfWeek,
                "enabled" to entity.isEnabled,
                "sound" to entity.soundName,
                "vibration" to entity.isVibrationEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            alarmDao.markAsSynced(entity.id, docRef.id)
        } catch (e: Exception) {
            Log.w("AlarmRepository", "Background cloud sync skipped: ${e.message}")
        }
    }

    private fun AlarmEntity.toDomain(): Alarm = Alarm(
        id = id,
        title = title,
        description = description,
        category = AlarmCategory.fromString(category),
        triggerTime = triggerTime,
        timezone = timezone,
        repeatType = RepeatType.fromString(repeatType),
        repeatIntervalDays = repeatIntervalDays,
        repeatDaysOfWeek = if (repeatDaysOfWeek.isEmpty()) emptyList() else repeatDaysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() },
        isEnabled = isEnabled,
        soundUri = soundUri,
        soundName = soundName,
        isVibrationEnabled = isVibrationEnabled,
        vibrationPattern = VibrationPattern.fromString(vibrationPattern),
        isPrayerAlarm = isPrayerAlarm,
        prayerName = prayerName,
        isSynced = isSynced,
        firestoreId = firestoreId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Alarm.toEntity(): AlarmEntity = AlarmEntity(
        id = id,
        title = title,
        description = description,
        category = category.displayName,
        triggerTime = triggerTime,
        timezone = timezone,
        repeatType = repeatType.name,
        repeatIntervalDays = repeatIntervalDays,
        repeatDaysOfWeek = repeatDaysOfWeek.joinToString(","),
        isEnabled = isEnabled,
        soundUri = soundUri,
        soundName = soundName,
        isVibrationEnabled = isVibrationEnabled,
        vibrationPattern = vibrationPattern.name,
        isPrayerAlarm = isPrayerAlarm,
        prayerName = prayerName,
        isSynced = isSynced,
        firestoreId = firestoreId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
