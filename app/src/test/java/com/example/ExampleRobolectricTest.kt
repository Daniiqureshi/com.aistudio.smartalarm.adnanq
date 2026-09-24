package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AlarmEntity
import com.example.data.local.AppDatabase
import com.example.domain.model.PrayerTimes
import com.example.domain.parser.NaturalLanguageParser
import com.example.domain.scheduler.AndroidAlarmScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SmartAlarm", appName)
    }

    @Test
    fun testRoomDatabaseAlarmDao() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val dao = db.alarmDao()

        val alarm = AlarmEntity(
            title = "Doctor Appointment",
            description = "Bring medical report",
            category = "Health",
            triggerTime = System.currentTimeMillis() + 3600000L,
            timezone = "UTC",
            repeatType = "NONE",
            isEnabled = true
        )

        val id = dao.insertAlarm(alarm)
        assertTrue(id > 0)

        val retrieved = dao.getAlarmById(id)
        assertNotNull(retrieved)
        assertEquals("Doctor Appointment", retrieved?.title)
        assertEquals("Health", retrieved?.category)

        dao.deleteAlarmById(id)
        val afterDelete = dao.getAlarmById(id)
        assertEquals(null, afterDelete)
    }

    @Test
    fun testNaturalLanguageParserRelative() {
        val parsed = NaturalLanguageParser.parse("2 weeks later meeting")
        assertNotNull(parsed)
        assertEquals("Meeting", parsed?.title)
        assertTrue(parsed!!.triggerTimeMillis > System.currentTimeMillis())

        val in30Min = NaturalLanguageParser.parse("in 30 minutes call doctor")
        assertNotNull(in30Min)
        assertEquals("Call doctor", in30Min?.title)
    }

    @Test
    fun testOfflinePrayerTimesCalculation() {
        val cal = Calendar.getInstance()
        val prayerTimes = PrayerTimes.calculateOffline(
            latitude = 21.4225,
            longitude = 39.8262,
            calendar = cal
        )

        assertNotNull(prayerTimes.fajr)
        assertNotNull(prayerTimes.dhuhr)
        assertNotNull(prayerTimes.asr)
        assertNotNull(prayerTimes.maghrib)
        assertNotNull(prayerTimes.isha)
        assertTrue(prayerTimes.fajr.matches(Regex("\\d{2}:\\d{2}")))
        assertTrue(prayerTimes.maghrib.matches(Regex("\\d{2}:\\d{2}")))

        val nextPrayerInfo = prayerTimes.getNextPrayerInfo()
        assertNotNull(nextPrayerInfo.prayerName)
        assertTrue(nextPrayerInfo.countdownText.isNotEmpty())
    }

    @Test
    fun testAlarmSchedulerRecurringCalculation() {
        val pastTime = System.currentTimeMillis() - 100000L
        val dailyAlarm = AlarmEntity(
            title = "Daily Workout",
            triggerTime = pastTime,
            timezone = "UTC",
            repeatType = "DAILY",
            isEnabled = true
        )

        val nextTrigger = AndroidAlarmScheduler.computeNextTriggerTime(dailyAlarm)
        assertTrue(nextTrigger > System.currentTimeMillis())
    }

    @Test
    fun testSettingsViewModelInitializationWithoutFirebase() {
        val context = ApplicationProvider.getApplicationContext<SmartAlarmApp>()
        val settingsRepo = context.settingsRepository
        val alarmRepo = context.alarmRepository
        val viewModel = com.example.ui.viewmodel.SettingsViewModel(settingsRepo, alarmRepo)

        assertNotNull(viewModel.settings.value)
        assertEquals(null, viewModel.currentUser.value)
        assertEquals(null, viewModel.syncStatus.value)
    }
}
