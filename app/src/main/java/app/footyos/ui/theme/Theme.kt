package app.footyos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FootyColors = darkColorScheme(
    primary = Color(0xFFD2F65A),
    primaryContainer = Color(0xFF29351B),
    onPrimaryContainer = Color(0xFFD2F65A),
    secondary = Color(0xFFB8C7EE),
    secondaryContainer = Color(0xFF29351B),
    onSecondaryContainer = Color(0xFFD2F65A),
    background = Color(0xFF0B0D0E),
    surface = Color(0xFF151819),
    surfaceVariant = Color(0xFF202425),
    surfaceContainer = Color(0xFF151819),
    surfaceContainerHigh = Color(0xFF202425),
    onSurfaceVariant = Color(0xFFA5ACAE),
    outline = Color(0xFF586062),
    outlineVariant = Color(0xFF2B3032),
    onPrimary = Color(0xFF172005),
    onBackground = Color(0xFFF6F8FA),
    onSurface = Color(0xFFF6F8FA),
)

@Composable
fun FootyOsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FootyColors,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)),
        typography = Typography(
            headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
            headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
            titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
            titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
            labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
        ),
        content = content,
    )
}
