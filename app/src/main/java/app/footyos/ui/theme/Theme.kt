package app.footyos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FootyColors = darkColorScheme(
    primary = Color(0xFF9EE3AD),
    secondary = Color(0xFFA9CFFF),
    background = Color(0xFF0F141B),
    surface = Color(0xFF171E27),
    surfaceVariant = Color(0xFF1C2631),
    onPrimary = Color(0xFF102017),
    onBackground = Color(0xFFF6F8FA),
    onSurface = Color(0xFFF6F8FA),
)

@Composable
fun FootyOsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FootyColors,
        content = content,
    )
}
