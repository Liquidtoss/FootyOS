package app.footyos.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("footyos_settings")

data class UserSettings(
    val startingWeightKg: Double = 89.0,
    val targetWeightKg: Double = 67.0,
    val proteinTargetGrams: Int = 170,
    val calorieOffset: Int = 0,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val startWeight = doublePreferencesKey("start_weight")
        val targetWeight = doublePreferencesKey("target_weight")
        val protein = intPreferencesKey("protein_target")
        val calorieOffset = intPreferencesKey("calorie_offset")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { values ->
        UserSettings(
            startingWeightKg = values[Keys.startWeight] ?: 89.0,
            targetWeightKg = values[Keys.targetWeight] ?: 67.0,
            proteinTargetGrams = values[Keys.protein] ?: 170,
            calorieOffset = values[Keys.calorieOffset] ?: 0,
        )
    }

    suspend fun updateCalories(offset: Int) {
        context.dataStore.edit { it[Keys.calorieOffset] = offset }
    }
}
