package ru.ytkab0bp.slicebeam

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
import ru.ytkab0bp.slicebeam.components.SmartSliceFormDialog
import ru.ytkab0bp.slicebeam.components.SmartSliceFormOptions
import ru.ytkab0bp.slicebeam.components.SmartSliceFormSubmission
import ru.ytkab0bp.slicebeam.components.SmartSliceProgressDialog
import ru.ytkab0bp.slicebeam.components.SmartSliceResultDialog
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

/** Checks dialog actions and validation without invoking model analysis or starting a slice. */
@RunWith(AndroidJUnit4::class)
class SmartSliceDialogsDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    private fun options() = SmartSliceFormOptions(
        printerNames = listOf("Printer"),
        purposeOptions = listOf("Decoration", "Functional part"),
        priorityOptions = listOf("Best quality", "Balanced", "Fast"),
        selectedPrinter = 0,
    )

    @Test
    fun invalidDimensionsAreRejectedAndValidValuesReachAnalyzeCallback() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val submissions = mutableListOf<SmartSliceFormSubmission>()
        compose.setContent {
            BeamComposeTheme {
                SmartSliceFormDialog(options(), onDismiss = {}) { submissions += it }
            }
        }

        compose.onNodeWithTag("smart-slice-wall").performScrollTo().performTextReplacement("-1")
        compose.onNodeWithTag("smart-slice-layer").performScrollTo().performTextReplacement("NaN")
        compose.onNodeWithText(context.getString(R.string.SmartSliceAnalyze)).performClick()
        compose.onAllNodesWithText(context.getString(R.string.SmartSliceInvalidDimension)).assertCountEquals(2)
        assertTrue(submissions.isEmpty())

        compose.onNodeWithTag("smart-slice-wall").performScrollTo().performTextReplacement("1,25")
        compose.onNodeWithTag("smart-slice-layer").performScrollTo().performTextReplacement("0.16")
        compose.onNodeWithText(context.getString(R.string.SmartSliceAnalyze)).performClick()

        assertEquals(1, submissions.size)
        assertEquals(0, submissions.single().selectedPrinter)
        assertEquals(0, submissions.single().selectedPurpose)
        assertEquals(1, submissions.single().selectedPriority)
        assertEquals(1.25, submissions.single().values.wallMm ?: Double.NaN, 0.00001)
        assertEquals(0.16, submissions.single().values.layerMm ?: Double.NaN, 0.00001)
    }

    @Test
    fun cancelAndBackCloseFormWithoutAnalysis() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var dismissed = false
        var analyzed = false
        compose.setContent {
            BeamComposeTheme {
                SmartSliceFormDialog(options(), onDismiss = { dismissed = true }) { analyzed = true }
            }
        }
        compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        assertTrue(dismissed)
        assertTrue(!analyzed)
    }

    @Test
    fun androidBackClosesFormWithoutAnalysis() {
        var dismissed = false
        var analyzed = false
        compose.setContent {
            BeamComposeTheme {
                SmartSliceFormDialog(options(), onDismiss = { dismissed = true }) { analyzed = true }
            }
        }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        assertTrue(dismissed)
        assertTrue(!analyzed)
    }

    @Test
    fun progressDialogCancelRequestsAnalysisCancellation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var cancelled = false
        compose.setContent {
            BeamComposeTheme { SmartSliceProgressDialog(onCancel = { cancelled = true }) }
        }

        compose.onNodeWithText(context.getString(android.R.string.cancel)).assertIsDisplayed().performClick()

        assertTrue(cancelled)
    }

    @Test
    fun dismissingResultDoesNotApplyOrStartSlicing() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var dismissed = false
        var appliedForSlice = false
        var appliedForEdit = false
        compose.setContent {
            BeamComposeTheme {
                SmartSliceResultDialog(
                    summary = "Profile suggestions",
                    onDismiss = { dismissed = true },
                    onApplyAndSlice = { appliedForSlice = true },
                    onApplyEdit = { appliedForEdit = true },
                )
            }
        }

        compose.onNodeWithText("Profile suggestions").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.SmartSliceApplyAndSlice)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.SmartSliceApplyEdit)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()

        assertTrue(dismissed)
        assertTrue(!appliedForSlice)
        assertTrue(!appliedForEdit)
    }
}
