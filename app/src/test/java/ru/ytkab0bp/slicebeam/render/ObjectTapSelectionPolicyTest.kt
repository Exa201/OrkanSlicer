package ru.ytkab0bp.slicebeam.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObjectTapSelectionPolicyTest {
    @Test
    fun tappingTheSoleSelectedObjectKeepsItsActionsStable() {
        val result = ObjectTapSelectionPolicy.apply(
            currentPrimaryIndex = 2,
            currentSelection = setOf(2),
            tappedIndex = 2,
        )

        assertEquals(2, result.primaryIndex)
        assertEquals(setOf(2), result.selectedIndices)
        assertFalse(result.changed)
    }

    @Test
    fun tappingAnotherObjectSelectsOnlyThatObject() {
        val result = ObjectTapSelectionPolicy.apply(
            currentPrimaryIndex = 2,
            currentSelection = setOf(1, 2),
            tappedIndex = 1,
        )

        assertEquals(1, result.primaryIndex)
        assertEquals(setOf(1), result.selectedIndices)
        assertTrue(result.changed)
    }

    @Test
    fun tappingEmptyPlateClearsSelection() {
        val result = ObjectTapSelectionPolicy.apply(
            currentPrimaryIndex = 2,
            currentSelection = setOf(1, 2),
            tappedIndex = -1,
        )

        assertEquals(-1, result.primaryIndex)
        assertEquals(emptySet<Int>(), result.selectedIndices)
        assertTrue(result.changed)
    }

    @Test
    fun tappingThePrimaryObjectInMultiSelectionCollapsesToIt() {
        val result = ObjectTapSelectionPolicy.apply(
            currentPrimaryIndex = 2,
            currentSelection = setOf(1, 2),
            tappedIndex = 2,
        )

        assertEquals(2, result.primaryIndex)
        assertEquals(setOf(2), result.selectedIndices)
        assertTrue(result.changed)
    }

    @Test
    fun switchingPlatesClearsThePreviousObjectAndItsActions() {
        val result = ObjectTapSelectionPolicy.clearForPlateSwitch(
            currentPrimaryIndex = 2,
            currentSelection = setOf(1, 2),
        )

        assertEquals(-1, result.primaryIndex)
        assertEquals(emptySet<Int>(), result.selectedIndices)
        assertTrue(result.changed)
    }

    @Test
    fun switchingPlatesWithoutASelectionDoesNotEmitAChange() {
        val result = ObjectTapSelectionPolicy.clearForPlateSwitch(
            currentPrimaryIndex = -1,
            currentSelection = emptySet(),
        )

        assertEquals(-1, result.primaryIndex)
        assertEquals(emptySet<Int>(), result.selectedIndices)
        assertFalse(result.changed)
    }
}
