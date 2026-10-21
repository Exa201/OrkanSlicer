package ru.ytkab0bp.slicebeam

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.fragment.FlushVolumesDialog
import ru.ytkab0bp.slicebeam.fragment.PrintConfigFlushMatrix
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspaceMessageDialog

/** Verifies the profile editor dialog validates and submits only explicit valid values. */
@RunWith(AndroidJUnit4::class)
class PrintConfigToolsDialogsDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    private fun matrix() = PrintConfigFlushMatrix(2, listOf("0", "280", "280", "0"))

    @Test
    fun fourByFourFlushMatrixShowsEveryCellWithoutHorizontalScrolling() {
        val values = List(16) { index -> if (index / 4 == index % 4) "0" else "280" }
        compose.setContent {
            BeamComposeTheme {
                FlushVolumesDialog(PrintConfigFlushMatrix(4, values), onDismiss = {}, onApply = {})
            }
        }

        // Check every cell without calling scrollTo: the complete common 4-slot matrix must fit at once.
        repeat(16) { index -> compose.onNodeWithTag("flush-cell-$index").assertIsDisplayed() }
        val matrixBounds = compose.onNodeWithTag("flush-matrix").fetchSemanticsNode().boundsInRoot
        val lastColumnBounds = compose.onNodeWithTag("flush-cell-3").fetchSemanticsNode().boundsInRoot
        assertTrue("last flush column stays inside the table", lastColumnBounds.right <= matrixBounds.right + 1f)
    }

    @Test
    fun invalidFlushVolumeIsRejectedAndCancelDoesNotApply() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var applied: String? = null
        var dismissed = false
        val largerMatrix = PrintConfigFlushMatrix(6, List(36) { if (it == 0) "0" else "280" })
        compose.setContent {
            BeamComposeTheme {
                FlushVolumesDialog(largerMatrix, onDismiss = { dismissed = true }, onApply = { applied = it })
            }
        }

        compose.onNodeWithTag("flush-cell-30").performScrollTo().performTextClearance()
        compose.onNodeWithTag("flush-cell-30").performTextInput("-1")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        compose.onNodeWithTag("flush-error").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.PrintConfigFlushVolumesInvalid)).assertIsDisplayed()
        assertEquals(null, applied)

        compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        assertTrue(dismissed)
        assertEquals(null, applied)
    }

    @Test
    fun validFlushVolumeAcceptsLocaleDecimalAndSubmitsCanonicalMatrix() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var applied: String? = null
        compose.setContent {
            BeamComposeTheme { FlushVolumesDialog(matrix(), onDismiss = {}, onApply = { applied = it }) }
        }

        compose.onNodeWithTag("flush-cell-1").performTextClearance()
        compose.onNodeWithTag("flush-cell-1").performTextInput("1,25")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()

        compose.runOnIdle { assertEquals("0,1.25,280,0", applied) }
    }

    @Test
    fun mmuSyncErrorMessageCanBeDismissedFromCompose() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var dismissed = false
        compose.setContent {
            BeamComposeTheme {
                WorkspaceMessageDialog(
                    title = context.getString(R.string.MmuSyncFailed),
                    message = "The printer did not return an MMU status.",
                    confirmLabel = context.getString(android.R.string.ok),
                    dismissLabel = null,
                    onDismiss = { dismissed = true },
                    onConfirm = { dismissed = true },
                )
            }
        }

        compose.onNodeWithText(context.getString(R.string.MmuSyncFailed)).assertIsDisplayed()
        compose.onNodeWithText("The printer did not return an MMU status.").assertIsDisplayed()
        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        compose.runOnIdle { assertTrue(dismissed) }
    }
}
