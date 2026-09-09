package app.footyos.domain

import app.footyos.data.local.WorkoutEntryEntity
import org.junit.Assert.*
import org.junit.Test

class TrainingRecordsTest {
    private fun entry(load: Double? = 20.0, reps: Int? = 8, sets: Int = 3, id: Long = 1, exercise: String = "row") =
        WorkoutEntryEntity(id, "2026-09-07", exercise, load, sets, reps, 8)

    @Test fun newLoadAndMatchingLoadRepsHaveIndependentRecords() {
        val history = listOf(entry(20.0, 8), entry(30.0, 6))
        assertEquals(listOf("Rep PB • +1 reps/set at the same load"), TrainingRecords.achievements(entry(20.0, 9), history))
        assertEquals(listOf("Weight PB • +5 lb"), TrainingRecords.achievements(entry(35.0, 4), history))
        assertTrue(TrainingRecords.achievements(entry(25.0, 20), history).isEmpty())
    }
    @Test fun firstSessionIsABaselineAndTiesAreNotRecords() {
        assertEquals(listOf("First session • baseline set"), TrainingRecords.achievements(entry(), emptyList()))
        assertTrue(TrainingRecords.achievements(entry(), listOf(entry())).isEmpty())
        assertEquals(listOf("First session • baseline set"), TrainingRecords.achievements(entry(), listOf(entry(exercise = "squat"))))
    }
    @Test fun historySortsSameDayEntriesByIdAndIgnoresEmptySets() {
        assertEquals(listOf(3L, 1L), TrainingRecords.history("row", listOf(entry(id = 1), entry(id = 4, sets = 0), entry(id = 3))).map { it.id })
    }
    @Test fun bodyweightAndTimedRecordsDoNotInventWeightsOrReps() {
        assertEquals(listOf("Sets PB • 4 sets"), TrainingRecords.achievements(entry(null, null, 4), listOf(entry(null, null))))
        assertEquals(listOf("Rep PB • +1 reps/set at the same load"), TrainingRecords.achievements(entry(0.0, 9), listOf(entry(null, 8))))
    }
}
