package app.footyos.ui.components

import android.graphics.Bitmap
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.footyos.nutrition.GeminiMealResult
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.footyos.photos.MealPhotos
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.OutlinedTextField
import app.footyos.data.local.MealEntryEntity
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MealPhotoCard(onSave: suspend (MealEntryEntity, app.footyos.data.local.MealEstimateEntity?) -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var kcal by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var offlineJson by rememberSaveable { mutableStateOf<String?>(null) }
    var offlineDetails by rememberSaveable { mutableStateOf("") }
    var offlinePhoto by rememberSaveable { mutableStateOf("") }
    var offlineCreatedAt by rememberSaveable { mutableStateOf(0L) }
    var draftId by rememberSaveable { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    val context = LocalContext.current
    val container = (context.applicationContext as app.footyos.FootyOsApplication).container
    val configured by container.geminiKeys.configured.collectAsStateWithLifecycle()
    var geminiJson by rememberSaveable { mutableStateOf<String?>(null) }
    var geminiStatus by rememberSaveable { mutableStateOf("") }
    var appliedPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var geminiApplied by rememberSaveable { mutableStateOf(false) }
    val photos = remember(context) { MealPhotos(context.applicationContext) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    fun useGemini(json: String) {
        val result = GeminiMealResult.parse(json)
        val values = result.nutrients.rounded()
        name = result.name
        kcal = values[0].toString(); protein = values[1].toString(); carbs = values[2].toString(); fat = values[3].toString()
        geminiApplied = true
    }
    suspend fun analyze(photo: String, retry: Boolean = false) {
        busy = true
        geminiStatus = "loading"
        try {
            val result = container.geminiAnalysis.analyze(photo, retry)
            geminiStatus = result.status
            geminiJson = result.resultJson
            if (result.resultJson != null && appliedPhoto != photo) {
                if (kcal.isBlank() && name.isBlank()) useGemini(result.resultJson)
                appliedPhoto = photo
            }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { geminiStatus = "failed" }
        finally { busy = false }
    }
    LaunchedEffect(selected, configured) {
        val photo = selected
        if (photo == null) { geminiJson = null; geminiStatus = "" }
        else if (configured) analyze(photo)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val captured = pending
        pending = null
        if (success && captured != null && photos.file(captured).length() > 0) {
            busy = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { photos.compress(captured); photos.delete(selected) }
                    if (geminiApplied) { name = ""; kcal = ""; protein = ""; carbs = ""; fat = "" }
                    offlineJson = null
                    geminiJson = null; geminiApplied = false
                    selected = captured
                    error = null
                } catch (_: Exception) {
                    withContext(Dispatchers.IO) { photos.delete(captured) }
                    error = "Could not process this photo. Please retake it."
                } finally { busy = false }
            }
        } else {
            photos.delete(captured)
            if (success) error = "The camera did not save a photo. Try again."
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                var imported: String? = null
                try {
                    val result = withContext(Dispatchers.IO) {
                        val file = photos.create()
                        imported = file.name
                        context.contentResolver.openInputStream(uri).use { input ->
                            requireNotNull(input)
                            file.outputStream().use { output ->
                                val buffer = ByteArray(8192)
                                var total = 0L
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    total += read
                                    require(total <= 25L * 1024 * 1024) { "Image too large" }
                                    output.write(buffer, 0, read)
                                }
                            }
                        }
                        photos.compress(file.name)
                        photos.delete(selected)
                        file.name
                    }
                    if (geminiApplied) { name = ""; kcal = ""; protein = ""; carbs = ""; fat = "" }
                    offlineJson = null
                    geminiJson = null; geminiApplied = false
                    selected = result
                    error = null
                } catch (_: Exception) {
                    withContext(Dispatchers.IO) { photos.delete(imported) }
                    error = "Could not import photo. Choose a readable image smaller than 25 MB."
                } finally { busy = false }
            }
        }
    }
    val preview by produceState<Bitmap?>(null, selected) {
        value = null
        val name = selected
        if (name != null) value = withContext(Dispatchers.IO) { runCatching { photos.preview(name) }.getOrNull() }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add meal", style = MaterialTheme.typography.titleLarge)
            Text("Photos stay for 30 days. Your nutrition log stays saved.")
            preview?.let {
                Image(it.asImageBitmap(), "Captured meal", Modifier.fillMaxWidth().heightIn(max = 280.dp))
            }
            if (selected != null) {
                val result = geminiJson?.let { runCatching { GeminiMealResult.parse(it) }.getOrNull() }
                if (result != null) {
                    Text("Gemini estimate · ${result.lowerCalories.toInt()}–${result.upperCalories.toInt()} kcal")
                    Text(result.foods)
                    Text(result.assumptions)
                    Text("Approximate portions and nutrients. Correct the meal totals below before confirming.")
                    TextButton(enabled = !busy, onClick = { useGemini(requireNotNull(geminiJson)) }) { Text("Use Gemini totals") }
                } else {
                    Text(when (geminiStatus) {
                        "loading" -> "Estimating this meal…"
                        "quota" -> "Gemini quota reached. Use the offline calculator below."
                        "auth", "setup" -> "Check your Gemini key and project access in Gemini setup. Offline entry is available."
                        "pending" -> "The previous request was interrupted. Use offline entry or explicitly retry."
                        "expired" -> "This photo expired. Add a new photo or use offline entry."
                        "failed", "unavailable", "unreadable" -> "No usable Gemini estimate. Use the offline calculator below."
                        else -> "Offline mode. Use the calculator below or enable Gemini in setup."
                    })
                    if (configured && !busy && geminiStatus in listOf("quota", "auth", "pending", "failed", "unavailable", "unreadable")) {
                        TextButton(onClick = { scope.launch { analyze(requireNotNull(selected), retry = true) } }) { Text("Retry Gemini (sends another request)") }
                    }
                }
                TextButton(enabled = !busy, onClick = { photos.delete(selected); selected = null; error = null }) { Text("Remove photo") }
            }
            androidx.compose.runtime.key(draftId) {
                OfflineMealEditor(enabled = !busy && pending == null) { totals, details, description ->
                    val rounded = totals.rounded()
                    kcal = rounded[0].toString(); protein = rounded[1].toString(); carbs = rounded[2].toString(); fat = rounded[3].toString()
                    name = description
                    offlineJson = org.json.JSONArray(totals.values).toString()
                    offlineDetails = org.json.JSONObject().put("portions", org.json.JSONArray(details)).put("geminiShownBeforeOffline", geminiJson != null).toString()
                    geminiApplied = false
                    offlinePhoto = selected ?: "no-photo"
                    offlineCreatedAt = System.currentTimeMillis()
                }
            }
            if (offlineJson != null) Text("Offline estimate applied. Review the values below before saving. Portion measurements determine the result.")
            OutlinedTextField(name, { name = it }, label = { Text("Meal / foods and portions") }, enabled = !busy)
            OutlinedTextField(kcal, { kcal = it }, label = { Text("Calories") }, enabled = !busy)
            OutlinedTextField(protein, { protein = it }, label = { Text("Protein g") }, enabled = !busy)
            OutlinedTextField(carbs, { carbs = it }, label = { Text("Carbs g") }, enabled = !busy)
            OutlinedTextField(fat, { fat = it }, label = { Text("Fat g") }, enabled = !busy)
            val amounts = listOf(kcal, protein, carbs, fat).map { it.toIntOrNull() }
            Button(enabled = !busy && pending == null && name.isNotBlank() && amounts.all { it != null && it in 0..10000 }, onClick = {
                busy = true
                scope.launch {
                    try {
                        val offline = offlineJson?.takeIf { offlinePhoto == (selected ?: "no-photo") }?.let {
                            val values = org.json.JSONArray(it)
                            app.footyos.data.local.MealEstimateEntity(draftId, "offline", offlinePhoto,
                                values.getDouble(0), values.getDouble(1), values.getDouble(2), values.getDouble(3),
                                app.footyos.nutrition.OfflineNutrition.VERSION, offlineCreatedAt, offlineDetails)
                        }
                        onSave(MealEntryEntity(draftId, java.time.LocalDate.now().toString(), name.trim(), selected,
                            kcal.toInt(), protein.toInt(), carbs.toInt(), fat.toInt(),
                            source = if (geminiApplied) "gemini (user confirmed)" else if (offline != null) "offline (user confirmed)" else "manual"), offline)
                        geminiApplied = false
                        geminiJson = null
                        offlineJson = null
                        offlineDetails = ""
                        selected = null
                        name = ""; kcal = ""; protein = ""; carbs = ""; fat = ""
                        draftId = java.util.UUID.randomUUID().toString()
                        error = null
                    } catch (_: Exception) { error = "Meal could not be saved. Your draft is still here; try again." }
                    finally { busy = false }
                }
            }) { Text(if (busy) "Processing…" else "Confirm and save meal") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TextButton(enabled = pending == null && !busy, onClick = {
                try { gallery.launch("image/*") }
                catch (_: Exception) { error = "Could not open the photo picker." }
            }) { Text("Choose photo") }
            Button(enabled = pending == null && !busy, onClick = {
                error = null
                try {
                    val file = photos.create()
                    pending = file.name
                    camera.launch(photos.uri(file.name))
                } catch (_: Exception) {
                    photos.delete(pending)
                    pending = null
                    error = "Could not open the camera. Check that a camera app is available and try again."
                }
            }) { Text(if (selected == null) "Take meal photo" else "Retake photo") }
        }
    }
}

@Composable
fun SavedMealPhoto(name: String) {
    val context = LocalContext.current
    var expanded by rememberSaveable(name) { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide photo" else "View photo") }
    if (expanded) {
        val bitmap by produceState<Bitmap?>(null, name) {
            value = withContext(Dispatchers.IO) { runCatching { MealPhotos(context).preview(name) }.getOrNull() }
        }
        bitmap?.let { Image(it.asImageBitmap(), "Saved meal", Modifier.fillMaxWidth().heightIn(max = 240.dp)) }
            ?: Text("Photo unavailable or expired. Nutrition details are retained.")
    }
}
