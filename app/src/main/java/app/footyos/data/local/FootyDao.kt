package app.footyos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FootyDao {
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
