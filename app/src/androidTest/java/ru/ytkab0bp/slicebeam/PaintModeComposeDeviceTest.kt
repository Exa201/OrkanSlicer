package ru.ytkab0bp.slicebeam

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
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
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import ru.ytkab0bp.slicebeam.utils.Prefs
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

    @Test
    fun paintPanelDoneUsesCommitAction() {
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

        compose.onNodeWithText(context.getString(R.string.ActionDone)).assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(committed && !cancelled) }
    }

    @Test
    fun colorPaintUsesOnePaletteAndSeparatesBaseFromBrushColor() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        lateinit var glView: GLView
        val previousSlots = Prefs.getStoredFilamentSlots().map { FilamentSlot(it.color, it.type) }
        Prefs.setStoredFilamentSlots(listOf(
            FilamentSlot(0xFFCC3333.toInt(), "Palette Base"),
            FilamentSlot(0xFF3366CC.toInt(), "Palette Accent"),
            FilamentSlot(0xFF33AA55.toInt(), "Palette Green"),
            FilamentSlot(0xFFFFCC22.toInt(), "Palette Yellow"),
        ))
        compose.runOnUiThread { glView = GLView(context) }
        try {
            compose.setContent {
                BeamComposeTheme {
                    PaintModePanel(glView, GLRenderer.PAINT_MODE_COLOR, {}, {})
                }
            }

            compose.onAllNodesWithTag("paint-palette-row").assertCountEquals(1)
            compose.onNodeWithTag("paint-palette-row").assertIsDisplayed()
            compose.onNodeWithTag("paint-palette-role-toggle").assertIsDisplayed()
            compose.onNodeWithTag("paint-palette-role-toggle")
                .assertIsDisplayed().performClick()
            compose.onNodeWithText(context.getString(R.string.PaintBaseFilament)).assertIsDisplayed()
            compose.onAllNodesWithTag("paint-filament-swatch-1").assertCountEquals(1)
            compose.onAllNodesWithTag("paint-filament-swatch-2").assertCountEquals(1)
            compose.onNodeWithTag("paint-filament-swatch-1").assertIsDisplayed()
            compose.onNodeWithTag("paint-filament-swatch-2").assertIsDisplayed()
            compose.onNodeWithTag("paint-filament-swatch-3").assertIsDisplayed()
            compose.onNodeWithTag("paint-filament-swatch-4").assertIsDisplayed()
            compose.onNodeWithTag("paint-filament-swatch-1").assertIsDisplayed().performClick()
            compose.onNodeWithTag("paint-palette-role-toggle").performClick()
            compose.onNodeWithText(context.getString(R.string.PaintWith)).assertIsDisplayed()
            compose.onNodeWithTag("paint-filament-swatch-2").performClick()
        } finally {
            Prefs.setStoredFilamentSlots(previousSlots)
        }
    }
}
