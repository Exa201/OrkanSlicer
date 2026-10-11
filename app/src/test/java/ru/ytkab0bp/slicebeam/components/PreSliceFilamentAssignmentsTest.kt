package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreSliceFilamentAssignmentsTest {
    @org.junit.Test fun keepingImportedColorsDoesNotForceTheParentsSingleSlotOntoItsParts() {
        org.junit.Assert.assertArrayEquals(intArrayOf(-1), PreSliceFilamentAssignments.resolve(intArrayOf(0), 0, true, true))
        org.junit.Assert.assertArrayEquals(intArrayOf(2,2), PreSliceFilamentAssignments.resolve(intArrayOf(0,1), 2, true, false))
        org.junit.Assert.assertArrayEquals(intArrayOf(0,1), PreSliceFilamentAssignments.resolve(intArrayOf(0,1), 2, false, false))
    }
    @Test
    fun appSlotsMapToZeroBasedGcodeToolsAndClampUnavailableTools() {
        assertArrayEquals(
            intArrayOf(0, 1, 2, 2),
            PreSliceFilamentAssignments.initialSlotIndices(intArrayOf(1, 2, 3, 9), slotCount = 3),
        )
    }

    @Test
    fun oneSlotModeCopiesTheSelectionAcrossEveryObjectWithoutMutatingInput() {
        val original = intArrayOf(0, 2, 1)

        val result = PreSliceFilamentAssignments.applySingleSlot(original, selectedSlot = 1)

        assertArrayEquals(intArrayOf(0, 2, 1), original)
        assertArrayEquals(intArrayOf(1, 1, 1), result)
    }

    @Test
    fun assignmentModeDetectsUniformAndIndividualObjectSlots() {
        assertTrue(PreSliceFilamentAssignments.allUseSameSlot(intArrayOf(2, 2, 2)))
        assertFalse(PreSliceFilamentAssignments.allUseSameSlot(intArrayOf(0, 1)))
        assertTrue(PreSliceFilamentAssignments.allUseSameSlot(intArrayOf()))
    }
}
