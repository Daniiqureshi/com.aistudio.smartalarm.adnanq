package com.example.domain.model

data class Alarm(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: AlarmCategory = AlarmCategory.PERSONAL,
    val triggerTime: Long,
    val timezone: String = java.util.TimeZone.getDefault().id,
    val repeatType: RepeatType = RepeatType.NONE,
    val repeatIntervalDays: Int = 1,
    val repeatDaysOfWeek: List<Int> = emptyList(), // 1=Mon, 2=Tue, ..., 7=Sun
    val isEnabled: Boolean = true,
    val soundUri: String = "",
    val soundName: String = "Default Alarm",
    val isVibrationEnabled: Boolean = true,
    val vibrationPattern: VibrationPattern = VibrationPattern.NORMAL,
    val isPrayerAlarm: Boolean = false,
    val prayerName: String? = null,
    val isSynced: Boolean = false,
    val firestoreId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class AlarmCategory(val displayName: String) {
    PERSONAL("Personal"),
    WORK("Work"),
    IBADAT("Ibadat"),
    EVENT("Event"),
    HEALTH("Health"),
    STUDY("Study"),
    TRAVEL("Travel"),
    CUSTOM("Custom");

    companion object {
        fun fromString(value: String): AlarmCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) } ?: PERSONAL
        }
    }
}

enum class RepeatType(val label: String) {
    NONE("Once"),
    DAILY("Every day"),
    WEEKDAYS("Every weekday"),
    WEEKLY("Every week"),
    BIWEEKLY("Every 2 weeks"),
    MONTHLY("Every month"),
    YEARLY("Every year"),
    INTERVAL_DAYS("Every X days"),
    CUSTOM_DAYS("Custom days");

    companion object {
        fun fromString(value: String): RepeatType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}

enum class VibrationPattern(val title: String) {
    SHORT("Short Pulse"),
    NORMAL("Standard"),
    STRONG("Strong Alert");

    companion object {
        fun fromString(value: String): VibrationPattern {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NORMAL
        }
    }
}
