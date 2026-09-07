package app.footyos.data

import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import app.footyos.data.local.*
import app.footyos.photos.MealPhotos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class NutritionStorageTest {
    @Test fun migrationPreservesBaselineAndMealUpsertsDoNotDuplicate() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "nutrition-migration-test.db"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open("app.footyos.data.local.FootyDatabase/1.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        context.openOrCreateDatabase(name, 0, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            db.execSQL("INSERT INTO nutrition_days VALUES ('2026-09-06', 500, 30, 50, 20, NULL, 3)")
            db.version = 1
        }
        val db = Room.databaseBuilder(context, FootyDatabase::class.java, name).addMigrations(NUTRITION_MIGRATION, ESTIMATE_MIGRATION, GEMINI_MIGRATION).build()
        try {
            val dao = db.footyDao()
            assertEquals(500, dao.observeNutrition("2026-09-06").first()!!.calories)
            val meal = MealEntryEntity("stable-id", "2026-09-06", "Lunch", null, 600, 40, 60, 22)
            dao.saveMeal(meal)
            dao.saveMeal(meal.copy(calories = 650))
            assertEquals(1, dao.observeMeals("2026-09-06").first().size)
            assertEquals(650, dao.observeMeals("2026-09-06").first().single().calories)
            assertTrue(dao.observeMeals("2026-09-07").first().isEmpty())
            assertEquals(500, dao.observeNutrition("2026-09-06").first()!!.calories)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun storesAgreementOnceWithoutChangingConfirmedMeal() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, FootyDatabase::class.java).build()
        try {
            val dao = db.footyDao()
            val meal = MealEntryEntity("comparison", "2026-09-06", "Lunch", "meal_test.jpg", 550, 30, 60, 20)
            val offline = MealEstimateEntity(meal.id, "offline", "meal_test.jpg", 500.0, 30.0, 60.0, 20.0, "label-portions-v1", 1, "label inputs")
            dao.saveMealWithOffline(meal, offline)
            assertNull(dao.comparison(meal.id))
            val gemini = offline.copy(source = "gemini", calories = 600.0, estimatorVersion = "test-model", createdAt = 2)
            dao.recordGemini(gemini)
            assertEquals(95.833333, dao.comparison(meal.id)!!.score!!, 0.00001)
            dao.recordGemini(gemini.copy(calories = 900.0))
            assertEquals(600.0, dao.estimate(meal.id, "gemini")!!.calories, 0.0)
            assertEquals(550, dao.observeMeals(meal.date).first().single().calories)
            dao.deleteMeal(meal.id)
            assertNull(dao.comparison(meal.id))
            assertNull(dao.estimate(meal.id, "offline"))
        } finally { db.close() }
    }

    @Test fun mismatchedPhotoDoesNotReceiveScore() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, FootyDatabase::class.java).build()
        try {
            val dao = db.footyDao()
            val meal = MealEntryEntity("mismatch", "2026-09-06", "Lunch", "meal_a.jpg", 500, 30, 60, 20)
            val offline = MealEstimateEntity(meal.id, "offline", "meal_a.jpg", 500.0, 30.0, 60.0, 20.0, "v1", 1, "inputs")
            dao.saveMealWithOffline(meal, offline)
            dao.recordGemini(offline.copy(source = "gemini", photoReference = "meal_b.jpg"))
            assertNull(dao.comparison(meal.id))
        } finally { db.close() }
    }

    @Test fun compressesAndExpiresPhotosWithoutTouchingRecentFiles() {
        val photos = MealPhotos(InstrumentationRegistry.getInstrumentation().targetContext)
        val old = photos.create()
        val recent = photos.create()
        try {
            val bitmap = Bitmap.createBitmap(2400, 1800, Bitmap.Config.ARGB_8888)
            recent.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            bitmap.recycle()
            photos.compress(recent.name)
            val preview = photos.preview(recent.name)!!
            assertTrue(maxOf(preview.width, preview.height) <= 1200)
            preview.recycle()
            old.setLastModified(System.currentTimeMillis() - MealPhotos.RETENTION_MS - 1000)
            photos.prune()
            assertFalse(old.exists())
            assertTrue(recent.exists())
        } finally { old.delete(); recent.delete() }
    }
}
