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

    @Test
    fun pickerUsesCircleOnlyWhenCardsFitTheDisplay() {
        assertEquals(PlatePickerLayout.INLINE, PlatePickerLayoutPolicy.choose(2, 320, 600))
        assertEquals(PlatePickerLayout.GRID, PlatePickerLayoutPolicy.choose(4, 360, 800))
        assertEquals(PlatePickerLayout.GRID, PlatePickerLayoutPolicy.choose(4, 430, 440))
        assertEquals(PlatePickerLayout.CIRCLE, PlatePickerLayoutPolicy.choose(4, 430, 800))
        assertEquals(PlatePickerLayout.GRID, PlatePickerLayoutPolicy.choose(4, 430, 800, 1.3f))
        assertEquals(PlatePickerLayout.GRID, PlatePickerLayoutPolicy.choose(20, 430, 800))
    }

    @Test
    fun platePickerKeepsLargeSyntheticProjectsInScrollableGrid() {
        listOf(1, 3, 6, 10, 20).forEach { count ->
            val layout = PlatePickerLayoutPolicy.choose(count, 411, 800)
            if (count <= 2) assertEquals("$count plates", PlatePickerLayout.INLINE, layout)
            else if (count <= 6) assertEquals("$count plates", PlatePickerLayout.CIRCLE, layout)
            else assertEquals("$count plates", PlatePickerLayout.GRID, layout)
        }
    }

    @Test
    fun largeTextAndSmallDisplaysUseGridForMultiPlateProjects() {
        listOf(3, 6, 10, 20).forEach { count ->
            assertEquals(PlatePickerLayout.GRID, PlatePickerLayoutPolicy.choose(count, 320, 640, 1.5f))
        }
    }
}
