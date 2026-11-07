package ru.ytkab0bp.slicebeam

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.setup.SetupMessageDialog

@RunWith(AndroidJUnit4::class)
class SetupMessageDialogDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun setupNoticeDisplaysAndConfirmDismisses() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val visible = mutableStateOf(true)
        compose.setContent {
            BeamComposeTheme {
                if (visible.value) SetupMessageDialog(
                    title = "Setup notice",
                    message = "Profile import could not continue.",
                    confirmLabel = context.getString(android.R.string.ok),
                    onDismiss = { visible.value = false },
                )
            }
        }

        compose.onNodeWithText("Setup notice").assertIsDisplayed()
        compose.onNodeWithText("Profile import could not continue.").assertIsDisplayed()
        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        compose.runOnIdle { assertFalse(visible.value) }
        assertEquals(0, compose.onAllNodesWithText("Setup notice").fetchSemanticsNodes().size)
    }

    @Test
    fun setupNoticeUsesAndroidBackToDismiss() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val visible = mutableStateOf(true)
        compose.setContent {
            BeamComposeTheme {
                if (visible.value) SetupMessageDialog(
                    title = "Setup error",
                    message = "The selected profile was invalid.",
                    confirmLabel = context.getString(android.R.string.ok),
                    onDismiss = { visible.value = false },
                )
            }
        }

        compose.onNodeWithText("Setup error").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.runOnIdle { assertFalse(visible.value) }
    }
}
