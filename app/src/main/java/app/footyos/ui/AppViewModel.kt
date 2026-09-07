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
import app.footyos.data.local.WorkoutEntryEntity
import app.footyos.domain.PerformancePlan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TrendSummary(
    val weeklyLossKg: Double,
    val calorieAdjustment: Int,
)

data class AppUiState(
    val settings: UserSettings = UserSettings(),
    val weights: List<WeightEntity> = emptyList(),
    val matches: List<MatchEntity> = emptyList(),
    val tests: List<PerformanceTestEntity> = emptyList(),
    val workouts: List<WorkoutEntryEntity> = emptyList(),
) {
    val currentWeightKg: Double
        get() = weights.lastOrNull()?.kilograms ?: settings.startingWeightKg

    val goalDate: LocalDate
        get() = PerformancePlan.plannedGoalDate(currentWeightKg)

    val trend: TrendSummary?
        get() {
            if (weights.size < 10) return null
            val recent = weights.takeLast(7).map { it.kilograms }.average()
            val previous = weights.dropLast(7).takeLast(7)
            if (previous.size < 4) return null
            val previousAverage = previous.map { it.kilograms }.average()
            val weeklyLoss = PerformancePlan.weeklyLoss(previousAverage, recent)
            return TrendSummary(
                weeklyLossKg = weeklyLoss,
                calorieAdjustment = PerformancePlan.calorieAdjustment(weeklyLoss, currentWeightKg),
            )
        }
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
        repository.observeWorkoutEntries(),
    ) { settings, weights, matches, tests, workouts ->
        AppUiState(settings, weights, matches, tests, workouts)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState(),
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val today = kotlinx.coroutines.flow.flow {
        while (true) {
            emit(LocalDate.now().toString())
            kotlinx.coroutines.delay(30_000)
        }
    }
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val nutrition = today.distinctUntilChanged().flatMapLatest { date ->
        combine(repository.observeMeals(date), repository.observeNutrition(date)) { meals, baseline ->
            meals to baseline
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<app.footyos.data.local.MealEntryEntity>() to null)

    suspend fun saveMeal(meal: app.footyos.data.local.MealEntryEntity, offline: app.footyos.data.local.MealEstimateEntity?) {
        require(meal.name.isNotBlank())
        require(listOf(meal.calories, meal.proteinGrams, meal.carbsGrams, meal.fatGrams).all { it in 0..10000 })
        repository.saveMeal(meal, offline)
    }
    fun deleteMeal(id: String) { viewModelScope.launch { repository.deleteMeal(id) } }
    fun saveMacroGoals(protein: Int, carbs: Int, fat: Int) {
        viewModelScope.launch { settingsRepository.updateMacros(protein, carbs, fat) }
    }

    fun saveWeight(weightKg: Double) {
        viewModelScope.launch {
            repository.saveWeight(WeightEntity(LocalDate.now().toString(), weightKg))
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

    fun saveWorkout(
        exerciseId: String,
        loadLb: Double?,
        sets: Int,
        reps: Int?,
        rpe: Int?,
    ) {
        viewModelScope.launch {
            repository.saveWorkoutEntry(
                WorkoutEntryEntity(
                    date = LocalDate.now().toString(),
                    exerciseId = exerciseId,
                    loadLb = loadLb,
                    sets = sets,
                    reps = reps,
                    rpe = rpe,
                ),
            )
        }
    }

    fun savePerformance(sprint10: Double?, sprint20: Double?, sprint30: Double?, jumpCm: Double?) {
        viewModelScope.launch {
            repository.savePerformanceTest(
                PerformanceTestEntity(
                    date = LocalDate.now().toString(),
                    sprint10 = sprint10,
                    sprint20 = sprint20,
                    sprint30 = sprint30,
                    broadJumpCm = jumpCm,
                ),
            )
        }
    }

    fun saveMatch(match: MatchEntity) {
        viewModelScope.launch { repository.saveMatch(match) }
    }

    fun applyCalorieAdjustment(delta: Int) {
        viewModelScope.launch {
            settingsRepository.updateCalories(state.value.settings.calorieOffset + delta)
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
