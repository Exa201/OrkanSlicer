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
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.workspace.EmbossModePanel
import ru.ytkab0bp.slicebeam.ui.workspace.VariableLayerHeightPanel
import ru.ytkab0bp.slicebeam.view.EmbossModeController
import ru.ytkab0bp.slicebeam.view.GLView

@RunWith(AndroidJUnit4::class)
class GlModePanelsComposeDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun variableLayerHeightPanelShowsStatusAndReturnsFromMode() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        lateinit var glView: GLView
        var exited = false
        compose.runOnUiThread { glView = GLView(context) }
        compose.setContent {
            BeamComposeTheme { VariableLayerHeightPanel(glView) { exited = true } }
        }

        compose.onNodeWithText(context.getString(R.string.VariableLayerTitle)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.VariableLayerStatus)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.ActionDone)).performClick()
        compose.runOnIdle { assertTrue(exited) }
    }

    @Test
    fun embossPanelShowsEditableSettingsAndOperationModes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        lateinit var controller: EmbossModeController
        var exited = false
        compose.runOnUiThread { controller = EmbossModeController(context, GLView(context)) }
        compose.setContent {
            BeamComposeTheme { EmbossModePanel(controller) { exited = true } }
        }

        compose.onNodeWithText(context.getString(R.string.EmbossInstruction)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.EmbossRaise)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.EmbossEngrave)).assertIsDisplayed().performClick()
        compose.onNodeWithText(context.getString(R.string.ActionDone)).performClick()
        compose.runOnIdle { assertTrue(exited) }
    }
}
