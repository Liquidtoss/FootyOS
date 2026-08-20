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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import app.footyos.domain.PerformancePlan
import java.time.DayOfWeek
import java.time.LocalDate

private enum class Destination(val label: String) {
    Today("Today"),
    Nutrition("Nutrition"),
    Training("Training"),
    Soccer("Soccer"),
    Progress("Progress"),
    Schedule("Schedule"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootyOsApp(initialUri: Uri?) {
    val context = LocalContext.current
    val container = (context.applicationContext as FootyOsApplication).container
    val viewModel: AppViewModel = viewModel(
        factory = AppViewModel.Factory(container.repository, container.settingsRepository),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var destination by rememberSaveable {
        mutableStateOf(destinationFromUri(initialUri) ?: Destination.Today)
    }

    LaunchedEffect(initialUri) {
        destinationFromUri(initialUri)?.let { destination = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("FootyOS")
                        Text(destination.label)
                    }
                },
            )
        },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (destination) {
                Destination.Today -> TodayScreen(state, viewModel)
                Destination.Nutrition -> NutritionScreen(state, viewModel)
                Destination.Training -> TrainingScreen()
                Destination.Soccer -> SoccerScreen()
                Destination.Progress -> ProgressScreen(state)
                Destination.Schedule -> ScheduleScreen()
            }
        }
    }
}

@Composable
private fun TodayScreen(state: AppUiState, viewModel: AppViewModel) {
    val day = DayOfWeek.from(LocalDate.now())
    val calories = PerformancePlan.calories(day, state.settings.calorieOffset)

    ScreenColumn {
        SectionCard("Current weight") {
            Text("${"%.1f".format(state.currentWeightKg)} kg → ${state.settings.targetWeightKg.toInt()} kg")
            Text("Planned goal: ${state.goalDate}")
        }
        SectionCard("Today") {
            Text("$calories kcal")
            Text("${state.settings.proteinTargetGrams} g protein")
            Text(todayTraining(day))
        }
        QuickWeight(viewModel)
        SectionCard("Operating rule") {
            Text("Weight and waist trend down while speed, strength and match quality trend up.")
        }
    }
}

@Composable
private fun QuickWeight(viewModel: AppViewModel) {
    var text by remember { mutableStateOf("") }
    SectionCard("Quick weigh-in") {
        TextField(
            value = text,
            onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("kg") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                text.toDoubleOrNull()?.let(viewModel::saveWeight)
                text = ""
            },
        ) {
            Text("Save")
        }
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
            NumberField("Calories", calories) { calories = it }
            NumberField("Protein g", protein) { protein = it }
            NumberField("Carbs g", carbs) { carbs = it }
            NumberField("Fat g", fat) { fat = it }
            Button(
                onClick = {
                    viewModel.saveNutrition(
                        calories = calories.toIntOrNull() ?: 0,
                        protein = protein.toIntOrNull() ?: 0,
                        carbs = carbs.toIntOrNull() ?: 0,
                        fat = fat.toIntOrNull() ?: 0,
                        waistCm = null,
                        readiness = 3,
                    )
                },
            ) {
                Text("Save nutrition")
            }
        }
        SectionCard("Easy meal system") {
            Text("Greek yogurt + whey + berries + measured granola")
            Text("Precooked chicken + microwave rice + bagged salad")
            Text("High-protein miso ramen with chicken, shrimp, tofu, or eggs")
            Text("Cottage cheese + fruit")
        }
    }
}

@Composable
private fun TrainingScreen() {
    val day = DayOfWeek.from(LocalDate.now())
    ScreenColumn {
        SectionCard("Today") {
            Text(todayTraining(day))
        }
        SectionCard("Monday — recovery + upper") {
            Text("Floor press or push-ups • 3×8–12")
            Text("1-arm kettlebell row • 3×8–12/side")
            Text("Overhead press • 3×6–10/side")
            Text("Suitcase carry • 3×40 sec/side")
            Text("Copenhagen plank + hamstring slider + calves")
        }
        SectionCard("Wednesday — main strength + power") {
            Text("65-lb kettlebell swing • 4×8")
            Text("Goblet squat • 3×6–10")
            Text("Bulgarian split squat • 3×6–8/leg")
            Text("Single-leg RDL • 3×6–8/leg")
            Text("Floor press + row + calf + tibialis")
        }
        SectionCard("Friday — recover") {
            Text("30–60 min easy walking pad + 10 min mobility.")
            Text("No hard HIIT, sprint session, or heavy vest walking.")
        }
    }
}

