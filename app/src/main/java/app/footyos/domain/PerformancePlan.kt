package app.footyos.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class WeightPhase(
    val floorKg: Double,
    val ceilingKg: Double,
    val targetWeeklyLossKg: ClosedFloatingPointRange<Double>,
    val plannedWeeklyLossKg: Double,
)

object PerformancePlan {
    const val targetWeightKg = 67.0

    val phases = listOf(
        WeightPhase(80.0, Double.MAX_VALUE, 0.60..0.80, 0.70),
        WeightPhase(74.0, 80.0, 0.50..0.70, 0.60),
        WeightPhase(67.0, 74.0, 0.35..0.55, 0.45),
    )

    fun calories(day: DayOfWeek, offset: Int = 0): Int =
        when (day) {
            DayOfWeek.MONDAY -> 2200
            DayOfWeek.TUESDAY -> 2450
            DayOfWeek.WEDNESDAY -> 2200
            DayOfWeek.THURSDAY -> 2450
            DayOfWeek.FRIDAY -> 2000
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> 2500
        } + offset

    fun phase(weightKg: Double): WeightPhase =
        phases.firstOrNull { weightKg > it.floorKg } ?: phases.last()

    fun calorieAdjustment(weeklyLossKg: Double, weightKg: Double): Int {
        val range = phase(weightKg).targetWeeklyLossKg
        return when {
            weeklyLossKg < range.start - 0.08 -> -150
            weeklyLossKg > range.endInclusive + 0.12 -> 150
            else -> 0
        }
    }

    fun plannedGoalDate(currentWeightKg: Double, from: LocalDate = LocalDate.now()): LocalDate {
        var weight = currentWeightKg
        var weeks = 0.0

        phases.forEach { phase ->
            if (weight > phase.floorKg) {
                val next = maxOf(phase.floorKg, targetWeightKg)
                weeks += (weight - next) / phase.plannedWeeklyLossKg
                weight = next
            }
        }

        return from.plusDays(kotlin.math.ceil(weeks * 7).toLong())
    }

    fun weeklyLoss(
        olderAverageKg: Double,
        newerAverageKg: Double,
        daysBetween: Long = 7,
    ): Double {
        if (daysBetween <= 0) return 0.0
        return (olderAverageKg - newerAverageKg) * 7.0 / daysBetween
    }

    fun daysToGoal(current: LocalDate, goal: LocalDate): Long =
        ChronoUnit.DAYS.between(current, goal)
}
