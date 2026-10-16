package ru.ytkab0bp.slicebeam.view

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.utils.Prefs

class FilamentPalettePolicyTest {
    @Test
    fun workspacePaletteUsesSheetOnPhonesAndSidePanelOnWideWindows() {
        assertEquals(FilamentPalettePolicy.WorkspacePresentation.BOTTOM_SHEET,
            FilamentPalettePolicy.workspacePresentation(411))
        assertEquals(FilamentPalettePolicy.WorkspacePresentation.BOTTOM_SHEET,
            FilamentPalettePolicy.workspacePresentation(599))
        assertEquals(FilamentPalettePolicy.WorkspacePresentation.SIDE_PANEL,
            FilamentPalettePolicy.workspacePresentation(600))
    }

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

    @Test
    fun syntheticPaletteCountsRespectTheSupportedSlotLimitWithoutChangingIt() {
        listOf(1, 4, 8, 16).forEach { count ->
            assertTrue("$count slots can be displayed", count <= Prefs.MAX_FILAMENT_COLORS)
            assertEquals(count < Prefs.MAX_FILAMENT_COLORS, FilamentPalettePolicy.canAddSlot(count))
            assertEquals(count > 1, FilamentPalettePolicy.canDeleteSlot(count))
        }
        // 24 is useful as an overflow guard for synthetic UI data, but beyond the product's slot limit.
        assertFalse(FilamentPalettePolicy.canAddSlot(24))
        assertFalse(FilamentPalettePolicy.canAddSlot(Prefs.MAX_FILAMENT_COLORS + 1))
    }
}
