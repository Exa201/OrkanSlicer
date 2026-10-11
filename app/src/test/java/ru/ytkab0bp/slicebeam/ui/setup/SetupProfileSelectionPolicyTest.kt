package ru.ytkab0bp.slicebeam.ui.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupProfileSelectionPolicyTest {
    @Test
    fun togglingChangesOnlyTheRequestedProfile() {
        val selected = SetupProfileSelectionPolicy.toggle(setOf("printer-a", "printer-b"), "printer-a", false)
        assertEquals(setOf("printer-b"), selected)
        assertEquals(setOf("printer-b", "printer-c"), SetupProfileSelectionPolicy.toggle(selected, "printer-c", true))
    }

    @Test
    fun selectingAllRetainsStableUniqueKeys() {
        assertEquals(listOf("filament-a", "filament-b"), SetupProfileSelectionPolicy.selectAll(listOf("filament-a", "filament-b", "filament-a")).toList())
    }

    @Test
    fun printerPageRequiresASelectionExceptForCustomProfiles() {
        assertFalse(SetupProfileSelectionPolicy.canContinue(useCustomProfile = false, selectedPrinterCount = 0))
        assertTrue(SetupProfileSelectionPolicy.canContinue(useCustomProfile = false, selectedPrinterCount = 1))
        assertTrue(SetupProfileSelectionPolicy.canContinue(useCustomProfile = true, selectedPrinterCount = 0))
    }
}
