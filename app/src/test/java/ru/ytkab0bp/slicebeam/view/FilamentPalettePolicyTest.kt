package ru.ytkab0bp.slicebeam.view

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.utils.Prefs

class FilamentPalettePolicyTest {
    @Test
    fun addSlotRemainsAvailableUntilTheSixteenToolLimit() {
        assertTrue(FilamentPalettePolicy.canAddSlot(1))
        assertTrue(FilamentPalettePolicy.canAddSlot(Prefs.MAX_FILAMENT_COLORS - 1))
        assertFalse(FilamentPalettePolicy.canAddSlot(Prefs.MAX_FILAMENT_COLORS))
        assertFalse(FilamentPalettePolicy.canAddSlot(Prefs.MAX_FILAMENT_COLORS + 1))
    }

    @Test
    fun cannotDeleteTheLastRemainingSlot() {
        assertFalse(FilamentPalettePolicy.canDeleteSlot(0))
        assertFalse(FilamentPalettePolicy.canDeleteSlot(1))
        assertTrue(FilamentPalettePolicy.canDeleteSlot(2))
    }

    @Test
    fun deletionRequiresAnExistingIndexAndAnotherSlotToRemain() {
        assertFalse(FilamentPalettePolicy.canDeleteAt(-1, 2))
        assertFalse(FilamentPalettePolicy.canDeleteAt(2, 2))
        assertFalse(FilamentPalettePolicy.canDeleteAt(0, 1))
        assertTrue(FilamentPalettePolicy.canDeleteAt(0, 2))
    }
}
