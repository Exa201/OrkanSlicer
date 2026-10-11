package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceProfileSelectionLogicTest {
    @Test
    fun selectedProfilesStartAtTheirCurrentIndices() {
        assertEquals(1, WorkspaceProfileSelectionLogic.selectedIndex(listOf("Printer A", "Printer B"), "Printer B"))
        assertEquals(0, WorkspaceProfileSelectionLogic.selectedIndex(listOf("PLA"), "Missing filament"))
        assertEquals(0, WorkspaceProfileSelectionLogic.selectedIndex(emptyList(), null))
    }

    @Test
    fun applyingIndicesResolvesNamesAndDoesNotInventMissingProfiles() {
        val selected = WorkspaceProfileSelectionLogic.resolve(
            listOf("Printer A", "Printer B"), 1,
            listOf("PLA"), 0,
            emptyList(), 0,
        )
        assertEquals("Printer B", selected.printer)
        assertEquals("PLA", selected.filament)
        assertNull(selected.process)
    }

    @Test
    fun profileChangesAreComparedAcrossAllThreeProfileKinds() {
        val before = WorkspaceProfileSelection("Printer A", "PLA", "0.20 mm")
        assertFalse(WorkspaceProfileSelectionLogic.didChange(before, before.copy()))
        assertTrue(WorkspaceProfileSelectionLogic.didChange(before, before.copy(printer = "Printer B")))
        assertTrue(WorkspaceProfileSelectionLogic.didChange(before, before.copy(filament = "PETG")))
        assertTrue(WorkspaceProfileSelectionLogic.didChange(before, before.copy(process = "0.12 mm")))
    }
}
