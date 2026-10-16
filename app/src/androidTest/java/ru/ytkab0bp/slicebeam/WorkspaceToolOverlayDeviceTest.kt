package ru.ytkab0bp.slicebeam

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.WorkspaceToolOverlayContent
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

/** Exercises the Compose tool sheet and its outside-dismiss path on a real Android runtime. */
@RunWith(AndroidJUnit4::class)
class WorkspaceToolOverlayDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun toolPanelIsVisibleAndOutsideTapDismissesIt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val visible = mutableStateOf(true)
        var dismissed = false
        compose.setContent {
            BeamComposeTheme {
                WorkspaceToolOverlayContent(
                    visible = visible.value,
                    exitDurationMs = 180,
                    panelFraction = { 0.6f },
                    onDismiss = { visible.value = false },
                    onDismissAnimationFinished = { dismissed = true },
                ) {
                    androidx.compose.material3.Text("Tool settings")
                }
            }
        }

        compose.onNodeWithText("Tool settings").assertIsDisplayed()
        val cancelDescription = context.getString(android.R.string.cancel)
        compose.onNodeWithContentDescription(cancelDescription).performTouchInput {
            click(Offset(10f, 10f))
        }
        compose.waitUntil(3_000) { dismissed }
        compose.runOnIdle {
            assertTrue(dismissed)
            assertTrue(!visible.value)
        }
    }
}

