package app.footyos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WeightEntity::class,
        MealEntryEntity::class,
        PhotoAnalysisEntity::class,
        MealEstimateEntity::class,
        EstimateComparisonEntity::class,
        NutritionDayEntity::class,
        WorkoutEntryEntity::class,
        PerformanceTestEntity::class,
        MatchEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class FootyDatabase : RoomDatabase() {
    abstract fun footyDao(): FootyDao
}

val NUTRITION_MIGRATION = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS meal_entries (id TEXT NOT NULL PRIMARY KEY, date TEXT NOT NULL, name TEXT NOT NULL, photoName TEXT, calories INTEGER NOT NULL, proteinGrams INTEGER NOT NULL, carbsGrams INTEGER NOT NULL, fatGrams INTEGER NOT NULL, source TEXT NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_entries_date ON meal_entries(date)")
    }
}

val ESTIMATE_MIGRATION = object : androidx.room.migration.Migration(2, 3) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS meal_estimates (mealId TEXT NOT NULL, source TEXT NOT NULL, photoReference TEXT NOT NULL, calories REAL NOT NULL, protein REAL NOT NULL, carbs REAL NOT NULL, fat REAL NOT NULL, estimatorVersion TEXT NOT NULL, createdAt INTEGER NOT NULL, inputDetails TEXT NOT NULL, PRIMARY KEY(mealId, source), FOREIGN KEY(mealId) REFERENCES meal_entries(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE TABLE IF NOT EXISTS estimate_comparisons (mealId TEXT NOT NULL PRIMARY KEY, score REAL, calorieDelta REAL NOT NULL, proteinDelta REAL NOT NULL, carbsDelta REAL NOT NULL, fatDelta REAL NOT NULL, formulaVersion TEXT NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(mealId) REFERENCES meal_entries(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
    }
}

val GEMINI_MIGRATION = object : androidx.room.migration.Migration(3, 4) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS photo_analysis (photoName TEXT NOT NULL PRIMARY KEY, status TEXT NOT NULL, resultJson TEXT, model TEXT NOT NULL, createdAt INTEGER NOT NULL)")
    }
}
