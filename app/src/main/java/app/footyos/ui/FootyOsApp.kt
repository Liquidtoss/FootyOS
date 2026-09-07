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
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import java.time.format.DateTimeFormatter
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
import app.footyos.ui.components.MealPhotoCard
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

    val screenState = rememberSaveableStateHolder()
    Scaffold(
        topBar = { TopAppBar(
            title = { Text("FOOTYOS", style = MaterialTheme.typography.titleMedium, letterSpacing = 3.sp) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            actions = { IconButton(onClick = { destination = Destination.Schedule }) { Icon(Icons.Default.CalendarMonth, "Schedule", tint = MaterialTheme.colorScheme.primary) } },
        ) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
                Destination.entries.filter { it != Destination.Schedule }.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(iconFor(item), item.label) },
                        label = { Text(item.label, maxLines = 1, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer, selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary),
                    )
                }
            }
        },
    ) { padding ->
        Crossfade(destination, modifier = Modifier.fillMaxSize().padding(padding), animationSpec = tween(180), label = "screen") { page ->
          screenState.SaveableStateProvider(page.name) {
            when (page) {
                Destination.Today -> TodayScreen(state, viewModel) { destination = it }
                Destination.Nutrition -> NutritionScreen(state, viewModel)
                Destination.Training -> TrainingScreen(state, viewModel)
                Destination.Soccer -> SoccerScreen(state, viewModel)
                Destination.Progress -> ProgressScreen(state, viewModel)
                Destination.Schedule -> ScheduleScreen(container)
            }
          }
        }
    }
}

