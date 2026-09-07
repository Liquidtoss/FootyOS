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

@Entity(tableName = "meal_entries", indices = [androidx.room.Index("date")])
data class MealEntryEntity(
    @PrimaryKey val id: String,
    val date: String,
    val name: String,
    val photoName: String?,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val source: String = "manual",
)

@Entity(tableName = "meal_estimates", primaryKeys = ["mealId", "source"],
    foreignKeys = [androidx.room.ForeignKey(entity = MealEntryEntity::class,
        parentColumns = ["id"], childColumns = ["mealId"], onDelete = androidx.room.ForeignKey.CASCADE)])
data class MealEstimateEntity(
    val mealId: String,
    val source: String,
    val photoReference: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val estimatorVersion: String,
    val createdAt: Long,
    val inputDetails: String,
) {
    fun nutrients() = app.footyos.nutrition.Nutrients(calories, protein, carbs, fat)
}

@Entity(tableName = "estimate_comparisons",
    foreignKeys = [androidx.room.ForeignKey(entity = MealEntryEntity::class,
        parentColumns = ["id"], childColumns = ["mealId"], onDelete = androidx.room.ForeignKey.CASCADE)])
data class EstimateComparisonEntity(
    @PrimaryKey val mealId: String,
    val score: Double?,
    val calorieDelta: Double,
    val proteinDelta: Double,
    val carbsDelta: Double,
    val fatDelta: Double,
    val formulaVersion: String,
    val createdAt: Long,
)

/** Durable request marker prevents automatic retries after process death or a timeout. */
@Entity(tableName = "photo_analysis")
data class PhotoAnalysisEntity(
    @PrimaryKey val photoName: String,
    val status: String,
    val resultJson: String?,
    val model: String,
    val createdAt: Long,
)
