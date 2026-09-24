package com.example.domain.model

data class SpiritualPractice(
    val id: String,
    val title: String,
    val tradition: TraditionType,
    val description: String,
    val suggestedTime: String, // HH:mm
    val repeatType: RepeatType = RepeatType.DAILY,
    val category: AlarmCategory = AlarmCategory.IBADAT
) {
    companion object {
        fun getPredefinedPractices(tradition: TraditionType): List<SpiritualPractice> {
            return TraditionType.getPredefinedPractices(tradition)
        }
    }
}

enum class TraditionType(val displayName: String) {
    ISLAM("Islam"),
    CHRISTIANITY("Christianity"),
    HINDUISM("Hinduism"),
    SIKHISM("Sikhism"),
    BUDDHISM("Buddhism"),
    JUDAISM("Judaism"),
    CUSTOM("Custom / Mindfulness");

    companion object {
        fun getPredefinedPractices(tradition: TraditionType): List<SpiritualPractice> {
            return when (tradition) {
                CHRISTIANITY -> listOf(
                    SpiritualPractice("c1", "Morning Devotional & Prayer", CHRISTIANITY, "Start the day with scripture and contemplation", "07:00"),
                    SpiritualPractice("c2", "The Angelus", CHRISTIANITY, "Traditional prayer commemorating the Incarnation", "12:00"),
                    SpiritualPractice("c3", "Evening Prayer & Thanksgiving", CHRISTIANITY, "Give thanks for the blessings of the day", "19:30"),
                    SpiritualPractice("c4", "Sunday Worship Service", CHRISTIANITY, "Weekly community gathering and church worship", "10:00", RepeatType.WEEKLY)
                )
                HINDUISM -> listOf(
                    SpiritualPractice("h1", "Brahma Muhurta Puja", HINDUISM, "Auspicious morning period before dawn for meditation & prayers", "05:00"),
                    SpiritualPractice("h2", "Sandhya Aarti", HINDUISM, "Evening light offering and sacred chant", "18:30"),
                    SpiritualPractice("h3", "Ekadashi Fast Reminder", HINDUISM, "Bi-monthly spiritual fasting day (11th lunar day)", "06:00", RepeatType.BIWEEKLY),
                    SpiritualPractice("h4", "Purnima Devotion", HINDUISM, "Full moon spiritual reflection and gratitude", "18:00", RepeatType.MONTHLY)
                )
                SIKHISM -> listOf(
                    SpiritualPractice("s1", "Amrit Vela (Ambrosial Hours)", SIKHISM, "Early morning contemplation of the divine name (Naam Simran)", "04:30"),
                    SpiritualPractice("s2", "Morning Nitnem Banis", SIKHISM, "Recitation of morning compositions (Japji Sahib, Jaap Sahib)", "06:00"),
                    SpiritualPractice("s3", "Rehras Sahib", SIKHISM, "Sunset evening prayer for peace and perseverance", "18:45"),
                    SpiritualPractice("s4", "Weekly Gurdwara Visit", SIKHISM, "Sunday Sangat gathering and Langar service", "10:30", RepeatType.WEEKLY)
                )
                BUDDHISM -> listOf(
                    SpiritualPractice("b1", "Dawn Vipassana Meditation", BUDDHISM, "Breath awareness and mindful observation", "06:30"),
                    SpiritualPractice("b2", "Mindfulness Chime (Midday)", BUDDHISM, "Pause for 3 deep breaths and present moment return", "12:00"),
                    SpiritualPractice("b3", "Metta Loving-Kindness Meditation", BUDDHISM, "Cultivating unconditional compassion for all beings", "21:00"),
                    SpiritualPractice("b4", "Uposatha Observance Day", BUDDHISM, "Lunar spiritual observance and reflection", "08:00", RepeatType.MONTHLY)
                )
                JUDAISM -> listOf(
                    SpiritualPractice("j1", "Shacharit (Morning Service)", JUDAISM, "Morning prayers and Shema recitation", "07:15"),
                    SpiritualPractice("j2", "Mincha (Afternoon Service)", JUDAISM, "Midday reflection and prayer", "14:30"),
                    SpiritualPractice("j3", "Ma'ariv (Evening Service)", JUDAISM, "Nightfall prayer and gratitude", "20:00"),
                    SpiritualPractice("j4", "Erev Shabbat Candle Lighting", JUDAISM, "Welcoming the Sabbath with peace and candles", "18:00", RepeatType.WEEKLY)
                )
                CUSTOM -> listOf(
                    SpiritualPractice("cu1", "Daily Gratitude Journal", CUSTOM, "Write down 3 things you are grateful for", "08:00"),
                    SpiritualPractice("cu2", "Afternoon Breathwork / Yoga", CUSTOM, "Recharge energy and stretch body", "16:00"),
                    SpiritualPractice("cu3", "Nightly Meditation & Reflection", CUSTOM, "Release tension before restful sleep", "22:00")
                )
                ISLAM -> emptyList() // Handled via dedicated Islamic prayer module
            }
        }
    }
}
