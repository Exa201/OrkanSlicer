package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class MmuGateAssignmentPolicyTest {
    @Test fun resolvesSelectedPhysicalGatesForOnlyTheActiveTools() {
        assertArrayEquals(
            intArrayOf(1, 3),
            MmuGateAssignmentPolicy.gatesForTools(intArrayOf(0, 1), intArrayOf(1, 3, 2, 0), intArrayOf(0, 1, 2, 3)),
        )
    }

    @Test fun fallsBackToFirmwareDefaultsWhenThereIsNoSavedSelection() {
        assertArrayEquals(
            intArrayOf(0, 3),
            MmuGateAssignmentPolicy.gatesForTools(intArrayOf(0, 3), null, intArrayOf(0, 1, 2, 3)),
        )
    }

    @Test fun selectingAnOccupiedGateSwapsThePreviousAssignment() {
        assertArrayEquals(
            intArrayOf(0, 3, 2, 1),
            MmuGateAssignmentPolicy.selectGate(intArrayOf(0, 1, 2, 3), 1, 3, intArrayOf(0, 1, 2, 3)),
        )
    }

    @Test fun repairsExistingDuplicateGateChoicesWhenEnoughGatesAreAvailable() {
        assertArrayEquals(
            intArrayOf(1, 3, 2, 0),
            MmuGateAssignmentPolicy.normalize(intArrayOf(1, 3, 2, 3), intArrayOf(0, 1, 2, 3)),
        )
    }

    @Test fun preservesSourceColorsWhenNoPhysicalGateColorIsKnown() {
        assertEquals(
            listOf(0x212721, 0xFFFFFF),
            MmuGateAssignmentPolicy.colorsForGates(
                listOf(0x212721, 0xFFFFFF), intArrayOf(1, 3), emptyMap(), 2,
            ),
        )
    }

    @Test fun selectedGateColorsFollowSlicerToolIndices() {
        assertEquals(
            listOf(0xFFFFFF, 0x0047BB),
            MmuGateAssignmentPolicy.colorsForGates(
                listOf(0x212721, 0xFFFFFF), intArrayOf(1, 3), mapOf(1 to 0xFFFFFF, 3 to 0x0047BB), 2,
            ),
        )
    }
}
