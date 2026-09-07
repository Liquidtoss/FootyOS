package app.footyos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FootyDao {
    @Query("SELECT * FROM photo_analysis WHERE photoName = :photo")
    suspend fun photoAnalysis(photo: String): PhotoAnalysisEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun claimAnalysis(value: PhotoAnalysisEntity): Long

    @androidx.room.Upsert
    suspend fun saveAnalysis(value: PhotoAnalysisEntity)

    @androidx.room.Transaction
    suspend fun saveConfirmedMeal(meal: MealEntryEntity, offline: MealEstimateEntity?) {
        saveMealWithOffline(meal, offline)
        val result = meal.photoName?.let { photoAnalysis(it) }
        if (result?.status == "complete" && result.resultJson != null) {
            val n = app.footyos.nutrition.GeminiMealResult.parse(result.resultJson).nutrients
            recordGemini(MealEstimateEntity(meal.id, "gemini", result.photoName, n.calories, n.protein, n.carbs, n.fat, result.model, result.createdAt, result.resultJson))
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEstimate(estimate: MealEstimateEntity)

    @Query("SELECT * FROM meal_estimates WHERE mealId = :id AND source = :source")
    suspend fun estimate(id: String, source: String): MealEstimateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveComparison(comparison: EstimateComparisonEntity)

    @Query("SELECT * FROM estimate_comparisons WHERE mealId = :id")
    suspend fun comparison(id: String): EstimateComparisonEntity?

    @androidx.room.Transaction
    suspend fun saveMealWithOffline(meal: MealEntryEntity, offline: MealEstimateEntity?) {
        require(offline == null || (offline.mealId == meal.id && offline.source == "offline" && offline.photoReference == (meal.photoName ?: "no-photo")))
        saveMeal(meal)
        if (offline != null) {
            offline.nutrients()
            insertEstimate(offline)
            updateAgreement(meal.id)
        }
    }

    @androidx.room.Transaction
    suspend fun recordGemini(estimate: MealEstimateEntity) {
        require(estimate.source == "gemini" && estimate.estimatorVersion.isNotBlank())
        estimate.nutrients()
        insertEstimate(estimate)
        updateAgreement(estimate.mealId)
    }

    suspend fun updateAgreement(id: String) {
        val offline = estimate(id, "offline") ?: return
        val gemini = estimate(id, "gemini") ?: return
        if (offline.photoReference != gemini.photoReference || offline.photoReference == "no-photo") return
        val a = offline.nutrients()
        val b = gemini.nutrients()
        val result = app.footyos.nutrition.NutritionAgreement.compare(a, b)
        val deltas = a.values.zip(b.values).map { (x, y) -> x - y }
        saveComparison(EstimateComparisonEntity(id, result?.score, deltas[0], deltas[1], deltas[2], deltas[3], app.footyos.nutrition.NutritionAgreement.VERSION, System.currentTimeMillis()))
    }

    @Query("SELECT * FROM meal_entries WHERE date = :date ORDER BY rowid DESC")
    fun observeMeals(date: String): Flow<List<MealEntryEntity>>

    @androidx.room.Upsert
    suspend fun saveMeal(meal: MealEntryEntity)

    @Query("DELETE FROM meal_entries WHERE id = :id")
    suspend fun deleteMeal(id: String)

    @Query("SELECT * FROM weights ORDER BY date")
    fun observeWeights(): Flow<List<WeightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeight(value: WeightEntity)

    @Query("SELECT * FROM nutrition_days WHERE date = :date LIMIT 1")
    fun observeNutrition(date: String): Flow<NutritionDayEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNutrition(value: NutritionDayEntity)

    @Query("SELECT * FROM workout_entries ORDER BY date DESC, id DESC")
    fun observeWorkoutEntries(): Flow<List<WorkoutEntryEntity>>

    @Insert
    suspend fun insertWorkoutEntry(value: WorkoutEntryEntity)

    @Query("SELECT * FROM performance_tests ORDER BY date")
    fun observePerformanceTests(): Flow<List<PerformanceTestEntity>>

    @Insert
    suspend fun insertPerformanceTest(value: PerformanceTestEntity)

    @Query("SELECT * FROM matches ORDER BY date DESC, id DESC")
    fun observeMatches(): Flow<List<MatchEntity>>

    @Insert
    suspend fun insertMatch(value: MatchEntity)
}
