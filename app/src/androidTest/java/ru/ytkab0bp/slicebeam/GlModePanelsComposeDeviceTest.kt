package ru.ytkab0bp.slicebeam

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.WorkspaceComposeToolRoute
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.workspace.EmbossModePanel
import ru.ytkab0bp.slicebeam.ui.workspace.MeasureModePanel
import ru.ytkab0bp.slicebeam.ui.workspace.VariableLayerHeightPanel
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspaceModePanelHost
import ru.ytkab0bp.slicebeam.view.EmbossModeController
import ru.ytkab0bp.slicebeam.view.GLView

@RunWith(AndroidJUnit4::class)
class GlModePanelsComposeDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun workspaceModePanelCanBeDismissedThroughAccessibleBackdrop() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val visible = mutableStateOf(true)
        var dismissed = false
        compose.setContent {
            BeamComposeTheme {
                if (visible.value) {
                    WorkspaceModePanelHost(
                        onDismiss = {
                            dismissed = true
                            visible.value = false
                        },
                    ) {
                        androidx.compose.material3.Text("Workspace tool panel")
                    }
                }
            }
        }

        compose.onNodeWithText("Workspace tool panel").assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(android.R.string.cancel))
            .assertIsDisplayed()
            .performClick()
        compose.runOnIdle { assertTrue(dismissed) }
        compose.onNodeWithText("Workspace tool panel").assertIsNotDisplayed()
    }

    @Test
    fun unfoldMenuRouteIsHostedAndDisposedByTheComposeWorkspacePanel() {
        val menu = LifecycleProbeMenu()
        val content = menu.createWorkspaceContent(portrait = true, fragment = BedFragment())!!
        val visible = mutableStateOf(true)
        var dismissed = 0
        menu.setOnDismiss { dismissed++ }
        menu.setWorkspaceDismiss { visible.value = false }

        compose.setContent {
            BeamComposeTheme {
                if (visible.value) {
                    WorkspaceModePanelHost(onDismiss = { menu.dismiss() }) {
                        menu.WorkspaceContent(content)
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(menu.created == 1) }

        compose.runOnIdle { menu.dismiss() }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(menu.destroyed == 1)
            assertTrue(dismissed == 1)
        }
    }

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

    @Test
    fun measurePanelShowsReadoutsInsideTheComposeWorkspacePanel() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        lateinit var glView: GLView
        compose.runOnUiThread { glView = GLView(context) }
        compose.setContent {
            BeamComposeTheme { MeasureModePanel(glView) {} }
        }

        compose.onNodeWithText(context.getString(R.string.MeasureTitle)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.MeasureDistance)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.MeasureDeltaX)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.MeasureDeltaY)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.MeasureDeltaZ)).assertIsDisplayed()
    }
}

private class LifecycleProbeMenu : WorkspaceComposeToolRoute() {
    var created = 0
    var destroyed = 0

    @androidx.compose.runtime.Composable
    override fun ComposeContent(portrait: Boolean) {
        androidx.compose.material3.Text("Tool lifecycle probe")
    }

    override fun onCreate() {
        created++
    }

    override fun onDestroy() {
        destroyed++
        super.onDestroy()
    }
}
