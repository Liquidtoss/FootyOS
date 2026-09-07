package app.footyos.data

import app.footyos.data.local.FootyDao
import app.footyos.data.local.MatchEntity
import app.footyos.data.local.NutritionDayEntity
import app.footyos.data.local.PerformanceTestEntity
import app.footyos.data.local.WeightEntity
import app.footyos.data.local.WorkoutEntryEntity
import kotlinx.coroutines.flow.Flow

class FootyRepository(private val dao: FootyDao) {
    fun observeMeals(date: String) = dao.observeMeals(date)
    suspend fun saveMeal(meal: app.footyos.data.local.MealEntryEntity, offline: app.footyos.data.local.MealEstimateEntity? = null) = dao.saveConfirmedMeal(meal, offline)
    suspend fun recordGeminiEstimate(estimate: app.footyos.data.local.MealEstimateEntity) = dao.recordGemini(estimate)
    suspend fun deleteMeal(id: String) = dao.deleteMeal(id)
    fun observeWeights(): Flow<List<WeightEntity>> = dao.observeWeights()
    fun observeNutrition(date: String): Flow<NutritionDayEntity?> = dao.observeNutrition(date)
    fun observeWorkoutEntries(): Flow<List<WorkoutEntryEntity>> = dao.observeWorkoutEntries()
    fun observePerformanceTests(): Flow<List<PerformanceTestEntity>> = dao.observePerformanceTests()
    fun observeMatches(): Flow<List<MatchEntity>> = dao.observeMatches()

    suspend fun saveWeight(value: WeightEntity) = dao.upsertWeight(value)
    suspend fun saveNutrition(value: NutritionDayEntity) = dao.upsertNutrition(value)
    suspend fun saveWorkoutEntry(value: WorkoutEntryEntity) = dao.insertWorkoutEntry(value)
    suspend fun savePerformanceTest(value: PerformanceTestEntity) = dao.insertPerformanceTest(value)
    suspend fun saveMatch(value: MatchEntity) = dao.insertMatch(value)
}
