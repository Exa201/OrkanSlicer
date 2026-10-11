package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelSelectionAvailabilityTest {
    @Test
    fun missingModelDisablesArrangeAndEverySelectionAction() {
        assertEquals(
            ModelSelectionAvailability(canArrange = false, hasSelectedObject = false),
            modelSelectionAvailability(modelLoaded = false, selectedObjectIndex = 0),
        )
    }

    @Test
    fun loadedModelWithoutSelectionOnlyEnablesArrange() {
        assertEquals(
            ModelSelectionAvailability(canArrange = true, hasSelectedObject = false),
            modelSelectionAvailability(modelLoaded = true, selectedObjectIndex = -1),
        )
    }

    @Test
    fun loadedModelWithSelectedObjectEnablesSelectionActions() {
        assertEquals(
            ModelSelectionAvailability(canArrange = true, hasSelectedObject = true),
            modelSelectionAvailability(modelLoaded = true, selectedObjectIndex = 2),
        )
    }
}
