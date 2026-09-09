package app.footyos.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.footyos.ui.components.Exercise3DViewer
import app.footyos.domain.ExerciseCatalog
import app.footyos.ui.theme.FootyOsTheme
import org.junit.Rule
import org.junit.Test

class FloorPressViewerTest {
    @get:Rule val compose = createComposeRule()

    @Test fun loadsOfflineAndKeepsControlsAfterChangingCamera() {
        compose.setContent { FootyOsTheme { Exercise3DViewer(ExerciseCatalog.byId("floor_press")!!) } }
        compose.onNodeWithText("Retry 3D preview").assertDoesNotExist()
        compose.onNodeWithText("Pause demo").performClick()
        compose.onNodeWithText("Play demo").assertExists()
        compose.onNodeWithText("Side").performClick()
        compose.onNodeWithText("Front").performClick()
        compose.onNodeWithText("Reset view").performClick()
        compose.onNodeWithText("1×").performClick()
        compose.onNodeWithText("0.5×").assertExists()
        compose.onNodeWithText("Play demo").performClick()
        compose.onNodeWithText("Pause demo").assertExists()
        compose.onNodeWithText("Retry 3D preview").assertDoesNotExist()
    }
}
