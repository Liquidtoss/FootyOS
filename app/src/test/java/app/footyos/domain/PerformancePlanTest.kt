package app.footyos.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class PerformancePlanTest {
    @Test
    fun soccerDaysReceiveHigherCalories() {
        assertEquals(2450, PerformancePlan.calories(DayOfWeek.TUESDAY))
        assertEquals(2500, PerformancePlan.calories(DayOfWeek.SATURDAY))
    }

    @Test
    fun slowLossRequestsSmallCalorieReduction() {
        assertEquals(-150, PerformancePlan.calorieAdjustment(0.20, 88.0))
    }

    @Test
    fun phasePlanProducesFutureGoalDate() {
        val date = PerformancePlan.plannedGoalDate(
            currentWeightKg = 89.0,
            from = LocalDate.of(2026, 8, 20),
        )
        assertEquals(true, date.isAfter(LocalDate.of(2027, 3, 1)))
    }
}
