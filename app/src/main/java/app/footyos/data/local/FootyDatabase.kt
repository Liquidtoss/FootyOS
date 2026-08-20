package app.footyos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WeightEntity::class,
        NutritionDayEntity::class,
        WorkoutEntryEntity::class,
        PerformanceTestEntity::class,
        MatchEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class FootyDatabase : RoomDatabase() {
    abstract fun footyDao(): FootyDao
}