@Composable
private fun SoccerScreen() {
    ScreenColumn {
        SectionCard("Thursday winger laboratory • 75–90 min") {
            Text("10 min dynamic warm-up + ball touches")
            Text("3×10 m + 3×20 m + 2×30 m, full recovery")
            Text("10 min left-foot passing + receiving")
            Text("15 min first touch + scanning")
            Text("10–15 min 1v1 / change of pace")
            Text("15–20 min finishing + left-foot cutbacks/crosses")
        }
        SectionCard("Left-wing identity") {
            Text("Shown outside: cut inside onto the right.")
            Text("Shown inside: attack the line and use the left.")
            Text("Scan before receipt; first touch should set up the next action.")
        }
        SectionCard("Three dribbling weapons") {
            Text("Body feint → outside burst")
            Text("Show outside → cut inside")
            Text("Stop/start → explode")
        }
    }
}

@Composable
private fun ProgressScreen(state: AppUiState) {
    ScreenColumn {
        SectionCard("Weight") {
            Text("${"%.1f".format(state.currentWeightKg)} kg")
            Text("${"%.1f".format(state.currentWeightKg - state.settings.targetWeightKg)} kg remaining")
        }
        SectionCard("Performance tests") {
            val latest = state.tests.lastOrNull()
            Text("10 m: ${latest?.sprint10 ?: "—"}")
            Text("20 m: ${latest?.sprint20 ?: "—"}")
            Text("30 m: ${latest?.sprint30 ?: "—"}")
            Text("Broad jump: ${latest?.broadJumpCm ?: "—"} cm")
        }
        SectionCard("Match log") {
            Text("${state.matches.size} matches logged")
            state.matches.take(3).forEach {
                Text("${it.date}: ${it.rating ?: "—"}/10 • ${it.goals}G ${it.assists}A")
            }
        }
    }
}

@Composable
private fun ScheduleScreen() {
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}

    ScreenColumn {
        SectionCard("Weekly anchors") {
            Text("Tue • 8–10 pm • 11v11")
            Text("Thu • 8 pm • winger development")
            Text("Sat • 7–9 am • 11v11")
            Text("Sun • 7–9 am • 11v11")
        }
        SectionCard("Notifications") {
            Button(
                onClick = {
                    if (
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            ) {
                Text("Enable reminders")
            }
            Text("FootyOS reminders deep-link directly into the app.")
        }
        SectionCard("Calendar integration") {
            Text("Calendar events can include FootyOS deep links so the event points back to the relevant app destination.")
        }
    }
}

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title)
            content()
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit)) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun todayTraining(day: DayOfWeek): String =
    when (day) {
        DayOfWeek.MONDAY -> "Recovery + upper body + prehab"
        DayOfWeek.TUESDAY -> "11v11 match • 8–10 pm"
        DayOfWeek.WEDNESDAY -> "Main strength + power"
        DayOfWeek.THURSDAY -> "Left-winger technical + speed • 8 pm"
        DayOfWeek.FRIDAY -> "Walking + mobility + recovery"
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> "11v11 match • 7–9 am"
    }

private fun destinationFromUri(uri: Uri?): Destination? =
    when (uri?.pathSegments?.firstOrNull()?.lowercase()) {
        "today" -> Destination.Today
        "nutrition" -> Destination.Nutrition
        "training" -> Destination.Training
        "soccer" -> Destination.Soccer
        "progress" -> Destination.Progress
        "schedule" -> Destination.Schedule
        else -> null
    }

private fun iconFor(destination: Destination) =
    when (destination) {
        Destination.Today -> Icons.Default.Home
        Destination.Nutrition -> Icons.Default.Restaurant
        Destination.Training -> Icons.Default.DirectionsRun
        Destination.Soccer -> Icons.Default.SportsSoccer
        Destination.Progress -> Icons.Default.Insights
        Destination.Schedule -> Icons.Default.CalendarMonth
    }
