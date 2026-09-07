package app.footyos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import app.footyos.data.local.WeightEntity

@Composable
fun WeightChart(
    weights: List<WeightEntity>,
    targetKg: Double,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    if (weights.isEmpty()) {
        Column(modifier.fillMaxWidth().height(140.dp), verticalArrangement = Arrangement.Center) {
            Text("Your progress starts here", style = MaterialTheme.typography.titleMedium)
            Text("Add a weigh-in on Today to see your weight trend.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        if (weights.isEmpty()) return@Canvas

        val values = weights.map { it.kilograms }
        val min = minOf(values.min(), targetKg) - 1.0
        val max = maxOf(values.max(), targetKg) + 1.0
        val range = (max - min).coerceAtLeast(1.0)
        val xStep = if (weights.size == 1) 0f else size.width / (weights.size - 1)
        fun y(value: Double) = size.height - (((value - min) / range) * size.height).toFloat()

        val targetY = y(targetKg)
        drawLine(
            color = grid,
            start = Offset(0f, targetY),
            end = Offset(size.width, targetY),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
        )

        val path = Path()
        weights.forEachIndexed { index, item ->
            val point = Offset(index * xStep, y(item.kilograms))
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        val fill = Path().apply {
            addPath(path)
            lineTo((weights.size - 1) * xStep, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, brush = Brush.verticalGradient(listOf(accent.copy(alpha = 0.22f), Color.Transparent)))
        drawPath(path, color = accent, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f))
        weights.forEachIndexed { index, item -> drawCircle(accent, radius = 5f, center = Offset(index * xStep, y(item.kilograms))) }
    }
}
