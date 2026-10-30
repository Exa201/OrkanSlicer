package ru.ytkab0bp.slicebeam

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.fragment.SettingsChoiceDialog
import ru.ytkab0bp.slicebeam.fragment.SettingsChoiceDialogState
import ru.ytkab0bp.slicebeam.fragment.SettingsFolderPickerErrorDialog
import ru.ytkab0bp.slicebeam.fragment.SettingsResetConfirmationDialog
import ru.ytkab0bp.slicebeam.components.KromacutErrorDialog
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

/** Verifies the shared settings selection dialog's scrolling, selection, and cancellation. */
@RunWith(AndroidJUnit4::class)
class SettingsChoiceDialogDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun longChoiceListsSelectAndCancelWithoutApplyingAValue() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val options = (1..12).map { "Display option $it" }
        var selected by mutableIntStateOf(3)
        var dialog by mutableStateOf<SettingsChoiceDialogState?>(null)

        fun showDialog() {
            dialog = SettingsChoiceDialogState(
                title = R.string.SettingsInterfaceTextSize,
                options = options,
                selectedIndex = selected,
                onSelect = { index ->
                    selected = index
                    dialog = null
                },
            )
        }
        compose.runOnIdle(::showDialog)

        compose.setContent {
            dialog?.let { SettingsChoiceDialog(it) { dialog = null } }
        }

        compose.onNodeWithText(context.getString(R.string.SettingsInterfaceTextSize)).assertIsDisplayed()
        compose.onNodeWithText(options.last()).performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(options.lastIndex, selected)
            assertEquals(null, dialog)
        }

        compose.runOnIdle(::showDialog)
        compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        compose.runOnIdle {
            assertEquals("Cancel leaves the previously selected setting unchanged", options.lastIndex, selected)
            assertEquals(null, dialog)
        }
    }

    @Test
    fun resetConfirmationCancelDoesNotInvokeDestructiveAction() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var confirmed = false
        var dismissed = false
        compose.setContent {
            BeamComposeTheme {
                SettingsResetConfirmationDialog(
                    onDismiss = { dismissed = true },
                    onConfirm = { confirmed = true },
                )
            }
        }

        compose.onNodeWithText(context.getString(R.string.SettingsResetToDefaultTitle)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        compose.runOnIdle {
            assertTrue(dismissed)
            assertFalse("Cancel must never invoke the reset callback", confirmed)
        }
    }

    @Test
    fun resetConfirmationBackDoesNotInvokeDestructiveAction() {
        var confirmed = false
        var dismissed = false
        compose.setContent {
            BeamComposeTheme {
                SettingsResetConfirmationDialog(
                    onDismiss = { dismissed = true },
                    onConfirm = { confirmed = true },
                )
            }
        }

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.runOnIdle {
            assertTrue(dismissed)
            assertFalse("Android Back must never invoke the reset callback", confirmed)
        }
    }

    @Test
    fun resetConfirmationInvokesResetOnlyAfterExplicitConfirm() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var confirmed = false
        compose.setContent {
            BeamComposeTheme {
                SettingsResetConfirmationDialog(onDismiss = {}, onConfirm = { confirmed = true })
            }
        }

        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        compose.runOnIdle { assertTrue(confirmed) }
    }

    @Test
    fun unavailableFolderPickerErrorUsesComposeAndDismisses() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var dismissed = false
        compose.setContent {
            BeamComposeTheme { SettingsFolderPickerErrorDialog { dismissed = true } }
        }

        compose.onNodeWithText(context.getString(R.string.ConverterFailed)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.ConverterFolderPickerUnavailable)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        compose.runOnIdle { assertTrue(dismissed) }
    }

    @Test
    fun longKromacutErrorCanScrollAndDismiss() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val detail = (1..40).joinToString("\n") { "Kromacut failure detail $it" }
        var dismissed = false
        compose.setContent {
            BeamComposeTheme { KromacutErrorDialog(detail) { dismissed = true } }
        }

        compose.onNodeWithText(context.getString(R.string.ConverterFailed)).assertIsDisplayed()
        compose.onNodeWithTag("kromacut-error-message").assert(hasScrollAction())
        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        compose.runOnIdle { assertTrue(dismissed) }
    }
}
