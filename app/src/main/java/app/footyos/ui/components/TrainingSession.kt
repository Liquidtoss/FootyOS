package app.footyos.ui.components

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.footyos.domain.*
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun TrainingSession(exercises: List<Exercise>, completed: Set<String>, onLog: (Exercise, Int) -> Unit, onSessionActive: (Boolean) -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("training", 0) }
    var guided by rememberSaveable { mutableStateOf(preferences.getBoolean("guided", true)) }
    var selectedId by rememberSaveable(exercises.map { it.id }) { mutableStateOf(exercises.first().id) }
    val exercise = exercises.firstOrNull { it.id == selectedId } ?: exercises.first()
    var menu by remember { mutableStateOf(false) }
    var reference by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(guided, onClick = { guided = true; preferences.edit().putBoolean("guided", true).apply() }, enabled = !active, label = { Text("Guided") })
        FilterChip(!guided, onClick = { guided = false; preferences.edit().putBoolean("guided", false).apply() }, enabled = !active, label = { Text("Simple · log only") })
    }
    Text(if (guided) "Press start. Follow the movement. Build your momentum." else "Your routine, at your pace. Select an exercise and log it.", style = MaterialTheme.typography.bodyMedium)
    Text("${exercises.count { it.id in completed }} / ${exercises.size} exercises logged today", color = MaterialTheme.colorScheme.primary)
    LinearProgressIndicator(progress = { exercises.count { it.id in completed }.toFloat() / exercises.size }, modifier = Modifier.fillMaxWidth())
    Box {
        OutlinedButton(onClick = { menu = true }, enabled = !active, modifier = Modifier.fillMaxWidth()) {
            Text("${exercises.indexOf(exercise) + 1}. ${exercise.name}  ▾")
        }
        DropdownMenu(menu, onDismissRequest = { menu = false }, modifier = Modifier.heightIn(max = 340.dp)) {
            exercises.forEach { item ->
                DropdownMenuItem(text = { Text("${if (item.id in completed) "✓ " else ""}${item.name}") }, onClick = { selectedId = item.id; menu = false })
            }
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                    Text(exercise.prescription, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { reference = true }) { Icon(Icons.Default.PlayArrow, "View form") }
            }
            if (guided) key(exercise.id) {
                GuidedPlayer(exercise, onActive = { active = it; onSessionActive(it) }, onLog = { onLog(exercise, exercise.drillPlan().sets) })
            } else {
                Text(exercise.cues.first(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { onLog(exercise, exercise.drillPlan().sets) }, modifier = Modifier.fillMaxWidth()) { Text("Log sets") }
            }
        }
    }
    if (reference) ExerciseReferenceDialog(exercise) { reference = false }
}

