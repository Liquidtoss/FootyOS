package app.footyos.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.footyos.FootyOsApplication
import app.footyos.calendar.CalendarEventDraft
import app.footyos.data.local.MatchEntity
import app.footyos.domain.Exercise
import app.footyos.domain.ExerciseCatalog
import app.footyos.domain.PerformancePlan
import app.footyos.reminders.ReminderRequest
import app.footyos.ui.components.ExerciseReferenceDialog
import app.footyos.ui.components.WeightChart
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime

private enum class Destination(val label: String) {
    Today("Today"), Nutrition("Nutrition"), Training("Training"), Soccer("Soccer"), Progress("Progress"), Schedule("Schedule")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootyOsApp(initialUri: Uri?) {
    val context = LocalContext.current
    val container = (context.applicationContext as FootyOsApplication).container
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory(container.repository, container.settingsRepository))
    val state by viewModel.state.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(destinationFromUri(initialUri) ?: Destination.Today) }

    LaunchedEffect(initialUri) { destinationFromUri(initialUri)?.let { destination = it } }

    Scaffold(
        topBar = { TopAppBar(title = { Column { Text("FootyOS"); Text(destination.label) } }) },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(iconFor(item), item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (destination) {
                Destination.Today -> TodayScreen(state, viewModel)
                Destination.Nutrition -> NutritionScreen(state, viewModel)
                Destination.Training -> TrainingScreen(state, viewModel)
                Destination.Soccer -> SoccerScreen(state, viewModel)
                Destination.Progress -> ProgressScreen(state, viewModel)
                Destination.Schedule -> ScheduleScreen(container)
            }
        }
    }
}

@Composable
private fun TodayScreen(state: AppUiState, viewModel: AppViewModel) {
    val day = DayOfWeek.from(LocalDate.now())
    val calories = PerformancePlan.calories(day, state.settings.calorieOffset)
    ScreenColumn {
        SectionCard("Current → target") {
            Text("${"%.1f".format(state.currentWeightKg)} kg → ${state.settings.targetWeightKg.toInt()} kg")
            Text("Planned goal: ${state.goalDate}")
        }
        SectionCard("Today") {
            Text("$calories kcal • ${state.settings.proteinTargetGrams} g protein")
            Text(todayTraining(day))
        }
        state.trend?.let { trend ->
            SectionCard("Coach check") {
                Text("Latest trend: ${"%.2f".format(trend.weeklyLossKg)} kg/week")
                when (trend.calorieAdjustment) {
                    0 -> Text("Stay the course.")
                    else -> {
                        Text("Suggested adjustment: ${trend.calorieAdjustment} kcal/day")
                        Button(onClick = { viewModel.applyCalorieAdjustment(trend.calorieAdjustment) }) { Text("Apply") }
                    }
                }
            }
        }
        QuickWeight(viewModel)
        SectionCard("Operating rule") { Text("Weight and waist trend down while speed, strength, availability, and match quality trend up.") }
    }
}

@Composable
private fun QuickWeight(viewModel: AppViewModel) {
    var text by remember { mutableStateOf("") }
    SectionCard("Quick weigh-in") {
        DecimalField("kg", text) { text = it }
        Button(onClick = { text.toDoubleOrNull()?.let(viewModel::saveWeight); text = "" }) { Text("Save") }
    }
}

@Composable
private fun NutritionScreen(state: AppUiState, viewModel: AppViewModel) {
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    ScreenColumn {
        SectionCard("Targets") {
            Text("${PerformancePlan.calories(DayOfWeek.from(LocalDate.now()), state.settings.calorieOffset)} kcal")
            Text("${state.settings.proteinTargetGrams} g protein")
        }
        SectionCard("Log today") {
            IntegerField("Calories", calories) { calories = it }
            IntegerField("Protein g", protein) { protein = it }
            IntegerField("Carbs g", carbs) { carbs = it }
            IntegerField("Fat g", fat) { fat = it }
            Button(onClick = {
                viewModel.saveNutrition(
                    calories.toIntOrNull() ?: 0,
                    protein.toIntOrNull() ?: 0,
                    carbs.toIntOrNull() ?: 0,
                    fat.toIntOrNull() ?: 0,
                    null,
                    3,
                )
            }) { Text("Save nutrition") }
        }
        SectionCard("Assembly meals") {
            Text("Greek yogurt + whey + berries + measured granola")
            Text("Precooked chicken + microwave rice + bagged salad")
            Text("High-protein miso ramen + chicken/shrimp/tofu + vegetables")
            Text("Cottage cheese + fruit")
        }
    }
}

