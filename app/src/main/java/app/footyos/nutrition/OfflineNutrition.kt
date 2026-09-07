package app.footyos.nutrition

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** Values are grams except calories. No photo recognition is implied. */
data class Nutrients(val calories: Double, val protein: Double, val carbs: Double, val fat: Double) {
    init { require(values.all { it.isFinite() && it in 0.0..10000.0 }) }
    val values get() = listOf(calories, protein, carbs, fat)
    fun rounded() = values.map { it.roundToInt() }
}

data class Portion(val food: String, val grams: Double, val per100g: Nutrients) {
    init { require(food.isNotBlank()); require(grams.isFinite() && grams > 0 && grams <= 5000) }
}

object OfflineNutrition {
    const val VERSION = "label-portions-v1"
    fun estimate(portions: List<Portion>): Nutrients {
        require(portions.isNotEmpty())
        val totals = (0..3).map { i -> portions.sumOf { it.per100g.values[i] * it.grams / 100.0 } }
        return Nutrients(totals[0], totals[1], totals[2], totals[3])
    }
}

data class EstimateAgreement(val score: Double, val deltas: List<Double>)

object NutritionAgreement {
    const val VERSION = "symmetric-max-v1"
    /** Agreement, NOT accuracy. Both-zero dimensions are excluded; all-zero is unscored. */
    fun compare(offline: Nutrients, gemini: Nutrients): EstimateAgreement? {
        val pairs = offline.values.zip(gemini.values)
        val active = pairs.filter { max(it.first, it.second) > 0 }
        if (active.isEmpty()) return null
        val score = 100 * active.map { (a, b) -> 1 - abs(a - b) / max(a, b) }.average()
        return EstimateAgreement(score.coerceIn(0.0, 100.0), pairs.map { (a, b) -> a - b })
    }
}
