package app.footyos.data

import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import app.footyos.data.local.*
import app.footyos.nutrition.*
import app.footyos.photos.MealPhotos
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class GeminiIntegrationTest {
    private val json = """{"name":"Test meal","calories":600,"protein":30,"carbs":60,"fat":20,"lowerCalories":450,"upperCalories":750,"assumptions":"Oil and portions uncertain","foods":[{"name":"Test food","grams":250}]}"""

    @Test fun successfulAnalysisIsCachedAndLinkedOnConfirmation() = runBlocking {
        fixture { db, photos, keys ->
            var calls = 0
            val service = GeminiAnalysis(db.footyDao(), photos, keys, GeminiTransport { _, _, details -> assertEquals("Coconut milk curry", details); calls++; GeminiResponse(json, "test-model") })
            val file = photos.create().apply { writeBytes(byteArrayOf(1, 2, 3)) }
            try {
                val first = service.analyze(file.name, mealContext = "Coconut milk curry")
                assertEquals("complete", first.status)
                assertEquals(first, service.analyze(file.name))
                assertEquals(1, calls)
                val meal = MealEntryEntity("gemini-test", "2026-09-07", "Confirmed", file.name, 550, 30, 60, 20)
                val offline = MealEstimateEntity(meal.id, "offline", file.name, 500.0, 30.0, 60.0, 20.0, "v1", 1, "inputs")
                db.footyDao().saveConfirmedMeal(meal, offline)
                assertEquals(600.0, db.footyDao().estimate(meal.id, "gemini")!!.calories, 0.0)
                assertEquals(95.833333, db.footyDao().comparison(meal.id)!!.score!!, 0.00001)
            } finally { file.delete() }
        }
    }

    @Test fun quotaAndInterruptedRequestsNeverAutomaticallyRetry() = runBlocking {
        fixture { db, photos, keys ->
            var calls = 0
            val service = GeminiAnalysis(db.footyDao(), photos, keys, GeminiTransport { _, _, _ -> calls++; throw GeminiFailure("quota") })
            val file = photos.create().apply { writeBytes(byteArrayOf(1)) }
            try {
                assertEquals("quota", service.analyze(file.name).status)
                assertEquals("quota", service.analyze(file.name).status)
                assertEquals(1, calls)
                service.analyze(file.name, retry = true)
                assertEquals(2, calls)
                db.footyDao().saveAnalysis(PhotoAnalysisEntity(file.name, "pending", null, "test", 1))
                assertEquals("pending", service.analyze(file.name).status)
                assertEquals(2, calls)
            } finally { file.delete() }
        }
    }

    @Test fun explicitCorrectionsRefreshTotalsAndFailurePreservesLastResult() = runBlocking {
        fixture { db, photos, keys ->
            var calls = 0
            val revised = json.replace("\"calories\":600", "\"calories\":500")
            val service = GeminiAnalysis(db.footyDao(), photos, keys, GeminiTransport { _, _, context ->
                calls++
                when (calls) {
                    1 -> GeminiResponse(json, "test-model")
                    2 -> { assertEquals("No coconut milk", context); GeminiResponse(revised, "test-model") }
                    else -> throw GeminiFailure("quota")
                }
            })
            val file = photos.create().apply { writeBytes(byteArrayOf(1)) }
            try {
                service.analyze(file.name)
                assertEquals(revised, service.analyze(file.name, mealContext = "No coconut milk", reestimate = true).resultJson)
                assertEquals(revised, service.analyze(file.name).resultJson)
                assertEquals(2, calls)
                val failed = service.analyze(file.name, mealContext = "Less rice", reestimate = true)
                assertEquals("quota", failed.status)
                assertEquals(revised, failed.resultJson)
                assertEquals("complete", service.cached(file.name)!!.status)
                assertEquals(revised, service.cached(file.name)!!.resultJson)
            } finally { file.delete() }
        }
    }

    @Test fun rejectsInvalidRangesAndNonfood() {
        assertThrows(IllegalArgumentException::class.java) { GeminiMealResult.parse(json.replace("\"lowerCalories\":450", "\"lowerCalories\":700")) }
        assertThrows(IllegalArgumentException::class.java) { GeminiMealResult.parse(json.replace("[{\"name\":\"Test food\",\"grams\":250}]", "[]")) }
    }

    private suspend fun fixture(block: suspend (FootyDatabase, MealPhotos, GeminiKeyStore) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "gemini-test-${java.util.UUID.randomUUID()}").apply { mkdirs() }
        val isolated = object : ContextWrapper(context) { override fun getNoBackupFilesDir() = directory }
        val keys = GeminiKeyStore(isolated)
        val db = Room.inMemoryDatabaseBuilder(context, FootyDatabase::class.java).build()
        try {
            keys.save("test-key-never-sent-to-network")
            assertEquals("test-key-never-sent-to-network", keys.read())
            assertFalse(File(directory, "gemini-key.enc").readText().contains("test-key-never-sent-to-network"))
            block(db, MealPhotos(context), keys)
        } finally { keys.clear(); db.close(); directory.deleteRecursively() }
    }
}
