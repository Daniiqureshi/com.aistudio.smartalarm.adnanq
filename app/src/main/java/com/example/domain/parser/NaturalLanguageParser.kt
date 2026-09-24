package com.example.domain.parser

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class ParsedReminder(
    val title: String,
    val triggerTimeMillis: Long,
    val dateDisplay: String,
    val timeDisplay: String,
    val isConfident: Boolean
)

object NaturalLanguageParser {

    private val timeRegex = Pattern.compile("(?i)(\\b\\d{1,2})(:(\\d{2}))?\\s*(am|pm)?\\b")
    private val relativeUnitRegex = Pattern.compile("(?i)(\\b(?:in\\s+)?(\\d+)\\s*(minute|min|hour|hr|day|week|month|year)s?\\s*(later)?\\b)")
    private val tomorrowRegex = Pattern.compile("(?i)\\btomorrow\\b")
    private val nextDayOfWeekRegex = Pattern.compile("(?i)\\bnext\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b")
    private val specificDateRegex = Pattern.compile("(?i)\\b(\\d{1,2})\\s+(january|february|march|april|may|june|july|august|september|october|november|december)\\s*(\\d{4})?\\b")

    fun parse(input: String): ParsedReminder? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val cal = Calendar.getInstance()
        var matchedTime = false
        var matchedDate = false
        var workingText = trimmed

        // 1. Check relative duration: e.g. "in 30 minutes", "2 hours later", "1 week later", "2 years later"
        val relMatcher = relativeUnitRegex.matcher(workingText)
        if (relMatcher.find()) {
            val amount = relMatcher.group(2)?.toIntOrNull() ?: 0
            val unit = relMatcher.group(3)?.lowercase() ?: ""
            when {
                unit.startsWith("min") -> cal.add(Calendar.MINUTE, amount)
                unit.startsWith("hour") || unit.startsWith("hr") -> cal.add(Calendar.HOUR_OF_DAY, amount)
                unit.startsWith("day") -> cal.add(Calendar.DAY_OF_YEAR, amount)
                unit.startsWith("week") -> cal.add(Calendar.WEEK_OF_YEAR, amount)
                unit.startsWith("month") -> cal.add(Calendar.MONTH, amount)
                unit.startsWith("year") -> cal.add(Calendar.YEAR, amount)
            }
            matchedDate = true
            matchedTime = true
            workingText = workingText.replace(relMatcher.group(0) ?: "", "")
        }

        // 2. Check "tomorrow"
        val tomMatcher = tomorrowRegex.matcher(workingText)
        if (tomMatcher.find()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            matchedDate = true
            workingText = workingText.replace(tomMatcher.group(0) ?: "", "")
        }

        // 3. Check "next Friday", "next Monday"
        val nextDayMatcher = nextDayOfWeekRegex.matcher(workingText)
        if (nextDayMatcher.find()) {
            val dayName = nextDayMatcher.group(1)?.lowercase() ?: ""
            val targetDay = when (dayName) {
                "monday" -> Calendar.MONDAY
                "tuesday" -> Calendar.TUESDAY
                "wednesday" -> Calendar.WEDNESDAY
                "thursday" -> Calendar.THURSDAY
                "friday" -> Calendar.FRIDAY
                "saturday" -> Calendar.SATURDAY
                "sunday" -> Calendar.SUNDAY
                else -> -1
            }
            if (targetDay != -1) {
                var daysAhead = (targetDay - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7
                if (daysAhead == 0) daysAhead = 7
                cal.add(Calendar.DAY_OF_YEAR, daysAhead)
                matchedDate = true
                workingText = workingText.replace(nextDayMatcher.group(0) ?: "", "")
            }
        }

        // 4. Check specific date: "23 November 2027" or "10 October"
        val specDateMatcher = specificDateRegex.matcher(workingText)
        if (specDateMatcher.find()) {
            val day = specDateMatcher.group(1)?.toIntOrNull() ?: 1
            val monthStr = specDateMatcher.group(2)?.lowercase() ?: ""
            val year = specDateMatcher.group(3)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
            val month = when (monthStr) {
                "january" -> Calendar.JANUARY
                "february" -> Calendar.FEBRUARY
                "march" -> Calendar.MARCH
                "april" -> Calendar.APRIL
                "may" -> Calendar.MAY
                "june" -> Calendar.JUNE
                "july" -> Calendar.JULY
                "august" -> Calendar.AUGUST
                "september" -> Calendar.SEPTEMBER
                "october" -> Calendar.OCTOBER
                "november" -> Calendar.NOVEMBER
                "december" -> Calendar.DECEMBER
                else -> Calendar.JANUARY
            }
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, day)
            matchedDate = true
            workingText = workingText.replace(specDateMatcher.group(0) ?: "", "")
        }

        // 5. Check time like "8 PM", "7:30 AM", "19:00", "8pm", "8 am"
        val timeMatcher = timeRegex.matcher(workingText)
        while (timeMatcher.find()) {
            val hourStr = timeMatcher.group(1) ?: ""
            val minStr = timeMatcher.group(3)
            val amPm = timeMatcher.group(4)?.lowercase()

            val rawHour = hourStr.toIntOrNull() ?: continue
            val minute = minStr?.toIntOrNull() ?: 0

            var hour = rawHour
            if (amPm != null) {
                if (amPm == "pm" && hour < 12) hour += 12
                if (amPm == "am" && hour == 12) hour = 0
            } else if (hour in 1..7 && !matchedDate) {
                // Heuristic: "call at 5" is usually 5 PM
                hour += 12
            }

            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            matchedTime = true
            workingText = workingText.replace(timeMatcher.group(0) ?: "", "")
            break
        }

        // Clean up title
        var title = workingText
            .replace(Regex("(?i)\\b(remind me to|remind me|reminder|at|on|for|to)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (title.isEmpty()) {
            title = "Reminder"
        }

        // If no time was specified, default to 9:00 AM next day if date matched, or 1 hour later
        if (!matchedTime) {
            if (matchedDate) {
                cal.set(Calendar.HOUR_OF_DAY, 9)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
            } else {
                cal.add(Calendar.HOUR_OF_DAY, 1)
            }
        }

        // If the calculated time is in the past, push by 1 day or 1 year
        if (cal.timeInMillis <= System.currentTimeMillis() && !matchedDate) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        return ParsedReminder(
            title = title.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            triggerTimeMillis = cal.timeInMillis,
            dateDisplay = dateFormat.format(cal.time),
            timeDisplay = timeFormat.format(cal.time),
            isConfident = matchedDate || matchedTime
        )
    }

    /**
     * Calculates future date/time from custom relative values
     * e.g. amount = 2, unit = "Years" -> now + 2 years
     */
    fun calculateRelative(amount: Int, unit: String): Long {
        val cal = Calendar.getInstance()
        when (unit.lowercase()) {
            "minutes" -> cal.add(Calendar.MINUTE, amount)
            "hours" -> cal.add(Calendar.HOUR_OF_DAY, amount)
            "days" -> cal.add(Calendar.DAY_OF_YEAR, amount)
            "weeks" -> cal.add(Calendar.WEEK_OF_YEAR, amount)
            "months" -> cal.add(Calendar.MONTH, amount)
            "years" -> cal.add(Calendar.YEAR, amount)
        }
        return cal.timeInMillis
    }
}
