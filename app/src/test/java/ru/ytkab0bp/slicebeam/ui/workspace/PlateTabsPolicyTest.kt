package ru.ytkab0bp.slicebeam.ui.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlateTabsPolicyTest {
    @Test
    fun closingTheOnlyPlateKeepsOneEmptyPlateSelected() {
        assertEquals(
            PlateCloseDecision(selectedIndex = 0, resetOnlyPlate = true),
            PlateTabsPolicy.afterClose(plateCount = 1, selectedIndex = 0, closedIndex = 0),
        )
    }

    @Test
    fun closingTheActivePlateSelectsThePreviousPlateWhenPossible() {
        assertEquals(
            PlateCloseDecision(selectedIndex = 1, resetOnlyPlate = false),
            PlateTabsPolicy.afterClose(plateCount = 3, selectedIndex = 2, closedIndex = 2),
        )
    }

    @Test
    fun closingTheFirstActivePlateKeepsTheNewFirstPlateSelected() {
        assertEquals(
            PlateCloseDecision(selectedIndex = 0, resetOnlyPlate = false),
            PlateTabsPolicy.afterClose(plateCount = 3, selectedIndex = 0, closedIndex = 0),
        )
    }

    @Test
    fun closingAnEarlierPlateKeepsTheSameActivePlateAfterIndexShift() {
        assertEquals(
            PlateCloseDecision(selectedIndex = 1, resetOnlyPlate = false),
            PlateTabsPolicy.afterClose(plateCount = 4, selectedIndex = 2, closedIndex = 0),
        )
    }

    @Test
    fun closingALaterPlateDoesNotChangeTheActiveIndex() {
        assertEquals(
            PlateCloseDecision(selectedIndex = 1, resetOnlyPlate = false),
            PlateTabsPolicy.afterClose(plateCount = 4, selectedIndex = 1, closedIndex = 3),
        )
    }

    @Test
    fun invalidPlateIndicesAreRejected() {
        assertNull(PlateTabsPolicy.afterClose(plateCount = 0, selectedIndex = 0, closedIndex = 0))
        assertNull(PlateTabsPolicy.afterClose(plateCount = 2, selectedIndex = 2, closedIndex = 0))
        assertNull(PlateTabsPolicy.afterClose(plateCount = 2, selectedIndex = 0, closedIndex = -1))
    }
}
