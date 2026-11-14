package ru.ytkab0bp.slicebeam

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.settings.OwnFilamentSlotBar
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Checks the complete T-slot picker and selection at narrow, enlarged-text dimensions. */
@RunWith(AndroidJUnit4::class)
class OwnFilamentSlotBarDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun sixSlotsWrapAfterFourAndRemainSelectableAtLargeFontScale() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SliceBeam
        SliceBeam.INSTANCE = app
        Prefs.init(app)
        val previousSlots = Prefs.getStoredFilamentSlots()
        val slots = (0 until 6).map { index ->
            FilamentSlot(0xFF000000.toInt() or (0x00112233 * (index + 1)), if (index % 2 == 0) "PLA" else "PETG")
        }
        Prefs.setStoredFilamentSlots(slots)
        var selected = -1
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                BeamComposeTheme {
                    OwnFilamentSlotBar(profileTitle = null, onSelect = { selected = it })
                }
            }
        }

        try {
            (0 until 6).forEach { index -> compose.onNodeWithText("T$index").assertIsDisplayed() }
            val firstTop = compose.onNodeWithText("T0").fetchSemanticsNode().boundsInRoot.top
            val fourthTop = compose.onNodeWithText("T3").fetchSemanticsNode().boundsInRoot.top
            val fifthTop = compose.onNodeWithText("T4").fetchSemanticsNode().boundsInRoot.top
            assertTrue("the first four slots should share a row", kotlin.math.abs(firstTop - fourthTop) < 1f)
            assertTrue("the fifth slot should begin a second row", fifthTop > firstTop)

            compose.onNodeWithContentDescription("T5, PETG").performClick()
            compose.runOnIdle { assertEquals(5, selected) }
        } finally {
            Prefs.setStoredFilamentSlots(previousSlots)
        }
    }
}
