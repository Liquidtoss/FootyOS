package app.footyos.nutrition

import org.json.JSONObject

/** Validated model output. Values remain estimates and require user confirmation. */
data class GeminiMealResult(
    val name: String,
    val nutrients: Nutrients,
    val lowerCalories: Double,
    val upperCalories: Double,
    val assumptions: String,
    val foods: String,
) {
    companion object {
        fun parse(json: String): GeminiMealResult {
            require(json.length <= 50000)
            val root = JSONObject(json)
            val name = root.getString("name").also { require(it.isNotBlank() && it.length <= 200) }
            val n = Nutrients(root.getDouble("calories"), root.getDouble("protein"), root.getDouble("carbs"), root.getDouble("fat"))
            val low = root.getDouble("lowerCalories")
            val high = root.getDouble("upperCalories")
            require(low.isFinite() && high.isFinite() && low >= 0 && low <= n.calories && n.calories <= high && high <= 10000)
            val assumptions = root.getString("assumptions").also { require(it.isNotBlank() && it.length <= 2000) }
            val items = root.getJSONArray("foods")
            require(items.length() in 1..30)
            val foods = (0 until items.length()).joinToString("\n") {
                val food = items.getJSONObject(it)
                val label = food.getString("name").also { label -> require(label.isNotBlank() && label.length <= 200) }
                val grams = food.getDouble("grams").also { grams -> require(grams.isFinite() && grams > 0 && grams <= 5000) }
                "$label · ${grams.toInt()} g (estimated)"
            }
            return GeminiMealResult(name, n, low, high, assumptions, foods)
        }
    }
}
