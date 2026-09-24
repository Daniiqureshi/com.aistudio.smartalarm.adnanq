package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.PrayerCacheEntity
import com.example.data.local.PrayerDao
import com.example.data.remote.AladhanApiService
import com.example.domain.model.PrayerTimes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PrayerRepository(
    private val context: Context,
    private val prayerDao: PrayerDao,
    private val apiService: AladhanApiService = AladhanApiService.create()
) {

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun getPrayerTimes(
        city: String = "Makkah",
        country: String = "Saudi Arabia",
        latitude: Double = 21.4225,
        longitude: Double = 39.8262,
        useGps: Boolean = false,
        method: Int = 3, // Muslim World League default
        school: Int = 0 // 0=Shafi, 1=Hanafi
    ): PrayerTimes = withContext(Dispatchers.IO) {
        val today = Calendar.getInstance()
        val dateApi = dateFormat.format(today.time)
        val dateDb = dbDateFormat.format(today.time)
        val cacheKey = if (useGps) "GPS_${String.format(Locale.US, "%.2f_%.2f", latitude, longitude)}" else city

        // 1. Check local Room cache first
        val cached = prayerDao.getCachedPrayer(dateDb, cacheKey)
        if (cached != null) {
            return@withContext cached.toDomain(cacheKey)
        }

        // 2. Try online Aladhan API
        try {
            val response = if (useGps) {
                apiService.getTimingsByCoordinates(dateApi, latitude, longitude, method, school)
            } else {
                apiService.getTimingsByCity(dateApi, city, country, method, school)
            }

            val timings = response.data?.timings
            if (timings != null) {
                val entity = PrayerCacheEntity(
                    dateString = dateDb,
                    cityName = cacheKey,
                    fajr = timings.fajr.cleanTime(),
                    sunrise = timings.sunrise.cleanTime(),
                    dhuhr = timings.dhuhr.cleanTime(),
                    asr = timings.asr.cleanTime(),
                    sunset = timings.sunset.cleanTime(),
                    maghrib = timings.maghrib.cleanTime(),
                    isha = timings.isha.cleanTime(),
                    imsak = timings.imsak.cleanTime(),
                    midnight = timings.midnight.cleanTime(),
                    firstThird = timings.firstThird.cleanTime(),
                    lastThird = timings.lastThird.cleanTime()
                )
                prayerDao.insertPrayerCache(entity)
                return@withContext entity.toDomain(cacheKey)
            }
        } catch (e: Exception) {
            Log.w("PrayerRepository", "Online prayer times fetch failed, calculating offline: ${e.message}")
        }

        // 3. Guaranteed offline astronomical calculation fallback!
        val isHanafi = (school == 1)
        val offlineTimes = PrayerTimes.calculateOffline(
            latitude = latitude,
            longitude = longitude,
            calendar = today,
            isHanafi = isHanafi
        ).copy(locationName = cacheKey)

        // Cache the offline calculation as well
        val fallbackEntity = PrayerCacheEntity(
            dateString = dateDb,
            cityName = cacheKey,
            fajr = offlineTimes.fajr,
            sunrise = offlineTimes.sunrise,
            dhuhr = offlineTimes.dhuhr,
            asr = offlineTimes.asr,
            sunset = offlineTimes.sunset,
            maghrib = offlineTimes.maghrib,
            isha = offlineTimes.isha,
            imsak = offlineTimes.imsak,
            midnight = offlineTimes.midnight,
            firstThird = offlineTimes.tahajjud,
            lastThird = offlineTimes.tahajjud
        )
        prayerDao.insertPrayerCache(fallbackEntity)

        offlineTimes
    }

    private fun String.cleanTime(): String {
        return this.split(" ")[0].trim().take(5)
    }

    private fun PrayerCacheEntity.toDomain(location: String): PrayerTimes {
        return PrayerTimes(
            fajr = fajr,
            sunrise = sunrise,
            dhuhr = dhuhr,
            asr = asr,
            sunset = sunset,
            maghrib = maghrib,
            isha = isha,
            imsak = imsak,
            midnight = midnight,
            tahajjud = lastThird.ifEmpty { "03:45" },
            dateString = dateString,
            locationName = location,
            isFromCache = true
        )
    }
}
