package ru.ytkab0bp.slicebeam

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.MmuAssignmentContent
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.utils.FilamentSlot

@RunWith(AndroidJUnit4::class)
class MmuGateAssignmentDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun twoModelColorsCanChooseTheSamePhysicalAceSlot() {
        val mapping = mutableStateOf(listOf(0, 1))
        compose.setContent {
            BeamComposeTheme {
                MmuAssignmentContent(
                    printerTitle = "unverified-test-printer",
                    toolCount = 2,
                    loadedGates = intArrayOf(0, 1),
                    palette = listOf(FilamentSlot(0x202020, "PETG"), FilamentSlot(0xFFFFFF, "PETG")),
                    mapping = mapping.value,
                    usedTools = intArrayOf(0, 1),
                    showHelp = false,
                    onMappingChange = { mapping.value = it },
                )
            }
        }

        compose.onNodeWithTag("mmu-assignment-tool-0").performClick()
        compose.onNodeWithTag("mmu-assignment-gate-1").performClick()
        assertEquals(listOf(1, 1), mapping.value)

        compose.onNodeWithTag("mmu-assignment-tool-1").performClick()
        compose.onNodeWithTag("mmu-assignment-gate-1").performClick()
        assertEquals("Selecting a second logical color must not move the first mapping", listOf(1, 1), mapping.value)
    }
}
