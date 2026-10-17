package ru.ytkab0bp.slicebeam

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspaceSnackbarHost
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspaceSnackbarController
import ru.ytkab0bp.slicebeam.view.SnackbarsLayout

/** Exercises tagged replacement, action callbacks, expiry and font-scaled workspace placement. */
@RunWith(AndroidJUnit4::class)
class WorkspaceSnackbarDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun taggedProgressCanBeReplacedAndActionReceivesAnAndroidView() {
        val controller = WorkspaceSnackbarController()
        var clickedView: android.view.View? = null
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                BeamComposeTheme {
                    Box(Modifier.width(320.dp).height(640.dp)) {
                        WorkspaceSnackbarHost(
                            controller = controller,
                            portrait = true,
                            toolPanelExpanded = true,
                            toolPanelHeightDp = 160,
                            navigationHeightDp = 112,
                            startInsetDp = 8,
                        )
                    }
                }
            }
        }

        compose.runOnIdle {
            controller.show(
                SnackbarsLayout.Snackbar(SnackbarsLayout.Type.LOADING, "Reading model").tag("load")
                    .apply {
                        buttonTitle = "Cancel"
                        buttonClick = android.view.View.OnClickListener { clickedView = it }
                    },
            )
        }
        compose.onNodeWithText("Reading model").assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertNotNull(clickedView)
            assertEquals("com.codemastercody3d.orcaslicermobile.slicetest", clickedView!!.context.packageName)
            controller.show(SnackbarsLayout.Snackbar(SnackbarsLayout.Type.LOADING, "Cancelling").tag("load"))
        }
        compose.onNodeWithText("Cancelling").assertIsDisplayed()
        compose.waitUntil(3_000) {
            compose.onAllNodesWithText("Reading model", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun finiteSnackbarExpiresWhilePersistentProgressWaitsForItsTag() {
        val controller = WorkspaceSnackbarController()
        compose.setContent {
            BeamComposeTheme {
                WorkspaceSnackbarHost(
                    controller = controller,
                    portrait = false,
                    toolPanelExpanded = true,
                    toolPanelHeightDp = 140,
                    navigationHeightDp = 56,
                    startInsetDp = 96,
                )
            }
        }

        compose.runOnIdle {
            controller.show(SnackbarsLayout.Snackbar(SnackbarsLayout.Type.DONE, "Saved").apply { lifetime = 120 })
            controller.show(SnackbarsLayout.Snackbar(SnackbarsLayout.Type.LOADING, "Upload running").tag("upload"))
        }
        compose.onNodeWithText("Saved").assertIsDisplayed()
        compose.onNodeWithText("Upload running").assertIsDisplayed()
        compose.waitUntil(3_000) {
            compose.onAllNodesWithText("Saved", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("Upload running").assertIsDisplayed()

        compose.runOnIdle { controller.dismiss("upload") }
        compose.waitUntil(3_000) {
            compose.onAllNodesWithText("Upload running", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
    }
}