@Composable
private fun TrainingScreen(state: AppUiState, viewModel: AppViewModel) {
    val day = DayOfWeek.from(LocalDate.now())
    val ids = when (day) {
        DayOfWeek.MONDAY -> ExerciseCatalog.monday
        DayOfWeek.WEDNESDAY -> ExerciseCatalog.wednesday
        else -> ExerciseCatalog.wednesday
    }
    var selected by remember { mutableStateOf<Exercise?>(null) }
    var logging by remember { mutableStateOf<Exercise?>(null) }

    ScreenColumn {
        SectionCard("Today") { Text(todayTraining(day)) }
        SectionCard(if (day == DayOfWeek.MONDAY) "Monday session" else "Wednesday session") {
            ids.mapNotNull(ExerciseCatalog::byId).forEach { exercise ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(exercise.name)
                        Text(exercise.prescription)
                    }
                    OutlinedButton(onClick = { selected = exercise }) { Text("Form") }
                    OutlinedButton(onClick = { logging = exercise }) { Text("Log") }
                }
            }
        }
        if (day == DayOfWeek.FRIDAY) {
            SectionCard("Recovery") { Text("30–60 min easy walking pad + ~10 min mobility. No hard HIIT, sprinting, or heavy vest walking.") }
        }
        PerformanceEntry(viewModel)
        state.workouts.firstOrNull()?.let { latest ->
            SectionCard("Latest strength entry") { Text("${latest.exerciseId}: ${latest.loadLb ?: "—"} lb • ${latest.sets} sets • ${latest.reps ?: "—"} reps") }
        }
    }

    selected?.let { ExerciseReferenceDialog(it) { selected = null } }
    logging?.let { exercise -> WorkoutLogDialog(exercise, viewModel) { logging = null } }
}