@Composable
private fun GuidedPlayer(exercise: Exercise, onActive: (Boolean) -> Unit, onLog: () -> Unit) {
    val plan = exercise.drillPlan()
    var phase by rememberSaveable { mutableStateOf(DrillPhase.Ready.name) }
    var round by rememberSaveable { mutableIntStateOf(0) }
    var remaining by rememberSaveable { mutableIntStateOf(0) }
    var paused by rememberSaveable { mutableStateOf(true) }
    val drill = GuidedDrill(DrillPhase.valueOf(phase), round, remaining)
    fun update(next: GuidedDrill) { phase = next.phase.name; round = next.round; remaining = next.remaining }
    val running = drill.phase !in listOf(DrillPhase.Ready, DrillPhase.Complete)
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("training", 0) }
    var audio by rememberSaveable { mutableStateOf(prefs.getBoolean("audio", true)) }
    var voiceReady by remember { mutableStateOf(false) }
    var speech by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val engine = TextToSpeech(context) { status -> voiceReady = status == TextToSpeech.SUCCESS }
        speech = engine
        onDispose { engine.stop(); engine.shutdown(); speech = null }
    }
    LaunchedEffect(voiceReady) {
        if (voiceReady) voiceReady = (speech?.setLanguage(Locale.US) ?: -1) >= 0
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) { paused = true; speech?.stop() }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val view = LocalView.current
    DisposableEffect(running, paused) {
        val previous = view.keepScreenOn
        view.keepScreenOn = running && !paused
        onDispose { view.keepScreenOn = previous }
    }
    LaunchedEffect(running) { onActive(running) }
    DisposableEffect(Unit) { onDispose { onActive(false) } }
    LaunchedEffect(phase, round, paused) {
        if (running && !paused) {
            while (true) { delay(1000); update(GuidedDrill(DrillPhase.valueOf(phase), round, remaining).tick(plan)) }
        }
    }
    val side = if (plan.sides == 2) if (round % 2 == 0) "First side" else "Other side" else ""
    val cue = when (drill.phase) {
        DrillPhase.Ready -> "Get set up, then start when you're ready."
        DrillPhase.Prepare -> "Get ready. ${exercise.cues.first()}"
        DrillPhase.Work -> "$side. ${exercise.cues.getOrElse((round / plan.sides) % exercise.cues.size) { exercise.cues.first() }}"
        DrillPhase.Rest -> if (plan.sides == 2 && round % 2 == 1) "Switch sides. Take a breath." else "Rest. Next set coming up."
        DrillPhase.Complete -> "Exercise complete. Nice work. Review and log your sets."
    }
    LaunchedEffect(cue, audio, voiceReady, paused) {
        if (audio && voiceReady && !paused) speech?.speak(cue, TextToSpeech.QUEUE_FLUSH, null, "coach") else speech?.stop()
    }
    LaunchedEffect(remaining) {
        if (audio && voiceReady && !paused && remaining in 1..3) speech?.speak(remaining.toString(), TextToSpeech.QUEUE_FLUSH, null, "countdown")
    }
    if (drill.phase == DrillPhase.Ready) {
        Button(onClick = { paused = false; update(drill.start()) }, modifier = Modifier.fillMaxWidth()) { Text("Start guided exercise") }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Coach audio", modifier = Modifier.padding(top = 12.dp))
        Switch(audio, onCheckedChange = { audio = it; prefs.edit().putBoolean("audio", it).apply() })
    }
    if (audio && !voiceReady) Text("Voice unavailable on this device. Follow the on-screen cues.", style = MaterialTheme.typography.bodySmall)
    ExerciseLoop(exercise.movement, playing = !paused && drill.phase == DrillPhase.Work)
    Text(cue, style = MaterialTheme.typography.bodyMedium)
    if (running) {
        Text("Set ${round / plan.sides + 1} of ${plan.sets}  $side", color = MaterialTheme.colorScheme.primary)
        Text(if (paused) "Paused" else when (drill.phase) { DrillPhase.Prepare -> "Get ready"; DrillPhase.Rest -> "Rest"; else -> "Your turn" }, style = MaterialTheme.typography.titleMedium)
        Text(if (drill.phase == DrillPhase.Work && plan.seconds == null) exercise.prescription.substringAfter("× ") else "%02d:%02d".format(remaining / 60, remaining % 60), style = MaterialTheme.typography.displayMedium)
        if (drill.phase == DrillPhase.Work && plan.seconds == null) Text("Move at your pace. Tap below after your reps.", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { paused = !paused }) { Text(if (paused) "Resume" else "Pause") }
            if (drill.phase == DrillPhase.Work && plan.seconds == null) Button(enabled = !paused, onClick = { update(drill.finishRound(plan)) }) { Text("Reps done") }
            if (drill.phase == DrillPhase.Rest) TextButton(enabled = !paused, onClick = { update(drill.copy(phase = DrillPhase.Prepare, remaining = 3)) }) { Text("Ready now") }
        }
        TextButton(onClick = { paused = true; update(GuidedDrill()) }) { Text("End exercise") }
    } else if (drill.phase == DrillPhase.Complete) {
        Text("${plan.sets} sets complete", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Button(onClick = onLog, modifier = Modifier.fillMaxWidth()) { Text("Review & log sets") }
        TextButton(onClick = { paused = true; update(GuidedDrill()) }) { Text("Do another round") }
    } else {
        Text("10 sec setup • ${plan.seconds?.let { "$it sec per side" } ?: "Self-paced reps"} • 60 sec between sets", style = MaterialTheme.typography.bodySmall)
    }
}
