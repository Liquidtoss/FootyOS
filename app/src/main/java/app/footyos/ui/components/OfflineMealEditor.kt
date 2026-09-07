package app.footyos.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.footyos.nutrition.*
import org.json.JSONArray
import org.json.JSONObject

/** Input stays local. Per-100g values come from the user's food label, not invented defaults. */
@Composable
fun OfflineMealEditor(enabled: Boolean, onApply: (Nutrients, String, String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var food by rememberSaveable { mutableStateOf("") }
    var grams by rememberSaveable { mutableStateOf("") }
    var kcal by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var rows by rememberSaveable { mutableStateOf("[]") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    TextButton(enabled = enabled, onClick = { expanded = !expanded }) { Text("Estimate offline from foods and portions") }
    if (expanded) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Enter each food’s label values per 100 g and the amount eaten. Include oils and sauces. No photo recognition or internet is used.")
        OutlinedTextField(food, { food = it }, label = { Text("Food") }, enabled = enabled)
        OutlinedTextField(grams, { grams = it }, label = { Text("Amount eaten (g)") }, enabled = enabled)
        OutlinedTextField(kcal, { kcal = it }, label = { Text("kcal per 100 g") }, enabled = enabled)
        OutlinedTextField(protein, { protein = it }, label = { Text("Protein per 100 g") }, enabled = enabled)
        OutlinedTextField(carbs, { carbs = it }, label = { Text("Carbs per 100 g") }, enabled = enabled)
        OutlinedTextField(fat, { fat = it }, label = { Text("Fat per 100 g") }, enabled = enabled)
        val portion = runCatching { Portion(food.trim(), grams.toDouble(), Nutrients(kcal.toDouble(), protein.toDouble(), carbs.toDouble(), fat.toDouble())) }.getOrNull()
        Button(enabled = enabled && portion != null, onClick = {
            val p = requireNotNull(portion)
            val array = JSONArray(rows)
            array.put(JSONObject().put("food", p.food).put("grams", p.grams).put("values", JSONArray(p.per100g.values)))
            rows = array.toString()
            food = ""; grams = ""; kcal = ""; protein = ""; carbs = ""; fat = ""
            error = null
        }) { Text("Add food") }
        val array = JSONArray(rows)
        for (i in 0 until array.length()) {
            val row = array.getJSONObject(i)
            Text("${row.getString("food")} · ${row.getDouble("grams")} g")
            TextButton(enabled = enabled, onClick = { val updated = JSONArray(rows); updated.remove(i); rows = updated.toString() }) { Text("Remove food") }
        }
        Button(enabled = enabled && array.length() > 0, onClick = {
            try {
                val portions = (0 until array.length()).map { i ->
                    val row = array.getJSONObject(i)
                    val values = row.getJSONArray("values")
                    Portion(row.getString("food"), row.getDouble("grams"), Nutrients(values.getDouble(0), values.getDouble(1), values.getDouble(2), values.getDouble(3)))
                }
                onApply(OfflineNutrition.estimate(portions), rows, portions.joinToString { "${it.food} ${it.grams} g" })
                expanded = false
                error = null
            } catch (_: IllegalArgumentException) { error = "These portions exceed the meal limit. Check quantities and label values." }
        }) { Text("Use offline estimate") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
