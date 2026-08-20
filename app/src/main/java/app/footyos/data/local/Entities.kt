package app.footyos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weights")
data class WeightEntity(
    @PrimaryKey val date: String,
    val kilograms: Double,
)

@Entity(tableName = "nutrition_days")
data class NutritionDayEntity(
    @PrimaryKey val date: String,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val waistCm: Double?,
    val readiness: Int,
)

@Entity(tableName = "workout_entries")
data class WorkoutEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val exerciseId: String,
    val loadLb: Double?,
    val sets: Int,
    val reps: Int?,
    val rpe: Int?,
)

@Entity(tableName = "performance_tests")
data class PerformanceTestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val sprint10: Double?,
    val sprint20: Double?,
    val sprint30: Double?,
    val broadJumpCm: Double?,
)

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val minutes: Int,
    val rating: Int?,
    val energy: Int?,
    val speed: Int?,
    val firstTouch: Int?,
    val scanning: Int?,
    val weakFoot: Int?,
    val dribbling: Int?,
    val goals: Int,
    val assists: Int,
    val notes: String,
)
