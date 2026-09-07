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
    ).addMigrations(app.footyos.data.local.NUTRITION_MIGRATION, app.footyos.data.local.ESTIMATE_MIGRATION, app.footyos.data.local.GEMINI_MIGRATION).build()

    val geminiKeys = app.footyos.nutrition.GeminiKeyStore(appContext)
    val geminiAnalysis = app.footyos.nutrition.GeminiAnalysis(database.footyDao(), app.footyos.photos.MealPhotos(appContext), geminiKeys)

    val repository = FootyRepository(database.footyDao())
    val settingsRepository = SettingsRepository(appContext)
    val reminderScheduler = ReminderScheduler(appContext)
    val calendarRepository = CalendarRepository(appContext)
}
