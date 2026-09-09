package app.footyos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.footyos.domain.Exercise
import app.footyos.domain.Movement
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private data class MovementGuide(
    val start: String,
    val finish: String,
    val motion: String,
)

@Composable
fun ExerciseReferenceDialog(
    exercise: Exercise,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(exercise.name) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (exercise.id == "floor_press") FloorPressViewer()
                else ExerciseMovementGuide(exercise.movement)
                Text(exercise.prescription, color = MaterialTheme.colorScheme.primary)
                Text("Form cues", style = MaterialTheme.typography.titleSmall)
                exercise.cues.forEach { Text("• $it") }
                Text("Avoid", style = MaterialTheme.typography.titleSmall)
                exercise.mistakes.forEach { Text("• $it") }
                OutlinedButton(
                    onClick = { uriHandler.openUri(exercise.reference.url) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Watch reference • ${exercise.reference.source}")
                }
                Text(
                    if (exercise.id == "floor_press") "Rotate the 3D demonstration to inspect form. Use the reference video for full setup and technique."
                    else "The diagrams highlight the main positions. Use the reference video for full setup, tempo, and technique.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
fun ExerciseLoop(movement: Movement, playing: Boolean) {
    if (movement == Movement.Press) {
        FloorPressViewer(playing = playing)
        return
    }
    var finish by remember(movement) { mutableStateOf(false) }
    LaunchedEffect(movement, playing) {
        if (playing) while (true) { delay(1800); finish = !finish }
        else finish = false
    }
    val guide = guideFor(movement)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PositionFrame(
            label = if (finish) "FINISH POSITION" else "START POSITION",
            detail = if (finish) guide.finish else guide.start,
            movement = movement, finish = finish, modifier = Modifier.fillMaxWidth(),
        )
        Text(guide.motion, style = MaterialTheme.typography.bodySmall)
        Text("Position guide • tap ▶ for full technique video", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExerciseMovementGuide(movement: Movement) {
    val guide = guideFor(movement)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PositionFrame(
                label = "START",
                detail = guide.start,
                movement = movement,
                finish = false,
                modifier = Modifier.weight(1f),
            )
            PositionFrame(
                label = "FINISH",
                detail = guide.finish,
                movement = movement,
                finish = true,
                modifier = Modifier.weight(1f),
            )
        }
        Text("MOVE • ${guide.motion}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PositionFrame(
    label: String,
    detail: String,
    movement: Movement,
    finish: Boolean,
    modifier: Modifier = Modifier,
) {
    val figure = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    val equipment = MaterialTheme.colorScheme.secondary
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = accent)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .semantics { contentDescription = "$label position: $detail" },
            ) {
                drawMovement(movement, finish, figure, accent, equipment)
            }
            Text(detail, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun guideFor(movement: Movement): MovementGuide = when (movement) {
    Movement.Press -> MovementGuide("Upper arm rests on floor", "Wrist stacks over shoulder", "Press straight up; lower until the upper arm meets the floor.")
    Movement.Row -> MovementGuide("Arm long, spine braced", "Elbow reaches back pocket", "Pull without twisting the torso, then lower under control.")
    Movement.OverheadPress -> MovementGuide("Bell at shoulder, ribs down", "Arm vertical overhead", "Press up without leaning back; return to the rack.")
    Movement.Carry -> MovementGuide("Stand tall with bell at side", "Walk without side-bending", "Take quiet steps while keeping shoulders and hips level.")
    Movement.Swing -> MovementGuide("Bell high between thighs", "Hips and knees fully tall", "Snap the hips; let the bell float instead of lifting with the arms.")
    Movement.Squat -> MovementGuide("Bell close to chest", "Hips down, feet planted", "Sit between the hips with knees tracking over toes; drive up tall.")
    Movement.SplitSquat -> MovementGuide("Long stance, rear foot supported", "Back knee drops toward floor", "Lower mostly straight down and drive through the front foot.")
    Movement.Hinge -> MovementGuide("Soft stance knee, hips square", "Torso and free leg form a line", "Reach the free leg back as the torso hinges forward.")
    Movement.CalfRaise -> MovementGuide("Whole foot grounded", "Heel high, ankle straight", "Rise through the big-toe side, pause, then lower slowly.")
    Movement.TibialisRaise -> MovementGuide("Heels and toes grounded", "Toes lift toward shins", "Keep heels down and move only through the ankles.")
    Movement.Copenhagen -> MovementGuide("Top leg supported, hips low", "Shoulder, hip, and foot align", "Drive the top leg into the support and lift the hips.")
    Movement.HamstringCurl -> MovementGuide("Bridge with legs long", "Heels pull under knees", "Keep hips lifted while the heels slide toward you.")
}

private fun DrawScope.drawMovement(
    movement: Movement,
    finish: Boolean,
    figure: Color,
    accent: Color,
    equipment: Color,
) {
    when (movement) {
        Movement.Press -> drawFloorPress(finish, figure, accent)
        Movement.Row -> drawRow(finish, figure, accent)
        Movement.OverheadPress -> drawOverheadPress(finish, figure, accent)
        Movement.Carry -> drawCarry(finish, figure, accent)
        Movement.Swing -> drawSwing(finish, figure, accent)
        Movement.Squat -> drawSquat(finish, figure, accent)
        Movement.SplitSquat -> drawSplitSquat(finish, figure, accent, equipment)
        Movement.Hinge -> drawSingleLegHinge(finish, figure, accent)
        Movement.CalfRaise -> drawCalfRaise(finish, figure, accent)
        Movement.TibialisRaise -> drawTibialisRaise(finish, figure, accent, equipment)
        Movement.Copenhagen -> drawCopenhagen(finish, figure, accent, equipment)
        Movement.HamstringCurl -> drawHamstringCurl(finish, figure, accent, equipment)
    }
}

private fun DrawScope.drawFloorPress(finish: Boolean, figure: Color, accent: Color) {
    ground(0.86f)
    val head = point(0.17f, 0.63f)
    val shoulder = point(0.29f, 0.66f)
    val hip = point(0.57f, 0.70f)
    val knee = point(0.72f, 0.56f)
    val foot = point(0.84f, 0.86f)
    drawHead(head, figure)
    bone(shoulder, hip, figure)
    bone(hip, knee, figure)
    bone(knee, foot, figure)
    val elbow = if (finish) point(0.35f, 0.45f) else point(0.40f, 0.82f)
    val hand = if (finish) point(0.37f, 0.20f) else point(0.40f, 0.55f)
    bone(shoulder, elbow, accent)
    bone(elbow, hand, accent)
    kettlebell(hand + Offset(0f, -12f), accent)
    if (finish) arrow(point(0.54f, 0.66f), point(0.54f, 0.28f), accent)
}

private fun DrawScope.drawRow(finish: Boolean, figure: Color, accent: Color) {
    ground(0.90f)
    val head = point(0.28f, 0.25f)
    val shoulder = point(0.34f, 0.38f)
    val hip = point(0.53f, 0.57f)
    val knee = point(0.62f, 0.73f)
    val foot = point(0.74f, 0.90f)
    drawHead(head, figure)
    bone(shoulder, hip, figure)
    bone(hip, knee, figure)
    bone(knee, foot, figure)
    bone(shoulder, point(0.63f, 0.54f), figure)
    val elbow = if (finish) point(0.50f, 0.47f) else point(0.37f, 0.58f)
    val hand = if (finish) point(0.45f, 0.58f) else point(0.39f, 0.78f)
    bone(shoulder, elbow, accent)
    bone(elbow, hand, accent)
    kettlebell(hand + Offset(0f, 10f), accent)
    if (finish) arrow(point(0.25f, 0.76f), point(0.42f, 0.56f), accent)
}

private fun DrawScope.drawOverheadPress(finish: Boolean, figure: Color, accent: Color) {
    ground(0.92f)
    val shoulder = standingBody(figure)
    val elbow = if (finish) point(0.56f, 0.24f) else point(0.66f, 0.43f)
    val hand = if (finish) point(0.54f, 0.10f) else point(0.55f, 0.31f)
    bone(shoulder, elbow, accent)
    bone(elbow, hand, accent)
    kettlebell(hand + Offset(0f, -10f), accent)
    if (finish) arrow(point(0.76f, 0.42f), point(0.76f, 0.14f), accent)
}

private fun DrawScope.drawCarry(finish: Boolean, figure: Color, accent: Color) {
    ground(0.92f)
    val shoulder = standingBody(figure, walking = finish)
    val hand = point(0.68f, 0.63f)
    bone(shoulder, point(0.65f, 0.47f), accent)
    bone(point(0.65f, 0.47f), hand, accent)
    kettlebell(hand + Offset(0f, 12f), accent)
    drawLine(accent, point(0.30f, 0.30f), point(0.70f, 0.30f), 3f)
    if (finish) arrow(point(0.30f, 0.82f), point(0.72f, 0.82f), accent)
}

private fun DrawScope.drawSwing(finish: Boolean, figure: Color, accent: Color) {
    ground(0.92f)
    if (finish) {
        val shoulder = standingBody(figure)
        val hand = point(0.79f, 0.37f)
        bone(shoulder, point(0.64f, 0.34f), accent)
        bone(point(0.64f, 0.34f), hand, accent)
        kettlebell(hand + Offset(10f, 0f), accent)
        arrow(point(0.67f, 0.72f), point(0.84f, 0.42f), accent)
    } else {
        val head = point(0.31f, 0.25f)
        val shoulder = point(0.38f, 0.37f)
        val hip = point(0.55f, 0.60f)
        drawHead(head, figure)
        bone(shoulder, hip, figure)
        bone(hip, point(0.64f, 0.72f), figure)
        bone(point(0.64f, 0.72f), point(0.72f, 0.92f), figure)
        bone(hip, point(0.47f, 0.73f), figure)
        bone(point(0.47f, 0.73f), point(0.40f, 0.92f), figure)
        val hand = point(0.46f, 0.69f)
        bone(shoulder, hand, accent)
        kettlebell(hand + Offset(-8f, 10f), accent)
    }
}

private fun DrawScope.drawSquat(finish: Boolean, figure: Color, accent: Color) {
    ground(0.92f)
    val shoulder = point(0.50f, if (finish) 0.38f else 0.28f)
    val hip = point(0.50f, if (finish) 0.63f else 0.56f)
    val leftKnee = point(if (finish) 0.34f else 0.43f, if (finish) 0.70f else 0.73f)
    val rightKnee = point(if (finish) 0.66f else 0.57f, if (finish) 0.70f else 0.73f)
    drawHead(shoulder + Offset(0f, -17f), figure)
    bone(shoulder, hip, figure)
    bone(hip, leftKnee, figure)
    bone(leftKnee, point(0.34f, 0.92f), figure)
    bone(hip, rightKnee, figure)
    bone(rightKnee, point(0.66f, 0.92f), figure)
    val bell = shoulder + Offset(0f, 24f)
    bone(shoulder, bell, accent)
    kettlebell(bell, accent)
    if (finish) arrow(point(0.82f, 0.42f), point(0.82f, 0.72f), accent)
}

private fun DrawScope.drawSplitSquat(finish: Boolean, figure: Color, accent: Color, equipment: Color) {
    ground(0.92f)
    drawLine(equipment, point(0.73f, 0.69f), point(0.96f, 0.69f), 8f, StrokeCap.Round)
    drawLine(equipment, point(0.78f, 0.69f), point(0.78f, 0.92f), 5f)
    val shoulder = point(0.51f, if (finish) 0.38f else 0.26f)
    val hip = point(0.51f, if (finish) 0.65f else 0.55f)
    val frontKnee = point(0.37f, if (finish) 0.70f else 0.72f)
    val backKnee = point(0.67f, if (finish) 0.82f else 0.73f)
    drawHead(shoulder + Offset(0f, -17f), figure)
    bone(shoulder, hip, figure)
    bone(hip, frontKnee, figure)
    bone(frontKnee, point(0.32f, 0.92f), figure)
    bone(hip, backKnee, figure)
    bone(backKnee, point(0.82f, 0.69f), figure)
    kettlebell(point(0.46f, 0.57f), accent)
    if (finish) arrow(point(0.88f, 0.46f), point(0.88f, 0.78f), accent)
}

private fun DrawScope.drawSingleLegHinge(finish: Boolean, figure: Color, accent: Color) {
    ground(0.92f)
    if (!finish) {
        val shoulder = standingBody(figure)
        val hand = point(0.67f, 0.61f)
        bone(shoulder, point(0.64f, 0.46f), accent)
        bone(point(0.64f, 0.46f), hand, accent)
        kettlebell(hand + Offset(0f, 10f), accent)
    } else {
        val head = point(0.22f, 0.40f)
        val shoulder = point(0.32f, 0.46f)
        val hip = point(0.58f, 0.57f)
        val stanceKnee = point(0.57f, 0.74f)
        drawHead(head, figure)
        bone(shoulder, hip, figure)
        bone(hip, stanceKnee, figure)
        bone(stanceKnee, point(0.54f, 0.92f), figure)
        bone(hip, point(0.86f, 0.48f), accent)
        val hand = point(0.40f, 0.74f)
        bone(shoulder, hand, accent)
        kettlebell(hand + Offset(0f, 10f), accent)
        arrow(point(0.25f, 0.28f), point(0.68f, 0.43f), accent)
    }
}

private fun DrawScope.drawCalfRaise(finish: Boolean, figure: Color, accent: Color) {
    ground(0.92f)
    val lift = if (finish) -12f else 0f
    val shoulder = point(0.50f, 0.28f) + Offset(0f, lift)
    val hip = point(0.50f, 0.56f) + Offset(0f, lift)
    val knee = point(0.50f, 0.73f) + Offset(0f, lift)
    val heel = point(0.48f, if (finish) 0.88f else 0.92f)
    val toe = point(0.63f, 0.92f)
    drawHead(shoulder + Offset(0f, -17f), figure)
    bone(shoulder, hip, figure)
    bone(hip, knee, figure)
    bone(knee, heel, figure)
    bone(heel, toe, accent)
    if (finish) arrow(point(0.78f, 0.82f), point(0.78f, 0.55f), accent)
}

private fun DrawScope.drawTibialisRaise(finish: Boolean, figure: Color, accent: Color, equipment: Color) {
    ground(0.92f)
    drawLine(equipment, point(0.25f, 0.08f), point(0.25f, 0.92f), 5f)
    val shoulder = point(0.42f, 0.29f)
    val hip = point(0.36f, 0.56f)
    val knee = point(0.47f, 0.73f)
    val heel = point(0.57f, 0.92f)
    val toe = if (finish) point(0.72f, 0.82f) else point(0.72f, 0.92f)
    drawHead(shoulder + Offset(0f, -17f), figure)
    bone(shoulder, hip, figure)
    bone(hip, knee, figure)
    bone(knee, heel, figure)
    bone(heel, toe, accent)
    if (finish) arrow(point(0.82f, 0.91f), point(0.82f, 0.72f), accent)
}

private fun DrawScope.drawCopenhagen(finish: Boolean, figure: Color, accent: Color, equipment: Color) {
    ground(0.90f)
    drawLine(equipment, point(0.72f, 0.48f), point(0.96f, 0.48f), 8f, StrokeCap.Round)
    drawLine(equipment, point(0.80f, 0.48f), point(0.80f, 0.90f), 5f)
    val shoulder = point(0.30f, 0.54f)
    val hip = point(0.55f, if (finish) 0.50f else 0.68f)
    val topFoot = point(0.82f, 0.48f)
    drawHead(point(0.18f, 0.49f), figure)
    bone(shoulder, hip, figure)
    bone(hip, topFoot, accent)
    bone(shoulder, point(0.34f, 0.80f), figure)
    bone(hip, point(0.68f, 0.76f), figure)
    if (finish) arrow(point(0.52f, 0.80f), point(0.52f, 0.55f), accent)
}

private fun DrawScope.drawHamstringCurl(finish: Boolean, figure: Color, accent: Color, equipment: Color) {
    ground(0.88f)
    val head = point(0.15f, 0.67f)
    val shoulder = point(0.26f, 0.70f)
    val hip = point(0.49f, if (finish) 0.55f else 0.61f)
    val knee = if (finish) point(0.66f, 0.58f) else point(0.68f, 0.70f)
    val heel = if (finish) point(0.78f, 0.86f) else point(0.91f, 0.86f)
    drawHead(head, figure)
    bone(shoulder, hip, figure)
    bone(hip, knee, accent)
    bone(knee, heel, accent)
    drawLine(equipment, heel + Offset(-12f, 8f), heel + Offset(12f, 8f), 6f, StrokeCap.Round)
    if (finish) arrow(point(0.91f, 0.78f), point(0.72f, 0.78f), accent)
}

private fun DrawScope.standingBody(figure: Color, walking: Boolean = false): Offset {
    val head = point(0.50f, 0.15f)
    val shoulder = point(0.50f, 0.29f)
    val hip = point(0.50f, 0.57f)
    val leftFoot = if (walking) point(0.35f, 0.92f) else point(0.43f, 0.92f)
    val rightFoot = if (walking) point(0.66f, 0.92f) else point(0.57f, 0.92f)
    drawHead(head, figure)
    bone(shoulder, hip, figure)
    bone(hip, point(0.45f, 0.75f), figure)
    bone(point(0.45f, 0.75f), leftFoot, figure)
    bone(hip, point(0.55f, 0.75f), figure)
    bone(point(0.55f, 0.75f), rightFoot, figure)
    return shoulder
}

private fun DrawScope.ground(y: Float) {
    drawLine(
        color = Color.Gray,
        start = point(0.05f, y),
        end = point(0.95f, y),
        strokeWidth = 2f,
    )
}

private fun DrawScope.drawHead(center: Offset, color: Color) {
    drawCircle(color = color, radius = 10f, center = center, style = Stroke(width = 4f))
}

private fun DrawScope.bone(start: Offset, end: Offset, color: Color) {
    drawLine(color = color, start = start, end = end, strokeWidth = 6f, cap = StrokeCap.Round)
    drawCircle(color = color, radius = 4f, center = end)
}

private fun DrawScope.kettlebell(center: Offset, color: Color) {
    drawCircle(color = color, radius = 8f, center = center)
    drawLine(color, center + Offset(-5f, -8f), center + Offset(-5f, -13f), 3f, StrokeCap.Round)
    drawLine(color, center + Offset(-5f, -13f), center + Offset(5f, -13f), 3f, StrokeCap.Round)
    drawLine(color, center + Offset(5f, -13f), center + Offset(5f, -8f), 3f, StrokeCap.Round)
}

private fun DrawScope.arrow(start: Offset, end: Offset, color: Color) {
    drawLine(color, start, end, 3f, StrokeCap.Round)
    val angle = atan2(end.y - start.y, end.x - start.x)
    val wing = 10f
    val spread = 0.55f
    drawLine(
        color,
        end,
        Offset(end.x - wing * cos(angle - spread), end.y - wing * sin(angle - spread)),
        3f,
        StrokeCap.Round,
    )
    drawLine(
        color,
        end,
        Offset(end.x - wing * cos(angle + spread), end.y - wing * sin(angle + spread)),
        3f,
        StrokeCap.Round,
    )
}

private fun DrawScope.point(x: Float, y: Float): Offset = Offset(size.width * x, size.height * y)
