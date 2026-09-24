package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "Personal",
    val triggerTime: Long, // Epoch millis
    val timezone: String,
    val repeatType: String = "NONE", // NONE, DAILY, WEEKDAYS, WEEKLY, BIWEEKLY, MONTHLY, YEARLY, INTERVAL_DAYS, CUSTOM_DAYS
    val repeatIntervalDays: Int = 1,
    val repeatDaysOfWeek: String = "", // e.g. "1,5" (Monday, Friday)
    val isEnabled: Boolean = true,
    val soundUri: String = "",
    val soundName: String = "Default Alarm",
    val isVibrationEnabled: Boolean = true,
    val vibrationPattern: String = "NORMAL", // SHORT, NORMAL, STRONG
    val isPrayerAlarm: Boolean = false,
    val prayerName: String? = null,
    val isSynced: Boolean = false,
    val firestoreId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
