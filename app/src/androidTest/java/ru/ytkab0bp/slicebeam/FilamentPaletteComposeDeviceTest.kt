package ru.ytkab0bp.slicebeam

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.view.FilamentPaletteContent
import ru.ytkab0bp.slicebeam.view.PaletteSlotSnapshot

/** Exercises the real virtualized palette row with both supported and synthetic UI-only sizes. */
@RunWith(AndroidJUnit4::class)
class FilamentPaletteComposeDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun paletteRowsFromOneThroughTwentyFourEntriesScrollAndSelectTheLastItem() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val count = mutableIntStateOf(1)
        var selectedIndex = -1

        compose.setContent {
            BeamComposeTheme {
                val slots = List(count.intValue) { index ->
                    PaletteSlotSnapshot(
                        color = 0xFF000000.toInt() or ((index * 0x010307) and 0x00FFFFFF),
                        type = "Synthetic PLA $index with a long material label",
                    )
                }
                FilamentPaletteContent(
                    slots = slots,
                    selectedGates = null,
                    onSlotClick = { selectedIndex = it },
                    onAddClick = {},
                )
            }
        }

        listOf(1, 4, 8, 16, 24).forEach { slotCount ->
            compose.runOnUiThread { count.intValue = slotCount }
            compose.waitForIdle()
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(slotCount - 1)
            val lastIndex = slotCount - 1
            val description = context.getString(
                R.string.FilamentPaletteSlotDescription,
                lastIndex,
                "Synthetic PLA $lastIndex with a long material label",
            )
            compose.onNodeWithContentDescription(description).assertIsDisplayed().performClick()
            compose.runOnIdle { assertEquals(lastIndex, selectedIndex) }
        }
    }
}
