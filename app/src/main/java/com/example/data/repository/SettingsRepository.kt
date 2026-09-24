package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme { LIGHT, DARK, SYSTEM }
enum class AppLanguage(val code: String, val label: String) {
    ENGLISH("en", "English"),
    URDU("ur", "اردو (Urdu)"),
    ARABIC("ar", "العربية (Arabic)")
}

data class UserSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val prayerCity: String = "Makkah",
    val prayerCountry: String = "Saudi Arabia",
    val latitude: Double = 21.4225,
    val longitude: Double = 39.8262,
    val useGps: Boolean = false,
    val calculationMethod: Int = 3, // 3=MWL, 2=ISNA, 4=Makkah, 1=Karachi, 5=Egyptian
    val madhab: Int = 0, // 0=Shafi, 1=Hanafi
    val defaultSnoozeMinutes: Int = 10,
    val isFajrAdhanEnabled: Boolean = true,
    val isDhuhrAdhanEnabled: Boolean = true,
    val isAsrAdhanEnabled: Boolean = true,
    val isMaghribAdhanEnabled: Boolean = true,
    val isIshaAdhanEnabled: Boolean = true,
    val adhanAudioType: String = "MELODY", // MELODY, CHIME, SILENT
    val tahajjudOffsetMinutes: Int = 0 // 0 = at last third start, 15 = 15m before, 30 = 30m before
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("smart_alarm_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): UserSettings {
        val themeName = prefs.getString("key_theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        val langCode = prefs.getString("key_lang", "en") ?: "en"

        return UserSettings(
            theme = try { AppTheme.valueOf(themeName) } catch (e: Exception) { AppTheme.SYSTEM },
            language = AppLanguage.entries.firstOrNull { it.code == langCode } ?: AppLanguage.ENGLISH,
            prayerCity = prefs.getString("key_city", "Makkah") ?: "Makkah",
            prayerCountry = prefs.getString("key_country", "Saudi Arabia") ?: "Saudi Arabia",
            latitude = prefs.getFloat("key_lat", 21.4225f).toDouble(),
            longitude = prefs.getFloat("key_lng", 39.8262f).toDouble(),
            useGps = prefs.getBoolean("key_use_gps", false),
            calculationMethod = prefs.getInt("key_calc_method", 3),
            madhab = prefs.getInt("key_madhab", 0),
            defaultSnoozeMinutes = prefs.getInt("key_snooze", 10),
            isFajrAdhanEnabled = prefs.getBoolean("key_adhan_fajr", true),
            isDhuhrAdhanEnabled = prefs.getBoolean("key_adhan_dhuhr", true),
            isAsrAdhanEnabled = prefs.getBoolean("key_adhan_asr", true),
            isMaghribAdhanEnabled = prefs.getBoolean("key_adhan_maghrib", true),
            isIshaAdhanEnabled = prefs.getBoolean("key_adhan_isha", true),
            adhanAudioType = prefs.getString("key_adhan_type", "MELODY") ?: "MELODY",
            tahajjudOffsetMinutes = prefs.getInt("key_tahajjud_offset", 0)
        )
    }

    fun updateTheme(theme: AppTheme) {
        prefs.edit().putString("key_theme", theme.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(theme = theme)
    }

    fun updateLanguage(language: AppLanguage) {
        prefs.edit().putString("key_lang", language.code).apply()
        _settingsFlow.value = _settingsFlow.value.copy(language = language)
    }

    fun updateCity(city: String, country: String, lat: Double, lng: Double) {
        prefs.edit()
            .putString("key_city", city)
            .putString("key_country", country)
            .putFloat("key_lat", lat.toFloat())
            .putFloat("key_lng", lng.toFloat())
            .putBoolean("key_use_gps", false)
            .apply()
        _settingsFlow.value = _settingsFlow.value.copy(
            prayerCity = city,
            prayerCountry = country,
            latitude = lat,
            longitude = lng,
            useGps = false
        )
    }

    fun updateGpsLocation(lat: Double, lng: Double) {
        prefs.edit()
            .putFloat("key_lat", lat.toFloat())
            .putFloat("key_lng", lng.toFloat())
            .putBoolean("key_use_gps", true)
            .apply()
        _settingsFlow.value = _settingsFlow.value.copy(
            latitude = lat,
            longitude = lng,
            useGps = true,
            prayerCity = "GPS (${String.format("%.2f", lat)}, ${String.format("%.2f", lng)})"
        )
    }

    fun updateCalculationMethod(method: Int, madhab: Int) {
        prefs.edit()
            .putInt("key_calc_method", method)
            .putInt("key_madhab", madhab)
            .apply()
        _settingsFlow.value = _settingsFlow.value.copy(
            calculationMethod = method,
            madhab = madhab
        )
    }

    fun updateAdhanToggle(prayer: String, enabled: Boolean) {
        val editor = prefs.edit()
        when (prayer.lowercase()) {
            "fajr" -> editor.putBoolean("key_adhan_fajr", enabled)
            "dhuhr" -> editor.putBoolean("key_adhan_dhuhr", enabled)
            "asr" -> editor.putBoolean("key_adhan_asr", enabled)
            "maghrib" -> editor.putBoolean("key_adhan_maghrib", enabled)
            "isha" -> editor.putBoolean("key_adhan_isha", enabled)
        }
        editor.apply()
        _settingsFlow.value = loadSettings()
    }
}
