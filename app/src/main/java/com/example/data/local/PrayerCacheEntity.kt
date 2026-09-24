package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prayer_cache")
data class PrayerCacheEntity(
    @PrimaryKey
    val dateString: String, // Format: YYYY-MM-DD
    val cityName: String,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val sunset: String,
    val maghrib: String,
    val isha: String,
    val imsak: String,
    val midnight: String,
    val firstThird: String,
    val lastThird: String,
    val timestamp: Long = System.currentTimeMillis()
)