@Composable
private fun WorkoutLogDialog(exercise: Exercise, viewModel: AppViewModel, onDismiss: () -> Unit) {
    var load by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("") }
    var rpe by remember { mutableStateOf("8") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exercise.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DecimalField("Load lb", load) { load = it }
                IntegerField("Sets", sets) { sets = it }
                IntegerField("Reps", reps) { reps = it }
                IntegerField("RPE", rpe) { rpe = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.saveWorkout(exercise.id, load.toDoubleOrNull(), sets.toIntOrNull() ?: 0, reps.toIntOrNull(), rpe.toIntOrNull())
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PerformanceEntry(viewModel: AppViewModel) {
    var s10 by remember { mutableStateOf("") }
    var s20 by remember { mutableStateOf("") }
    var s30 by remember { mutableStateOf("") }
    var jump by remember { mutableStateOf("") }
    SectionCard("Performance test • every 4–6 weeks") {
        DecimalField("10 m sprint (s)", s10) { s10 = it }
        DecimalField("20 m sprint (s)", s20) { s20 = it }
        DecimalField("30 m sprint (s)", s30) { s30 = it }
        DecimalField("Broad jump (cm)", jump) { jump = it }
        Button(onClick = { viewModel.savePerformance(s10.toDoubleOrNull(), s20.toDoubleOrNull(), s30.toDoubleOrNull(), jump.toDoubleOrNull()) }) { Text("Save test") }
    }
}

@Composable
private fun SoccerScreen(state: AppUiState, viewModel: AppViewModel) {
    var matchDialog by remember { mutableStateOf(false) }
    ScreenColumn {
        SectionCard("Thursday winger laboratory • 75–90 min") {
            Text("10 min dynamic warm-up + ball touches")
            Text("3×10 m + 3×20 m + 2×30 m • full recovery")
            Text("10 min left-foot passing + receiving")
            Text("15 min first touch + scanning")
            Text("10–15 min 1v1 / change of pace")
            Text("15–20 min finishing + left-foot cutbacks/crosses")
        }
        SectionCard("Left-wing identity") {
            Text("Shown outside → cut inside onto the right.")
            Text("Shown inside → attack the line and use the left.")
            Text("Scan before receipt; first touch should set up the next action.")
        }
        SectionCard("Three dribbling weapons") {
            Text("Body feint → outside burst")
            Text("Show outside → cut inside")
            Text("Stop/start → explode")
        }
        Button(onClick = { matchDialog = true }) { Text("Log a match") }
        state.matches.take(3).forEach { match ->
            SectionCard(match.date) { Text("${match.rating ?: "—"}/10 • ${match.goals}G ${match.assists}A • scan ${match.scanning ?: "—"}/10") }
        }
    }
    if (matchDialog) MatchDialog(viewModel) { matchDialog = false }
}

@Composable
private fun MatchDialog(viewModel: AppViewModel, onDismiss: () -> Unit) {
    var minutes by remember { mutableStateOf("90") }
    var rating by remember { mutableStateOf("") }
    var energy by remember { mutableStateOf("") }
    var speed by remember { mutableStateOf("") }
    var touch by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf("") }
    var weakFoot by remember { mutableStateOf("") }
    var dribbling by remember { mutableStateOf("") }
    var goals by remember { mutableStateOf("0") }
    var assists by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post-match review") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IntegerField("Minutes", minutes) { minutes = it }
                IntegerField("Overall /10", rating) { rating = it }
                IntegerField("Energy /10", energy) { energy = it }
                IntegerField("Speed /10", speed) { speed = it }
                IntegerField("First touch /10", touch) { touch = it }
                IntegerField("Scanning /10", scanning) { scanning = it }
                IntegerField("Weak foot /10", weakFoot) { weakFoot = it }
                IntegerField("Dribbling /10", dribbling) { dribbling = it }
                IntegerField("Goals", goals) { goals = it }
                IntegerField("Assists", assists) { assists = it }
                TextField(notes, { notes = it }, label = { Text("Notes / one improvement") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.saveMatch(
                    MatchEntity(
                        date = LocalDate.now().toString(),
                        minutes = minutes.toIntOrNull() ?: 0,
                        rating = rating.toIntOrNull(),
                        energy = energy.toIntOrNull(),
                        speed = speed.toIntOrNull(),
                        firstTouch = touch.toIntOrNull(),
                        scanning = scanning.toIntOrNull(),
                        weakFoot = weakFoot.toIntOrNull(),
                        dribbling = dribbling.toIntOrNull(),
                        goals = goals.toIntOrNull() ?: 0,
                        assists = assists.toIntOrNull() ?: 0,
                        notes = notes,
                    ),
                )
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ProgressScreen(state: AppUiState, viewModel: AppViewModel) {
    ScreenColumn {
        SectionCard("Weight trajectory") {
            Text("${"%.1f".format(state.currentWeightKg)} kg • ${"%.1f".format(state.currentWeightKg - state.settings.targetWeightKg)} kg remaining")
            WeightChart(state.weights, state.settings.targetWeightKg)
        }
        SectionCard("Performance") {
            val latest = state.tests.lastOrNull()
            Text("10 m: ${latest?.sprint10 ?: "—"} s")
            Text("20 m: ${latest?.sprint20 ?: "—"} s")
            Text("30 m: ${latest?.sprint30 ?: "—"} s")
            Text("Broad jump: ${latest?.broadJumpCm ?: "—"} cm")
        }
        state.trend?.let { trend ->
            SectionCard("Fat-loss trend") {
                Text("${"%.2f".format(trend.weeklyLossKg)} kg/week")
                Text(if (trend.calorieAdjustment == 0) "On target" else "Suggested calorie change: ${trend.calorieAdjustment} kcal/day")
                if (trend.calorieAdjustment != 0) Button(onClick = { viewModel.applyCalorieAdjustment(trend.calorieAdjustment) }) { Text("Apply") }
            }
        }
        SectionCard("Matches") {
            Text("${state.matches.size} matches logged")
            state.matches.take(5).forEach { Text("${it.date}: ${it.rating ?: "—"}/10 • ${it.goals}G ${it.assists}A") }
        }
    }
}

@Composable
private fun ScheduleScreen(container: app.footyos.data.AppContainer) {
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    ScreenColumn {
        SectionCard("Weekly anchors") {
            Text("Tue • 8–10 pm • 11v11")
            Text("Thu • 8 pm • winger development")
            Text("Sat • 7–9 am • 11v11")
            Text("Sun • 7–9 am • 11v11")
        }
        SectionCard("Notifications") {
            Button(onClick = {
                if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }) { Text("Enable reminders") }
            OutlinedButton(onClick = {
                val start = nextOccurrence(DayOfWeek.THURSDAY, 20, 0)
                container.reminderScheduler.schedule(
                    ReminderRequest(
                        id = 400,
                        title = "Winger development",
                        body = "Speed → left foot → first touch → 1v1 → finishing",
                        triggerAtMillis = start.minusMinutes(30).toInstant().toEpochMilli(),
                        deepLink = "footyos://open/soccer",
                    ),
                )
            }) { Text("Schedule next Thursday reminder") }
        }
        SectionCard("Calendar") {
            Button(onClick = {
                val draft = CalendarEventDraft(
                    title = "FootyOS • Winger development",
                    start = nextOccurrence(DayOfWeek.THURSDAY, 20, 0),
                    durationMinutes = 90,
                    deepLink = "footyos://open/soccer",
                )
                context.startActivity(container.calendarRepository.insertIntent(draft))
            }) { Text("Add Thursday to calendar") }
            Text("Calendar events include a FootyOS app URI. Native FootyOS reminders always open the matching screen directly.")
        }
    }
}

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title)
            content()
        }
    }
}

