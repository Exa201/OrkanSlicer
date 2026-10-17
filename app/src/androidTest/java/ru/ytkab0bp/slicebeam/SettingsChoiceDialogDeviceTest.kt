package ru.ytkab0bp.slicebeam

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.fragment.SettingsChoiceDialog
import ru.ytkab0bp.slicebeam.fragment.SettingsChoiceDialogState

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
}
