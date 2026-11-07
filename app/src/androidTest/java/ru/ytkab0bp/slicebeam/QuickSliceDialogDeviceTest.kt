package ru.ytkab0bp.slicebeam

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.bed_menu.QuickSliceDialogContent
import ru.ytkab0bp.slicebeam.components.bed_menu.QuickSliceDialogOptions
import ru.ytkab0bp.slicebeam.components.bed_menu.QuickSliceProfileSettings
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

/** Verifies that invalid input is held in the Compose dialog and valid values reach its callback. */
@RunWith(AndroidJUnit4::class)
class QuickSliceDialogDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    private fun options() = QuickSliceDialogOptions(
        printerNames = listOf("Printer"),
        filamentNames = listOf("PLA"),
        projectFilamentSummary = null,
        qualityOptions = listOf("Draft", "Standard", "Fine"),
        selectedPrinter = 0,
        selectedFilament = 0,
        selectedQuality = 1,
        originalLayerHeight = "0.20",
        layerHeight = "0.20",
        infill = "15",
        supports = true,
        brim = false,
    )

    @Test
    fun invalidValuesDoNotStart_andCorrectedValuesAreReturned() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val starts = mutableListOf<QuickSliceProfileSettings>()
        var selectedPrinter = -1
        var selectedFilament = -1

        compose.setContent {
            BeamComposeTheme {
                QuickSliceDialogContent(options(), onDismiss = {}) { printer, filament, settings ->
                    selectedPrinter = printer
                    selectedFilament = filament
                    starts += settings
                }
            }
        }

        compose.onNodeWithTag("quick-slice-layer-height").performScrollTo().performTextReplacement("-1")
        compose.onNodeWithTag("quick-slice-infill").performScrollTo().performTextReplacement("101")
        compose.onNodeWithText(context.getString(R.string.QuickSliceStart)).performClick()
        compose.onNodeWithText(context.getString(R.string.QuickSliceInvalidLayerHeight)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.QuickSliceInvalidInfill)).assertIsDisplayed()
        assertTrue(starts.isEmpty())

        compose.onNodeWithTag("quick-slice-layer-height").performScrollTo().performTextReplacement("0.24")
        compose.onNodeWithTag("quick-slice-infill").performScrollTo().performTextReplacement("85")
        compose.onNodeWithText(context.getString(R.string.QuickSliceStart)).performClick()

        assertEquals(0, selectedPrinter)
        assertEquals(0, selectedFilament)
        assertEquals(QuickSliceProfileSettings("0.24", "85", supports = true, brim = false), starts.single())
    }

    @Test
    fun cancelClosesWithoutStarting() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var dismissed = false
        var started = false
        compose.setContent {
            BeamComposeTheme {
                QuickSliceDialogContent(options(), onDismiss = { dismissed = true }) { _, _, _ ->
                    started = true
                }
            }
        }

        compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()

        assertTrue(dismissed)
        assertTrue(!started)
    }

    @Test
    fun androidBackDismissesWithoutStarting() {
        var dismissed = false
        var started = false
        compose.setContent {
            BeamComposeTheme {
                QuickSliceDialogContent(options(), onDismiss = { dismissed = true }) { _, _, _ ->
                    started = true
                }
            }
        }

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)

        assertTrue(dismissed)
        assertTrue(!started)
    }
}
