package app.footyos.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.footyos.domain.Exercise
import app.footyos.domain.Movement

@Composable
fun ExerciseReferenceDialog(
    exercise: Exercise,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(exercise.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExerciseAnimation(exercise.movement)
                Text(exercise.prescription, color = MaterialTheme.colorScheme.primary)
                Text("Form cues", style = MaterialTheme.typography.titleSmall)
                exercise.cues.forEach { Text("• $it") }
                Text("Avoid", style = MaterialTheme.typography.titleSmall)
                exercise.mistakes.forEach { Text("• $it") }
                Text(
                    "Animation is a simplified movement reference, not a biomechanical model.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun ExerciseAnimation(movement: Movement) {
    val transition = rememberInfiniteTransition(label = "exercise")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "movement",
    )
    val outline = MaterialTheme.colorScheme.outline
    val accent = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(8.dp),
    ) {
        val ground = size.height * 0.88f
        drawLine(
            color = outline,
            start = Offset(0f, ground),
            end = Offset(size.width, ground),
            strokeWidth = 3f,
        )
        drawFigure(movement, progress, ground, accent)
    }
}

private fun DrawScope.drawFigure(
    movement: Movement,
    progress: Float,
    ground: Float,
    accent: Color,
) {
    val figure = Color.White
    val stroke = 8f
    val centerX = size.width * 0.5f
    val baseY = when (movement) {
        Movement.Squat, Movement.SplitSquat -> ground - 58f * progress
        else -> ground - 10f
    }
    val torsoTop = Offset(centerX, baseY - 88f)
    val torsoBottom = when (movement) {
        Movement.Hinge, Movement.Swing -> Offset(centerX + 55f * progress, baseY - 30f)
        else -> Offset(centerX, baseY - 32f)
    }
    val head = Offset(torsoTop.x, torsoTop.y - 22f)

    drawCircle(figure, 16f, head, style = Stroke(strokeWidth = 6f))
    drawLine(figure, torsoTop, torsoBottom, stroke, StrokeCap.Round)

    val leftFoot = when (movement) {
        Movement.SplitSquat -> Offset(centerX - 70f, ground)
        Movement.Copenhagen, Movement.HamstringCurl -> Offset(centerX - 88f, ground - 10f)
        else -> Offset(centerX - 34f, ground)
    }
    val rightFoot = when (movement) {
        Movement.SplitSquat -> Offset(centerX + 90f, ground)
        Movement.Copenhagen, Movement.HamstringCurl -> Offset(centerX + 88f, ground - 10f)
        else -> Offset(centerX + 34f, ground)
    }

    val leftKnee = Offset(
        (torsoBottom.x + leftFoot.x) / 2f - 8f * progress,
        (torsoBottom.y + leftFoot.y) / 2f,
    )
    val rightKnee = Offset(
        (torsoBottom.x + rightFoot.x) / 2f + 8f * progress,
        (torsoBottom.y + rightFoot.y) / 2f,
    )
    drawLine(figure, torsoBottom, leftKnee, stroke, StrokeCap.Round)
    drawLine(figure, leftKnee, leftFoot, stroke, StrokeCap.Round)
    drawLine(figure, torsoBottom, rightKnee, stroke, StrokeCap.Round)
    drawLine(figure, rightKnee, rightFoot, stroke, StrokeCap.Round)

    val handHeight = when (movement) {
        Movement.OverheadPress -> torsoTop.y - 70f * progress
        Movement.CalfRaise, Movement.TibialisRaise -> torsoTop.y + 55f
        else -> torsoTop.y + 48f
    }
    val reach = when (movement) {
        Movement.Swing -> 90f * progress
        Movement.Row -> 65f * (1f - progress)
        Movement.Carry -> 42f
        else -> 44f
    }
    val leftHand = Offset(centerX - reach, handHeight)
    val rightHand = Offset(centerX + reach, handHeight)
    drawLine(figure, torsoTop, leftHand, stroke, StrokeCap.Round)
    drawLine(figure, torsoTop, rightHand, stroke, StrokeCap.Round)

    if (
        movement in setOf(
            Movement.Swing,
            Movement.Carry,
            Movement.Row,
            Movement.OverheadPress,
            Movement.Press,
        )
    ) {
        drawCircle(accent, 13f, rightHand)
    }
}
