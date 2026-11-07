package ru.ytkab0bp.slicebeam

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.settings.ComposeProfileNumericOption
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

/** Verifies numeric profile edits commit only valid values and are discarded on cancel/Back. */
@RunWith(AndroidJUnit4::class)
class ProfileNumericOptionDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun numericProfileValueAppliesOnlyAfterConfirmation() {
        val changes = CopyOnWriteArrayList<String>()
        compose.setContent {
            BeamComposeTheme {
                ComposeProfileNumericOption.Content(
                    title = "Nozzle temperature",
                    initialValue = "220",
                    unit = "°C",
                    description = "",
                    minimum = 100f,
                    maximum = 300f,
                    integer = true,
                    percentAllowed = false,
                    percentRequired = false,
                    onChanged = Consumer(changes::add),
                    onLongClick = Runnable {},
                )
            }
        }

        compose.onNodeWithText("Nozzle temperature").performClick()
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("235")
        assertEquals("Typing in the draft editor must not immediately modify the profile", emptyList<String>(), changes.toList())
        val ok = InstrumentationRegistry.getInstrumentation().targetContext.getString(android.R.string.ok)
        compose.onNodeWithText(ok).performClick()
        compose.waitForIdle()

        assertEquals(listOf("235"), changes.toList())
        compose.onNodeWithText("235").assertIsDisplayed()
    }

    @Test
    fun numericProfileValueIsDiscardedByCancelAndAndroidBack() {
        val changes = CopyOnWriteArrayList<String>()
        compose.setContent {
            BeamComposeTheme {
                ComposeProfileNumericOption.Content(
                    title = "Nozzle temperature",
                    initialValue = "220",
                    unit = "°C",
                    description = "",
                    minimum = 100f,
                    maximum = 300f,
                    integer = true,
                    percentAllowed = false,
                    percentRequired = false,
                    onChanged = Consumer(changes::add),
                    onLongClick = Runnable {},
                )
            }
        }

        compose.onNodeWithText("Nozzle temperature").performClick()
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("250")
        val cancel = InstrumentationRegistry.getInstrumentation().targetContext.getString(android.R.string.cancel)
        compose.onNodeWithText(cancel).performClick()
        compose.waitForIdle()
        assertEquals(emptyList<String>(), changes.toList())
        compose.onNodeWithText("220").assertIsDisplayed()

        compose.onNodeWithText("Nozzle temperature").performClick()
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("260")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertEquals("Android Back discards the unconfirmed numeric edit", emptyList<String>(), changes.toList())
        compose.onNodeWithText("220").assertIsDisplayed()
    }

    @Test
    fun outOfRangeNumericProfileValueCannotBeApplied() {
        val changes = CopyOnWriteArrayList<String>()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            BeamComposeTheme {
                ComposeProfileNumericOption.Content(
                    title = "Nozzle temperature",
                    initialValue = "220",
                    unit = "°C",
                    description = "",
                    minimum = 100f,
                    maximum = 300f,
                    integer = true,
                    percentAllowed = false,
                    percentRequired = false,
                    onChanged = Consumer(changes::add),
                    onLongClick = Runnable {},
                )
            }
        }

        compose.onNodeWithText("Nozzle temperature").performClick()
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("400")
        compose.onNodeWithText(context.getString(R.string.ProfileNumericOutOfRange)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(android.R.string.ok)).assertIsNotEnabled()
        assertEquals(emptyList<String>(), changes.toList())
    }
}
