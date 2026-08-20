package app.footyos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.footyos.data.FootyRepository
import app.footyos.data.SettingsRepository
import app.footyos.data.UserSettings
import app.footyos.data.local.MatchEntity
import app.footyos.data.local.NutritionDayEntity
import app.footyos.data.local.PerformanceTestEntity
import app.footyos.data.local.WeightEntity
import app.footyos.domain.PerformancePlan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AppUiState(
    val settings: UserSettings = UserSettings(),
    val weights: List<WeightEntity> = emptyList(),
    val matches: List<MatchEntity> = emptyList(),
    val tests: List<PerformanceTestEntity> = emptyList(),
) {
    val currentWeightKg: Double
        get() = weights.lastOrNull()?.kilograms ?: settings.startingWeightKg

    val goalDate: LocalDate
        get() = PerformancePlan.plannedGoalDate(currentWeightKg)
}

class AppViewModel(
    private val repository: FootyRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<AppUiState> = combine(
        settingsRepository.settings,
        repository.observeWeights(),
        repository.observeMatches(),
        repository.observePerformanceTests(),
    ) { settings, weights, matches, tests ->
        AppUiState(settings, weights, matches, tests)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState(),
    )

    fun saveWeight(weightKg: Double) {
        viewModelScope.launch {
            repository.saveWeight(
                WeightEntity(LocalDate.now().toString(), weightKg),
            )
        }
    }

    fun saveNutrition(
        calories: Int,
        protein: Int,
        carbs: Int,
        fat: Int,
        waistCm: Double?,
        readiness: Int,
    ) {
        viewModelScope.launch {
            repository.saveNutrition(
                NutritionDayEntity(
                    date = LocalDate.now().toString(),
                    calories = calories,
                    proteinGrams = protein,
                    carbsGrams = carbs,
                    fatGrams = fat,
                    waistCm = waistCm,
                    readiness = readiness,
                ),
            )
        }
    }

    fun applyCalorieAdjustment(delta: Int) {
        viewModelScope.launch {
            val current = state.value.settings.calorieOffset
            settingsRepository.updateCalories(current + delta)
        }
    }

    class Factory(
        private val repository: FootyRepository,
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AppViewModel(repository, settingsRepository) as T
    }
}
