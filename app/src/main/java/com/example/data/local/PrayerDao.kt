package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {

    @Query("SELECT * FROM prayer_cache WHERE dateString = :dateString AND cityName = :cityName LIMIT 1")
    suspend fun getCachedPrayer(dateString: String, cityName: String): PrayerCacheEntity?

    @Query("SELECT * FROM prayer_cache WHERE dateString = :dateString AND cityName = :cityName LIMIT 1")
    fun getCachedPrayerFlow(dateString: String, cityName: String): Flow<PrayerCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerCache(entity: PrayerCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PrayerCacheEntity>)

    @Query("DELETE FROM prayer_cache WHERE timestamp < :oldTimestamp")
    suspend fun deleteOldCache(oldTimestamp: Long)
}
