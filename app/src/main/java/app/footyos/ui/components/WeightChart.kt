package app.footyos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
            color = Color.Gray,
            start = Offset(0f, targetY),
            end = Offset(size.width, targetY),
            strokeWidth = 2f,
        )

        val path = Path()
        weights.forEachIndexed { index, item ->
            val point = Offset(index * xStep, y(item.kilograms))
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, color = Color(0xFF9EE3AD), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f))
    }
}
