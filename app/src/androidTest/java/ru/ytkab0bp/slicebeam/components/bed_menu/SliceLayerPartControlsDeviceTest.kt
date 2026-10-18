package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.slic3r.GCodePreviewPolicy

/** Exercises the visible layer-part controls and verifies every selection updates the range. */
@RunWith(AndroidJUnit4::class)
class SliceLayerPartControlsDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun selectingPartChangingBlockSizeAndShowingAllUpdatesTheRange() {
        val range = mutableStateOf(SliceLayerRange(from = 1, to = 50, maximum = 120))
        val blockSize = mutableStateOf(50)

        compose.setContent {
            MaterialTheme {
                SliceLayerPartControls(
                    range = range.value,
                    size = blockSize.value,
                    onPart = { part ->
                        val next = GCodePreviewPolicy.blockRange(part, range.value.maximum, blockSize.value)
                        range.value = range.value.copy(from = next.from, to = next.to)
                    },
                    onSize = { size ->
                        blockSize.value = size
                        val part = GCodePreviewPolicy.blockAt(range.value.from, range.value.maximum, size)
                        val next = GCodePreviewPolicy.blockRange(part, range.value.maximum, size)
                        range.value = range.value.copy(from = next.from, to = next.to)
                    },
                    onAll = { range.value = range.value.copy(from = 1, to = range.value.maximum) },
                )
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val currentPartLabel = context.getString(R.string.GCodePreviewPartFormat, 1, 3, 1, 50)
        compose.onNodeWithText(currentPartLabel).performClick()
        compose.onNodeWithText(context.getString(R.string.GCodePreviewPartLabel, 2, 51, 100))
            .assertIsDisplayed().performClick()
        compose.waitForIdle()
        assertEquals(SliceLayerRange(from = 51, to = 100, maximum = 120), range.value)

        compose.onNodeWithText("25", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        assertEquals(25, blockSize.value)
        assertEquals(SliceLayerRange(from = 51, to = 75, maximum = 120), range.value)

        compose.onNodeWithText(context.getString(R.string.GCodePreviewAll)).performClick()
        compose.waitForIdle()
        assertEquals(SliceLayerRange(from = 1, to = 120, maximum = 120), range.value)
        compose.onNodeWithText(context.getString(R.string.GCodePreviewCustomRange, 1, 120)).assertIsDisplayed()
    }
}
