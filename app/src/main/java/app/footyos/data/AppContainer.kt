package app.footyos.data

import android.content.Context
import androidx.room.Room
import app.footyos.calendar.CalendarRepository
import app.footyos.data.local.FootyDatabase
import app.footyos.reminders.ReminderScheduler

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database = Room.databaseBuilder(
        appContext,
        FootyDatabase::class.java,
        "footyos.db",
    ).build()

    val repository = FootyRepository(database.footyDao())
    val settingsRepository = SettingsRepository(appContext)
    val reminderScheduler = ReminderScheduler(appContext)
    val calendarRepository = CalendarRepository(appContext)
}
