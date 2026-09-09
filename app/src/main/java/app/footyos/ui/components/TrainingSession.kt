package app.footyos.ui.components

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.text.input.KeyboardType
import app.footyos.data.local.WorkoutEntryEntity
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
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
fun TrainingSession(
    exercises: List<Exercise>,
    completed: Set<String>,
    history: List<WorkoutEntryEntity>,
    onSave: suspend (WorkoutEntryEntity) -> Unit,
    onSessionActive: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("training", 0) }
    var guided by rememberSaveable { mutableStateOf(preferences.getBoolean("guided", true)) }
    var selectedId by rememberSaveable(exercises.map { it.id }) { mutableStateOf(exercises.first().id) }
    val exercise = exercises.firstOrNull { it.id == selectedId } ?: exercises.first()
    val exerciseAnchor = remember { BringIntoViewRequester() }
    LaunchedEffect(exercise.id) { exerciseAnchor.bringIntoView() }
    val index = exercises.indexOf(exercise)
    val next = exercises.getOrNull(index + 1)
    var menu by remember { mutableStateOf(false) }
    var reference by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(false) }
    var pendingId by remember { mutableStateOf<String?>(null) }
    var logging by remember { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf("") }
    var records by remember { mutableStateOf(false) }
    var startNextId by rememberSaveable { mutableStateOf("") }
    var lastSavedId by rememberSaveable { mutableStateOf("") }
    val savedIds = completed + listOf(lastSavedId).filter { it.isNotBlank() }
    val count = exercises.count { it.id in savedIds }
    fun select(id: String) {
        if (id != exercise.id) {
            if (active) pendingId = id else { startNextId = ""; selectedId = id }
        }
        menu = false
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!active) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Guided", "Simple").forEachIndexed { mode, label ->
                    SegmentedButton(
                        selected = guided == (mode == 0),
                        onClick = { startNextId = ""; guided = mode == 0; preferences.edit().putBoolean("guided", guided).apply() },
                        shape = SegmentedButtonDefaults.itemShape(mode, 2),
                    ) { Text(label) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$count / ${exercises.size} logged today", color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp))
            TextButton(onClick = { menu = true }) { Text("Session list") }
        }
        LinearProgressIndicator(progress = { count.toFloat() / exercises.size }, modifier = Modifier.fillMaxWidth())
        if (result.isNotBlank()) Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary)
                Text(result, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (count == exercises.size) Text("Session complete • every exercise logged. Great work!", color = MaterialTheme.colorScheme.primary)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f).bringIntoViewRequester(exerciseAnchor)) {
                        Text("EXERCISE ${index + 1} OF ${exercises.size}${if (exercise.id in savedIds) "  •  LOGGED" else ""}",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                        Text(exercise.prescription, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { reference = true }) { Icon(Icons.Default.PlayArrow, "View form") }
                }
                if (!active) {
                    val previous = TrainingRecords.history(exercise.id, history).firstOrNull()
                    Text(previous?.let { "Last time • ${TrainingRecords.summary(it)}" } ?: "First time? This session sets your baseline.",
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { records = true }, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Default.EmojiEvents, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Personal bests & history")
                    }
                }
                if (guided) key(exercise.id) {
                    GuidedPlayer(exercise, autoStart = startNextId == exercise.id, overlayOpen = menu || reference || pendingId != null, onActive = { active = it; onSessionActive(it) }, onLog = { logging = true })
                } else {
                    Button(onClick = { logging = true }, modifier = Modifier.fillMaxWidth()) { Text("Log sets") }
                }
                HorizontalDivider()
                Text(next?.let { "UP NEXT • ${it.name}" } ?: "FINAL EXERCISE • finish strong", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (index > 0) OutlinedButton(onClick = { select(exercises[index - 1].id) }) { Text("Previous") }
                    if (next != null) FilledTonalButton(onClick = { select(next.id) }, modifier = Modifier.weight(1f)) {
                        Text("Next exercise")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                    } else TextButton(onClick = { menu = true }) { Text("Review session") }
                }
                if (next != null && exercise.id !in savedIds) Text("Next skips this exercise without logging it.", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    if (menu) AlertDialog(
        onDismissRequest = { menu = false }, title = { Text("Your session") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            exercises.forEachIndexed { i, item ->
                TextButton(onClick = { select(item.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("${i + 1}. ${item.name}  ${if (item.id in savedIds) "✓" else if (item.id == exercise.id) "• Current" else ""}", modifier = Modifier.fillMaxWidth())
                }
            }
        } }, confirmButton = { TextButton(onClick = { menu = false }) { Text("Close") } },
    )
    pendingId?.let { id -> AlertDialog(
        onDismissRequest = { pendingId = null }, title = { Text("Move to another exercise?") },
        text = { Text("Your current timer will end. Unlogged sets won't be saved.") },
        confirmButton = { TextButton(onClick = { startNextId = ""; selectedId = id; pendingId = null }) { Text("Move on") } },
        dismissButton = { TextButton(onClick = { pendingId = null }) { Text("Keep training") } },
    ) }
    if (reference) ExerciseReferenceDialog(exercise) { reference = false }
    if (records) TrainingHistoryDialog(exercise, history) { records = false }
    if (logging) TrainingLogDialog(exercise, history, next != null, onSave, onDismiss = { logging = false }) { _, achievements ->
        lastSavedId = exercise.id
        result = "${exercise.name} saved" + if (achievements.isEmpty()) " • session logged ✓" else "\n${achievements.joinToString("\n")}"
        logging = false
        if (next != null) { startNextId = if (guided) next.id else ""; selectedId = next.id }
    }
}

@Composable
private fun TrainingHistoryDialog(exercise: Exercise, entries: List<WorkoutEntryEntity>, onDismiss: () -> Unit) {
    val history = TrainingRecords.history(exercise.id, entries)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Personal bests") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(exercise.name, style = MaterialTheme.typography.titleMedium)
            if (history.isEmpty()) Text("Log your first session to start building your records.")
            else {
                history.mapNotNull { it.loadLb }.maxOrNull()?.let { Text("Heaviest • ${TrainingRecords.number(it)} lb", color = MaterialTheme.colorScheme.primary) }
                history.mapNotNull { it.reps }.maxOrNull()?.let { Text("Most reps • $it / set") }
                Text("Most sets • ${history.maxOf { it.sets }}")
                Text("Rep PB celebrations compare the same load. These records describe logged sets, not estimated max strength.", style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text("Recent sessions", style = MaterialTheme.typography.titleSmall)
                history.take(10).forEach { Text("${it.date}\n${TrainingRecords.summary(it)}", style = MaterialTheme.typography.bodySmall) }
            }
        } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable
private fun TrainingLogDialog(
    exercise: Exercise, history: List<WorkoutEntryEntity>, hasNext: Boolean,
    onSave: suspend (WorkoutEntryEntity) -> Unit, onDismiss: () -> Unit,
    onSaved: (WorkoutEntryEntity, List<String>) -> Unit,
) {
    val previous = remember { TrainingRecords.history(exercise.id, history) }
    var load by rememberSaveable { mutableStateOf(previous.firstOrNull()?.loadLb?.let(TrainingRecords::number) ?: "") }
    var sets by rememberSaveable { mutableStateOf(exercise.drillPlan().sets.toString()) }
    var reps by rememberSaveable { mutableStateOf("") }
    var rpe by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val timed = exercise.drillPlan().seconds != null
    val valid = sets.toIntOrNull() in 1..100 && (timed || reps.toIntOrNull() in 1..1000) &&
        (load.isBlank() || load.toDoubleOrNull()?.let { it.isFinite() && it in 0.0..2000.0 } == true) &&
        (rpe.isBlank() || rpe.toIntOrNull() in 1..10)
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text("Log ${exercise.name}") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            previous.firstOrNull()?.let { Text("Last time • ${TrainingRecords.summary(it)}", color = MaterialTheme.colorScheme.primary) }
            Text("Enter what you actually completed. Reps are per set${if (exercise.drillPlan().sides == 2) " on each side" else ""}.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(load, { load = it }, label = { Text("Load lb (optional)") }, enabled = !saving, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(sets, { sets = it }, label = { Text("Sets (1–100)") }, enabled = !saving, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            if (!timed) OutlinedTextField(reps, { reps = it }, label = { Text("Reps per set (1–1000)") }, enabled = !saving, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(rpe, { rpe = it }, label = { Text("Effort / RPE 1–10 (optional)") }, enabled = !saving, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            if (timed) Text("Timed exercise: saves completed sets, not duration records.", style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(enabled = valid && !saving, onClick = {
            saving = true
            val entry = WorkoutEntryEntity(date = LocalDate.now().toString(), exerciseId = exercise.id,
                loadLb = load.toDoubleOrNull(), sets = sets.toInt(), reps = if (timed) null else reps.toInt(), rpe = rpe.toIntOrNull())
            scope.launch {
                try { onSave(entry); onSaved(entry, TrainingRecords.achievements(entry, previous)) }
                catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { error = "Couldn't save. Your entry is still here. Try again." }
                finally { saving = false }
            }
        }) { Text(if (saving) "Saving…" else if (hasNext) "Save & next" else "Save & finish") } },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun GuidedPlayer(exercise: Exercise, autoStart: Boolean, overlayOpen: Boolean, onActive: (Boolean) -> Unit, onLog: () -> Unit) {
    val plan = exercise.drillPlan()
    var phase by rememberSaveable { mutableStateOf(if (autoStart) DrillPhase.Prepare.name else DrillPhase.Ready.name) }
    var round by rememberSaveable { mutableIntStateOf(0) }
    var remaining by rememberSaveable { mutableIntStateOf(if (autoStart) 10 else 0) }
    var paused by rememberSaveable { mutableStateOf(!autoStart) }
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
    LaunchedEffect(overlayOpen) { if (overlayOpen) { paused = true; speech?.stop() } }
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
        DrillPhase.Prepare -> "Get ready for ${exercise.name}. ${exercise.cues.first()}"
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
    if (running) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Set ${round / plan.sides + 1}/${plan.sets}  $side", color = MaterialTheme.colorScheme.primary)
                Text(if (paused) "Paused" else when (drill.phase) { DrillPhase.Prepare -> "Get ready"; DrillPhase.Rest -> "Rest"; else -> "Your turn" }, style = MaterialTheme.typography.labelLarge)
                Text(if (drill.phase == DrillPhase.Work && plan.seconds == null) exercise.prescription.substringAfter("× ") else "%02d:%02d".format(remaining / 60, remaining % 60), style = MaterialTheme.typography.headlineLarge)
            }
            OutlinedButton(onClick = { paused = !paused }) { Text(if (paused) "Resume" else "Pause") }
        }
        LinearProgressIndicator(progress = { round.toFloat() / plan.rounds }, modifier = Modifier.fillMaxWidth())
        if (drill.phase == DrillPhase.Work && plan.seconds == null) Button(enabled = !paused, onClick = { update(drill.finishRound(plan)) }, modifier = Modifier.fillMaxWidth()) { Text("Reps done") }
        if (drill.phase == DrillPhase.Rest) FilledTonalButton(enabled = !paused, onClick = { update(drill.copy(phase = DrillPhase.Prepare, remaining = 3)) }, modifier = Modifier.fillMaxWidth()) { Text("Ready now") }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = { audio = !audio; prefs.edit().putBoolean("audio", audio).apply() }) { Text(if (audio) "Coach audio on" else "Coach audio off") }
        if (running) TextButton(onClick = { paused = true; update(GuidedDrill()) }) { Text("End exercise") }
    }
    if (audio && !voiceReady) Text("Voice unavailable on this device. Follow the on-screen cues.", style = MaterialTheme.typography.bodySmall)
    ExerciseLoop(exercise.movement, playing = !paused && drill.phase == DrillPhase.Work)
    Text(cue, style = MaterialTheme.typography.bodyMedium)
    if (drill.phase == DrillPhase.Complete) {
        Text("${plan.sets} sets complete", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Button(onClick = onLog, modifier = Modifier.fillMaxWidth()) { Text("Review & log sets") }
        TextButton(onClick = { paused = true; update(GuidedDrill()) }) { Text("Do another round") }
    } else if (!running) {
        Text("10 sec setup • ${plan.seconds?.let { "$it sec per side" } ?: "Self-paced reps"} • 60 sec between sets", style = MaterialTheme.typography.bodySmall)
    }
}
