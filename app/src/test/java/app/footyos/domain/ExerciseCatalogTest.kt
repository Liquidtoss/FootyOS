package app.footyos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseCatalogTest {
    @Test
    fun everyExerciseHasAUniqueSecureReference() {
        val references = ExerciseCatalog.exercises.map { it.reference }

        assertTrue(references.all { it.source.isNotBlank() })
        assertTrue(references.all { it.url.startsWith("https://") })
        assertEquals(references.size, references.map { it.url }.distinct().size)
    }

    @Test
    fun everyScheduledExerciseExistsInTheCatalog() {
        val scheduledIds = ExerciseCatalog.monday + ExerciseCatalog.wednesday

        assertTrue(scheduledIds.all { ExerciseCatalog.byId(it) != null })
    }
}
