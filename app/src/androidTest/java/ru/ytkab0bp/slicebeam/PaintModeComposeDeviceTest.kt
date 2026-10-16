package ru.ytkab0bp.slicebeam

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.render.GLRenderer
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.workspace.PaintModePanel
import ru.ytkab0bp.slicebeam.view.GLView

/** Verifies the Compose paint controls and their accessible, dismissible mode actions. */
@RunWith(AndroidJUnit4::class)
class PaintModeComposeDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun paintPanelExposesBrushControlsAndSeparateCommitAndCancelActions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        lateinit var glView: GLView
        var committed = false
        var cancelled = false
        compose.runOnUiThread { glView = GLView(context) }
        compose.setContent {
            BeamComposeTheme {
                PaintModePanel(
                    glView = glView,
                    mode = GLRenderer.PAINT_MODE_COLOR,
                    onCommit = { committed = true },
                    onCancel = { cancelled = true },
                )
            }
        }

        val brushLabel = context.getString(R.string.PaintBrush)
        val bucketLabel = context.getString(R.string.PaintBucket)
        val cancelLabel = context.getString(android.R.string.cancel)
        compose.onNodeWithText(brushLabel).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.PaintDisableBrush)).assertIsDisplayed().performClick()
        compose.onNodeWithText(context.getString(R.string.PaintEnableBrush)).assertIsDisplayed()
        compose.onNodeWithText(bucketLabel).performClick()
        compose.onNodeWithText(context.getString(R.string.PaintAngleLimit, 30)).assertIsDisplayed()
        compose.onNodeWithText(cancelLabel).assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(cancelled && !committed) }
    }
}
