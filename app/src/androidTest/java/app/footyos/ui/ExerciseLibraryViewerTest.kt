package app.footyos.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import app.footyos.domain.ExerciseCatalog
import app.footyos.ui.components.Exercise3DViewer
import app.footyos.ui.theme.FootyOsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExerciseLibraryViewerTest {
    @get:Rule val compose = createComposeRule()

    @Test fun everyExerciseLoadsAndRendersMotionOffline() {
        val current = mutableStateOf(ExerciseCatalog.exercises.first())
        compose.setContent { FootyOsTheme { Exercise3DViewer(current.value) } }
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        fun pixels(): IntArray {
            val screenshot = automation.takeScreenshot()
            val bitmap = screenshot.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
            // The scene is at the top; omit controls and the system clock from the comparison.
            val y = 150.coerceAtMost(bitmap.height / 4)
            val height = bitmap.height - y
            val result = IntArray(bitmap.width * height)
            bitmap.getPixels(result, 0, bitmap.width, 0, y, bitmap.width, height)
            bitmap.recycle(); screenshot.recycle()
            return result
        }
        for (exercise in ExerciseCatalog.exercises) {
            compose.runOnIdle { current.value = exercise }
            compose.onNodeWithText("Pause demo").assertIsDisplayed()
            compose.onNodeWithText("Retry 3D preview").assertDoesNotExist()
            compose.waitUntil(15_000) {
                compose.onAllNodesWithText("Loading 3D…").fetchSemanticsNodes().isEmpty()
            }
            // First-use GPU shader compilation can outlast the first frame callback.
            // Observe across several phases rather than treating a hold/blank warm-up as failure.
            var before = pixels()
            var changed = 0
            repeat(8) {
                if (changed <= 100 && exercise.id != "copenhagen") {
                    android.os.SystemClock.sleep(1200)
                    val after = pixels()
                    changed = before.indices.count { index ->
                        val a = before[index]; val b = after[index]
                        kotlin.math.abs(((a shr 16) and 255) - ((b shr 16) and 255)) +
                            kotlin.math.abs(((a shr 8) and 255) - ((b shr 8) and 255)) +
                            kotlin.math.abs((a and 255) - (b and 255)) > 45
                    }
                    before = after
                }
            }
            // A timed Copenhagen hold only has subtle breathing, checked in the GLB validator.
            val evidence = java.io.File(
                InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
                "viewer-review")
            evidence.mkdirs()
            fun capture(label: String) {
                android.os.SystemClock.sleep(700)
                val shot = automation.takeScreenshot()
                java.io.File(evidence, "${exercise.id}_$label.png").outputStream().use {
                    shot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                shot.recycle()
            }
            if (exercise.id == "copenhagen") android.os.SystemClock.sleep(3000)
            capture("default")
            if (exercise.id != "copenhagen") assertTrue("${exercise.id} should visibly animate ($changed pixels)", changed > 100)
            compose.onNodeWithText("Side").performClick()
            capture("side")
            compose.onNodeWithText("Front").performClick()
            capture("front")
            compose.onNodeWithText("Reset view").performClick()
            compose.onNodeWithText("Pause demo").assertIsDisplayed()
        }
    }
}
