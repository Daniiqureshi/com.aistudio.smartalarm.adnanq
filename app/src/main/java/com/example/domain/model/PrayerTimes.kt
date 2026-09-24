package com.example.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.*

data class PrayerTimes(
    val fajr: String = "05:15",
    val sunrise: String = "06:35",
    val dhuhr: String = "12:30",
    val asr: String = "15:45",
    val sunset: String = "18:20",
    val maghrib: String = "18:25",
    val isha: String = "19:45",
    val imsak: String = "05:05", // Sehri cutoff
    val midnight: String = "00:00",
    val tahajjud: String = "03:45", // Approx last third of night
    val dateString: String = "",
    val locationName: String = "Current Location",
    val isFromCache: Boolean = false
) {
    /**
     * Determines which prayer is next, along with the remaining milliseconds.
     */
    fun getNextPrayerInfo(nowMillis: Long = System.currentTimeMillis()): NextPrayerResult {
        val calendar = Calendar.getInstance()
        val timesMap = mapOf(
            "Fajr" to fajr,
            "Sunrise" to sunrise,
            "Dhuhr" to dhuhr,
            "Asr" to asr,
            "Maghrib" to maghrib,
            "Isha" to isha
        )

        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        var nextPrayerName = "Fajr"
        var nextPrayerMillis = Long.MAX_VALUE

        for ((name, timeStr) in timesMap) {
            val cleanTime = timeStr.trim().take(5)
            val parts = cleanTime.split(":")
            if (parts.size >= 2) {
                val hour = parts[0].toIntOrNull() ?: 0
                val min = parts[1].toIntOrNull() ?: 0
                val prayerCal = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, min)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (prayerCal.timeInMillis > nowMillis && prayerCal.timeInMillis < nextPrayerMillis) {
                    nextPrayerMillis = prayerCal.timeInMillis
                    nextPrayerName = name
                }
            }
        }

        // If all prayers today passed, next is Fajr tomorrow
        if (nextPrayerMillis == Long.MAX_VALUE) {
            val fajrParts = fajr.trim().take(5).split(":")
            val hour = fajrParts.getOrNull(0)?.toIntOrNull() ?: 5
            val min = fajrParts.getOrNull(1)?.toIntOrNull() ?: 15
            val tomorrowFajr = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            nextPrayerName = "Fajr"
            nextPrayerMillis = tomorrowFajr.timeInMillis
        }

        val remainingMillis = (nextPrayerMillis - nowMillis).coerceAtLeast(0)
        val hours = (remainingMillis / (1000 * 60 * 60)).toInt()
        val minutes = ((remainingMillis / (1000 * 60)) % 60).toInt()

        val countdown = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

        return NextPrayerResult(
            prayerName = nextPrayerName,
            time = timesMap[nextPrayerName] ?: fajr,
            remainingMillis = remainingMillis,
            countdownText = "$nextPrayerName in $countdown"
        )
    }

    companion object {
        /**
         * Calculates approximate prayer times completely offline using astronomical solar position formulas.
         * Guarantees prayer times are always available even without internet or API access!
         */
        fun calculateOffline(
            latitude: Double = 21.4225, // Default Makkah
            longitude: Double = 39.8262,
            calendar: Calendar = Calendar.getInstance(),
            isHanafi: Boolean = false,
            fajrAngle: Double = 18.0,
            ishaAngle: Double = 18.0
        ): PrayerTimes {
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            val timezoneOffset = calendar.timeZone.getOffset(calendar.timeInMillis) / (1000.0 * 3600.0)

            // Approximate solar declination and equation of time
            val b = 2.0 * Math.PI * (dayOfYear - 81) / 365.0
            val eot = 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b) // in minutes
            val declination = 23.45 * sin(Math.toRadians((360.0 / 365.0) * (dayOfYear - 81))) // in degrees

            // Solar noon
            val solarNoonMinutes = 720.0 - (4.0 * longitude) - eot + (timezoneOffset * 60.0)
            val dhuhrHour = (solarNoonMinutes / 60.0)

            val latRad = Math.toRadians(latitude)
            val decRad = Math.toRadians(declination)

            fun hourAngle(altitude: Double): Double {
                val altRad = Math.toRadians(altitude)
                val cosHA = (sin(altRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
                val clamped = cosHA.coerceIn(-1.0, 1.0)
                return Math.toDegrees(acos(clamped)) / 15.0
            }

            // Sunrise & Sunset: sun center is -0.833 degrees below horizon
            val sunriseHa = hourAngle(-0.833)
            val sunriseHour = dhuhrHour - sunriseHa
            val sunsetHour = dhuhrHour + sunriseHa

            // Fajr & Isha
            val fajrHa = hourAngle(-fajrAngle)
            val fajrHour = dhuhrHour - fajrHa

            val ishaHa = hourAngle(-ishaAngle)
            val ishaHour = dhuhrHour + ishaHa

            // Asr: shadow ratio (1 for Shafi, 2 for Hanafi)
            val shadowRatio = if (isHanafi) 2.0 else 1.0
            val asrAlt = Math.toDegrees(atan(1.0 / (shadowRatio + tan(abs(latRad - decRad)))))
            val asrHa = hourAngle(asrAlt)
            val asrHour = dhuhrHour + asrHa

            fun formatHour(hourDec: Double): String {
                val normalized = ((hourDec % 24.0) + 24.0) % 24.0
                val h = normalized.toInt()
                val m = (((normalized - h) * 60.0) + 0.5).toInt().coerceIn(0, 59)
                return String.format(Locale.US, "%02d:%02d", h, m)
            }

            // Tahajjud: last third of night (between Sunset and Fajr)
            val nightLength = ((fajrHour + 24.0) - sunsetHour) % 24.0
            val tahajjudStart = sunsetHour + (nightLength * 2.0 / 3.0)

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

            return PrayerTimes(
                fajr = formatHour(fajrHour),
                sunrise = formatHour(sunriseHour),
                dhuhr = formatHour(dhuhrHour),
                asr = formatHour(asrHour),
                sunset = formatHour(sunsetHour),
                maghrib = formatHour(sunsetHour + 0.05), // ~3 mins after sunset
                isha = formatHour(ishaHour),
                imsak = formatHour(fajrHour - 0.17), // 10 mins before Fajr
                midnight = formatHour(sunsetHour + (nightLength / 2.0)),
                tahajjud = formatHour(tahajjudStart),
                dateString = sdf.format(calendar.time),
                locationName = "Offline Calculator",
                isFromCache = false
            )
        }
    }
}

data class NextPrayerResult(
    val prayerName: String,
    val time: String,
    val remainingMillis: Long,
    val countdownText: String
)
