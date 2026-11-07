package ru.ytkab0bp.slicebeam

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
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
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspaceModePanelHost

/** Exercises the production workspace tool sheet and outside-dismiss path on Android. */
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
                if (visible.value) {
                    WorkspaceModePanelHost(
                        onDismiss = {
                            visible.value = false
                            dismissed = true
                        },
                    ) {
                        androidx.compose.material3.Text("Tool settings")
                    }
                }
            }
        }

        compose.onNodeWithText("Tool settings").assertIsDisplayed()
        val cancelDescription = context.getString(android.R.string.cancel)
        compose.onNodeWithContentDescription(cancelDescription).performTouchInput {
            click(Offset(10f, 10f))
        }
        compose.runOnIdle {
            assertTrue(dismissed)
            assertTrue(!visible.value)
        }
        compose.onNodeWithText("Tool settings").assertIsNotDisplayed()
    }
}

