package ru.ytkab0bp.slicebeam

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
import ru.ytkab0bp.slicebeam.fragment.PrintConfigToolsLogic
import ru.ytkab0bp.slicebeam.fragment.PrimeTowerPositionDialog
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

/** Exercises the Material Compose dialog window, keyboard, multi-plate values, and explicit Apply. */
@RunWith(AndroidJUnit4::class)
class PrimeTowerKeyboardDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun singlePlateDialogAcceptsImeValuesOnlyAfterApply() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val positions = PrintConfigToolsLogic.towerPositions(null, null)
        var applied: Pair<String, String>? = null

        compose.setContent {
            BeamComposeTheme {
                PrimeTowerPositionDialog(positions, onDismiss = {}, onApply = { applied = it })
            }
        }

        val firstX = compose.onNodeWithTag("prime-tower-x-0")
        firstX.performTextClearance()
        firstX.performTextInput("27.5")
        val firstY = compose.onNodeWithTag("prime-tower-y-0")
        firstY.performTextClearance()
        firstY.performTextInput("42.25")
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        compose.runOnIdle { assertEquals("27.5" to "42.25", applied) }
    }

    @Test
    fun multiPlateDialogKeepsEachCoordinateAndUsesExplicitApply() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val positions = PrintConfigToolsLogic.towerPositions("10,20", "30")
        var applied: Pair<String, String>? = null

        compose.setContent {
            BeamComposeTheme {
                PrimeTowerPositionDialog(positions, onDismiss = {}, onApply = { applied = it })
            }
        }

        compose.onNodeWithTag("prime-tower-x-1").performTextClearance()
        compose.onNodeWithTag("prime-tower-x-1").performTextInput("21.5")
        compose.onNodeWithTag("prime-tower-y-1").performTextClearance()
        compose.onNodeWithTag("prime-tower-y-1").performTextInput("31.25")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()

        compose.runOnIdle { assertEquals("10,21.5" to "30,31.25", applied) }
    }
}