@Composable
private fun IntegerField(label: String, value: String, onValueChange: (String) -> Unit) {
    TextField(value, { onValueChange(it.filter(Char::isDigit)) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun DecimalField(label: String, value: String, onValueChange: (String) -> Unit) {
    TextField(value, { onValueChange(it.filter { c -> c.isDigit() || c == '.' }) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}

private fun todayTraining(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Recovery + upper body + prehab"
    DayOfWeek.TUESDAY -> "11v11 match • 8–10 pm"
    DayOfWeek.WEDNESDAY -> "Main strength + power"
    DayOfWeek.THURSDAY -> "Left-winger technical + speed • 8 pm"
    DayOfWeek.FRIDAY -> "Walking + mobility + recovery"
    DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> "11v11 match • 7–9 am"
}

private fun nextOccurrence(day: DayOfWeek, hour: Int, minute: Int): ZonedDateTime {
    var date = ZonedDateTime.now().withHour(hour).withMinute(minute).withSecond(0).withNano(0)
    while (date.dayOfWeek != day || !date.isAfter(ZonedDateTime.now())) date = date.plusDays(1)
    return date
}

private fun destinationFromUri(uri: Uri?): Destination? = when (uri?.pathSegments?.firstOrNull()?.lowercase()) {
    "today" -> Destination.Today
    "nutrition" -> Destination.Nutrition
    "training" -> Destination.Training
    "soccer" -> Destination.Soccer
    "progress" -> Destination.Progress
    "schedule" -> Destination.Schedule
    else -> null
}

private fun iconFor(destination: Destination) = when (destination) {
    Destination.Today -> Icons.Default.Home
    Destination.Nutrition -> Icons.Default.Restaurant
    Destination.Training -> Icons.Default.DirectionsRun
    Destination.Soccer -> Icons.Default.SportsSoccer
    Destination.Progress -> Icons.Default.Insights
    Destination.Schedule -> Icons.Default.CalendarMonth
}
