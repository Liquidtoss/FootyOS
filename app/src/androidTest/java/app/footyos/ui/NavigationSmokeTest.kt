package app.footyos.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.platform.app.InstrumentationRegistry
import app.footyos.MainActivity
import org.junit.Rule
import org.junit.Test
import java.io.File
import android.graphics.Bitmap

class NavigationSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun mainScreensAndFormRemainReachable() {
        compose.onNodeWithText("Your daily edge").assertIsDisplayed()
        capture("today")
        compose.onNodeWithText("Nutrition", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Fuel your game").assertIsDisplayed()
        capture("nutrition")
        compose.onNodeWithText("Training", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Built for the pitch").assertIsDisplayed()
        capture("training")
        compose.onNodeWithText("Monday · Upper").performClick()
        compose.onNodeWithContentDescription("View form").performScrollTo().performClick()
        compose.onNodeWithText("Form cues").assertIsDisplayed()
        capture("form")
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Guided").performScrollTo().performClick()
        compose.onNodeWithText("Start guided exercise").performScrollTo().performClick()
        compose.onNodeWithText("Pause", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Paused").assertIsDisplayed()
        capture("guided-paused")
        compose.onNodeWithText("End exercise").performScrollTo().performClick()
        compose.onNodeWithText("Simple · log only").performScrollTo().performClick()
        compose.onNodeWithText("Log sets").performScrollTo().performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Soccer", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Own your wing").assertIsDisplayed()
        capture("soccer")
        compose.onNodeWithText("Progress", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Every session counts").assertIsDisplayed()
        capture("progress")
        compose.onNodeWithContentDescription("Schedule").performClick()
        compose.onNodeWithText("Make room for better").assertIsDisplayed()
        capture("schedule")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        // Compose idleness does not include Android's dialog-window enter animation.
        android.os.SystemClock.sleep(350)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = instrumentation.uiAutomation.takeScreenshot()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png")
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
        instrumentation.uiAutomation.executeShellCommand("cp ${file.absolutePath} /data/local/tmp/footyos-$name.png").use {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()
        }
    }
}
