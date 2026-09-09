package app.footyos.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import app.footyos.data.local.WorkoutEntryEntity
import app.footyos.domain.ExerciseCatalog
import app.footyos.ui.components.TrainingSession
import app.footyos.ui.theme.FootyOsTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class TrainingFlowTest {
    @get:Rule val compose = createComposeRule()
    private val history = mutableStateListOf<WorkoutEntryEntity>()
    private var failSave = false
    private var saveCalls = 0

    @Before fun setup() {
        InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("training", 0)
            .edit().putBoolean("guided", false).putBoolean("audio", false).commit()
        history.add(WorkoutEntryEntity(1, "2026-01-01", "floor_press", 20.0, 3, 8, 8))
        compose.setContent {
            FootyOsTheme {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                    TrainingSession(ExerciseCatalog.monday.take(2).mapNotNull(ExerciseCatalog::byId),
                        history.filter { it.date == LocalDate.now().toString() }.map { it.exerciseId }.toSet(),
                        history, onSave = {
                            saveCalls++
                            if (failSave) error("Test storage failure")
                            history.add(it.copy(id = history.size + 1L))
                        }, onSessionActive = {})
                }
            }
        }
    }

    @Test fun guidedPreviewActuallyMovesBeforeStartingAndStopsWhenPaused() {
        compose.onNodeWithText("Guided").performScrollTo().performClick()
        compose.onNodeWithText("Pause demo").performScrollTo().assertIsDisplayed()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Loading 3D…").fetchSemanticsNodes().isEmpty()
        }
        // SurfaceView animation uses the real render clock, not Compose's test clock.
        android.os.SystemClock.sleep(2000)
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        fun pixels(): IntArray {
            val screenshot = automation.takeScreenshot()
            val bitmap = screenshot.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
            val result = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(result, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            bitmap.recycle(); screenshot.recycle()
            return result
        }
        // Ignore small tone-mapping/dithering changes; count substantial RGB changes.
        fun changed(a: IntArray, b: IntArray) = a.indices.count {
            val first = a[it]; val second = b[it]
            kotlin.math.abs(((first shr 16) and 255) - ((second shr 16) and 255)) +
                kotlin.math.abs(((first shr 8) and 255) - ((second shr 8) and 255)) +
                kotlin.math.abs((first and 255) - (second and 255)) > 60
        }
        val moving = pixels()
        android.os.SystemClock.sleep(1500)
        assertTrue("The rendered demo should move before starting a workout", changed(moving, pixels()) > 1000)
        compose.onNodeWithText("Pause demo").performClick()
        compose.onNodeWithText("Play demo").assertIsDisplayed()
        android.os.SystemClock.sleep(700)
        val paused = pixels()
        android.os.SystemClock.sleep(1000)
        assertTrue("Pausing the demo should hold its pose", changed(paused, pixels()) < 1000)
        compose.onNodeWithText("Play demo").performClick()
        compose.onNodeWithText("Pause demo").assertIsDisplayed()
        android.os.SystemClock.sleep(1500)
        assertTrue("Resuming should move the model again", changed(paused, pixels()) > 1000)
    }

    @Test fun savingAwardsRecordAndAdvancesOnlyOnce() {
        compose.onNodeWithText("Log sets").performScrollTo().performClick()
        compose.onNodeWithText("Save & next").assertIsNotEnabled()
        compose.onNodeWithText("Reps per set (1–1000)").performScrollTo().performTextInput("9")
        compose.onNodeWithText("Save & next").performClick()
        compose.onNodeWithText("EXERCISE 2 OF 2").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Rep PB • +1 reps/set at the same load", substring = true).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, saveCalls); assertEquals(9, history.last().reps) }
        compose.onNodeWithText("Previous").performScrollTo().performClick()
        compose.onNodeWithText("Last time • 20 lb · 3 sets · 9 reps/set").assertExists()
        compose.onNodeWithText("Personal bests & history").performScrollTo().performClick()
        compose.onNodeWithText("Heaviest • 20 lb").assertIsDisplayed()
    }

    @Test fun failedSaveKeepsEntryAndNeverAdvances() {
        failSave = true
        compose.onNodeWithText("Log sets").performScrollTo().performClick()
        compose.onNodeWithText("Reps per set (1–1000)").performScrollTo().performTextInput("10")
        compose.onNodeWithText("Save & next").performClick()
        compose.onNodeWithText("Couldn't save. Your entry is still here. Try again.").assertExists()
        compose.runOnIdle { assertEquals(1, history.size) }
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("EXERCISE 1 OF 2").assertExists()
    }

    @Test fun guidedSaveStartsNextSetupAndFinalSaveFinishesSession() {
        compose.onNodeWithText("Guided").performScrollTo().performClick()
        compose.onNodeWithText("Start guided exercise").performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(11_000)
        repeat(6) { set ->
            compose.onNodeWithText("Reps done").performScrollTo().performClick()
            if (set < 5) {
                compose.onNodeWithText("Ready now").performScrollTo().performClick()
                compose.mainClock.advanceTimeBy(4_000)
            }
        }
        compose.onNodeWithText("Review & log sets").performScrollTo().performClick()
        compose.onNodeWithText("Reps per set (1–1000)").performScrollTo().performTextInput("8")
        compose.onNodeWithText("Save & next").performClick()
        compose.onNodeWithText("EXERCISE 2 OF 2").assertExists()
        compose.onNodeWithText("Get ready").assertExists()
        compose.onNodeWithText("Pause").performScrollTo().performClick()
        compose.onNodeWithText("End exercise").performScrollTo().performClick()
        compose.onNodeWithText("Simple").performScrollTo().performClick()
        compose.onNodeWithText("Log sets").performScrollTo().performClick()
        compose.onNodeWithText("Reps per set (1–1000)").performScrollTo().performTextInput("8")
        compose.onNodeWithText("Save & finish").performClick()
        compose.onNodeWithText("Session complete • every exercise logged. Great work!").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(2, saveCalls) }
    }

    @Test fun nextDuringGuidedSessionNeedsExplicitSkipAndDoesNotLog() {
        compose.onNodeWithText("Guided").performScrollTo().performClick()
        compose.onNodeWithText("Start guided exercise").performScrollTo().performClick()
        compose.onNodeWithText("Next exercise").performScrollTo().performClick()
        compose.onNodeWithText("Keep training").performClick()
        compose.onNodeWithText("Session list").performScrollTo().performClick()
        compose.onNodeWithText("2. 1-arm kettlebell row", substring = true).performClick()
        compose.onNodeWithText("Move on").performClick()
        compose.onNodeWithText("EXERCISE 2 OF 2").assertExists()
        compose.runOnIdle { assertEquals(0, saveCalls) }
    }
}