@Composable
private fun TodayScreen(state: AppUiState, viewModel: AppViewModel, navigate: (Destination) -> Unit) {
    val day = DayOfWeek.from(LocalDate.now())
    val calories = PerformancePlan.calories(day, state.settings.calorieOffset)
    ScreenColumn {
        PageHeading("Your daily edge", LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")))
        HeroCard("TODAY’S FOCUS", todayTraining(day), "Build strength. Move better. Show up ready.") {
            Button(onClick = { navigate(if (day in listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)) Destination.Training else Destination.Soccer) }, modifier = Modifier.fillMaxWidth()) { Text("View your session") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("$calories", "kcal target", Modifier.weight(1f))
            MetricCard("${state.settings.proteinTargetGrams} g", "protein target", Modifier.weight(1f))
        }
        SectionCard("Your trajectory") {
            Text("${"%.1f".format(state.currentWeightKg)} kg", style = MaterialTheme.typography.headlineLarge)
            Text("Target ${state.settings.targetWeightKg.toInt()} kg · Planned ${state.goalDate}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { navigate(Destination.Progress) }) { Text("See progress →") }
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
        PageHeading("Fuel your game", "NUTRITION")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("${PerformancePlan.calories(DayOfWeek.from(LocalDate.now()), state.settings.calorieOffset)}", "kcal target", Modifier.weight(1f))
            MetricCard("${state.settings.proteinTargetGrams} g", "protein target", Modifier.weight(1f))
        }
        app.footyos.ui.components.GeminiSetup()
        DailyNutrition(state, viewModel)
        MealPhotoCard(onSave = viewModel::saveMeal)
        SectionCard("Manual daily baseline") {
            Text("Use this only for nutrition not already logged as meals. Saving replaces the manual baseline.")
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
    var upperBody by rememberSaveable { mutableStateOf(day == DayOfWeek.MONDAY) }
    val ids = if (upperBody) ExerciseCatalog.monday else ExerciseCatalog.wednesday
    var selected by remember { mutableStateOf<Exercise?>(null) }
    var logging by remember { mutableStateOf<Exercise?>(null) }

    ScreenColumn {
        PageHeading("Built for the pitch", "TRAINING")
        HeroCard("YOUR PLAN", if (upperBody) "Upper body & prehab" else "Strength & power", "${ids.size} exercises · Home equipment") {}
        Text("Today · ${todayTraining(day)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = upperBody, onClick = { upperBody = true }, label = { Text("Monday · Upper") })
            FilterChip(selected = !upperBody, onClick = { upperBody = false }, label = { Text("Wednesday · Power") })
        }
        ids.mapNotNull(ExerciseCatalog::byId).forEachIndexed { index, exercise ->
            SectionCard(exercise.name) {
                Text("${(index + 1).toString().padStart(2, '0')}  /  ${ids.size}    ·    ${exercise.prescription}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { selected = exercise }, modifier = Modifier.weight(1f)) { Text("View form") }
                    Button(onClick = { logging = exercise }, modifier = Modifier.weight(1f)) { Text("Log sets") }
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
    var expanded by rememberSaveable { mutableStateOf(false) }
    var s10 by remember { mutableStateOf("") }
    var s20 by remember { mutableStateOf("") }
    var s30 by remember { mutableStateOf("") }
    var jump by remember { mutableStateOf("") }
    SectionCard("Performance test • every 4–6 weeks") {
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Close test" else "Record sprint & jump results") }
        AnimatedVisibility(expanded) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DecimalField("10 m sprint (s)", s10) { s10 = it }
        DecimalField("20 m sprint (s)", s20) { s20 = it }
        DecimalField("30 m sprint (s)", s30) { s30 = it }
        DecimalField("Broad jump (cm)", jump) { jump = it }
        Button(onClick = { viewModel.savePerformance(s10.toDoubleOrNull(), s20.toDoubleOrNull(), s30.toDoubleOrNull(), jump.toDoubleOrNull()) }) { Text("Save test") }
          }
        }
    }
}

@Composable
private fun SoccerScreen(state: AppUiState, viewModel: AppViewModel) {
    var matchDialog by remember { mutableStateOf(false) }
    ScreenColumn {
        PageHeading("Own your wing", "SOCCER")
        SectionCard("Thursday winger laboratory • 75–90 min") {
            PlanRow("01", "Warm up", "10 min · Dynamic movement + ball touches")
            PlanRow("02", "Find your speed", "3×10 m + 3×20 m + 2×30 m · Full recovery")
            PlanRow("03", "Build your left foot", "10 min · Passing + receiving")
            PlanRow("04", "See the next move", "15 min · First touch + scanning")
            PlanRow("05", "Beat your player", "10–15 min · 1v1 + change of pace")
            PlanRow("06", "Make it count", "15–20 min · Finishing + cutbacks + crosses")
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
        PageHeading("Every session counts", "PROGRESS")
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
        PageHeading("Make room for better", "YOUR WEEK")
        SectionCard("Weekly anchors") {
            PlanRow("TUE", "Match night", "8–10 pm · 11v11")
            PlanRow("THU", "Winger development", "8 pm · Technique + speed")
            PlanRow("SAT", "Weekend football", "7–9 am · 11v11")
            PlanRow("SUN", "Back on the pitch", "7–9 am · 11v11")
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
            Text("Keep your next session close. Your reminder opens the session in FootyOS.")
        }
    }
}

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = content,
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun IntegerField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value, { onValueChange(it.filter(Char::isDigit)) }, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun DecimalField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value, { onValueChange(it.filter { c -> c.isDigit() || c == '.' }) }, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun PageHeading(title: String, eyebrow: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.headlineLarge)
    }
}

@Composable
private fun HeroCard(eyebrow: String, title: String, detail: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface))).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(eyebrow, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlanRow(marker: String, title: String, detail: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Text(marker, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
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

@Composable
private fun DailyNutrition(state: AppUiState, viewModel: AppViewModel) {
    val nutrition by viewModel.nutrition.collectAsStateWithLifecycle()
    val (meals, baseline) = nutrition
    val settings = state.settings
    var protein by rememberSaveable(settings.proteinTargetGrams) { mutableStateOf(settings.proteinTargetGrams.toString()) }
    var carbs by rememberSaveable(settings.carbsTargetGrams) { mutableStateOf(settings.carbsTargetGrams.toString()) }
    var fat by rememberSaveable(settings.fatTargetGrams) { mutableStateOf(settings.fatTargetGrams.toString()) }
    SectionCard("Today's progress") {
        val metrics = listOf(
            Triple("Calories", meals.sumOf { it.calories } + (baseline?.calories ?: 0), PerformancePlan.calories(LocalDate.now().dayOfWeek, settings.calorieOffset)),
            Triple("Protein g", meals.sumOf { it.proteinGrams } + (baseline?.proteinGrams ?: 0), settings.proteinTargetGrams),
            Triple("Carbs g", meals.sumOf { it.carbsGrams } + (baseline?.carbsGrams ?: 0), settings.carbsTargetGrams),
            Triple("Fat g", meals.sumOf { it.fatGrams } + (baseline?.fatGrams ?: 0), settings.fatTargetGrams),
        )
        metrics.forEach { (label, consumed, target) ->
            Text("$label: $consumed / $target")
            androidx.compose.material3.LinearProgressIndicator(
                progress = { (consumed.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(if (consumed <= target) "${target - consumed} remaining" else "${consumed - target} above goal", style = MaterialTheme.typography.bodySmall)
        }
        Text("Progress includes confirmed meals and your manual baseline. Photo estimates will remain approximate; check portions, oils and sauces.")
    }
    SectionCard("Daily macro goals") {
        Text("Adjust these starting goals to your own plan.")
        IntegerField("Protein g", protein) { protein = it }
        IntegerField("Carbs g", carbs) { carbs = it }
        IntegerField("Fat g", fat) { fat = it }
        Button(enabled = listOf(protein, carbs, fat).all { (it.toIntOrNull() ?: 0) in 1..1000 }, onClick = {
            viewModel.saveMacroGoals(protein.toInt(), carbs.toInt(), fat.toInt())
        }) { Text("Save goals") }
    }
    SectionCard("Today's meals") {
        if (meals.isEmpty()) Text("No meals logged yet.")
        meals.forEach { meal ->
            Text(meal.name, style = MaterialTheme.typography.titleMedium)
            Text("${meal.calories} kcal · P ${meal.proteinGrams} g · C ${meal.carbsGrams} g · F ${meal.fatGrams} g")
            Text("Confirmed · ${meal.source}", style = MaterialTheme.typography.bodySmall)
            meal.photoName?.let { app.footyos.ui.components.SavedMealPhoto(it) }
            TextButton(onClick = { viewModel.deleteMeal(meal.id) }) { Text("Delete meal") }
        }
    }
}
