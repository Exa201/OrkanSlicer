package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.ytkab0bp.slicebeam.utils.GCodeToolMapper
import ru.ytkab0bp.slicebeam.utils.FilamentTypes

class MmuColorAssignmentPolicyTest {
    @Test fun knownMaterialMismatchIsBlockedButAnUnknownSlotRemainsUnverified() {
        assertEquals(true, MmuColorAssignmentPolicy.materialMismatch("PLA", "PETG"))
        assertEquals(false, MmuColorAssignmentPolicy.materialMismatch(" PLA ", "pla"))
        assertEquals(false, MmuColorAssignmentPolicy.materialMismatch("PLA", null))
    }
    @Test fun printerMaterialAliasesMatchOrcaCompatibilityFamilies() {
        assertEquals(false, MmuColorAssignmentPolicy.materialMismatch("PLA", "PLA+"))
        assertEquals(false, MmuColorAssignmentPolicy.materialMismatch("PETG", "PETG+"))
        assertEquals(false, MmuColorAssignmentPolicy.materialMismatch("PA", "Nylon"))
        assertEquals(true, MmuColorAssignmentPolicy.materialMismatch("PLA", "PLA-CF"))
        assertEquals(true, MmuColorAssignmentPolicy.materialMismatch("PETG", "PETG-CF"))
        assertEquals(true, MmuColorAssignmentPolicy.materialMismatch("PLA", "PLA Matte"))
        assertEquals(true, MmuColorAssignmentPolicy.materialMismatch("PETG", "PETG HF"))
    }
    @Test fun filamentAliasesMatchOrcaGenericMaterialFamilies() {
        val aliases = listOf(
            "PLA+" to "PLA", "PLA PRO" to "PLA", "PLA MATTE" to "PLA MATTE", "PLA-MATTE" to "PLA MATTE",
            "PLA SILK" to "PLA SILK", "PLA-SILK" to "PLA SILK", "PLA HIGH SPEED" to "PLA HIGH SPEED",
            "PLA-HS" to "PLA HIGH SPEED", "PLA HS" to "PLA HIGH SPEED", "ABS+" to "ABS",
            "PETG+" to "PETG", "PET" to "PETG", "PETG HF" to "PETG HF", "PETG-HF" to "PETG HF",
            "PETG-CF" to "PETG-CF", "PCTG" to "PCTG", "NYLON" to "PA", "PPA" to "PPA-CF",
            "PPA-CF" to "PPA-CF", "PPA-GF" to "PPA-GF", "PE-CF" to "PE-CF", "PP-CF" to "PP-CF",
            "PP-GF" to "PP-GF", "COPE" to "COPE",
        )
        aliases.forEach { (reported, expectedFamily) ->
            assertEquals("Orca alias $reported", expectedFamily, FilamentTypes.compatibilityFamily(reported))
        }
        assertEquals(null, FilamentTypes.compatibilityFamily("  "))
    }
    @Test fun colorsRouteToPhysicalSlotsInADifferentOrderWithoutRenumberingSourceColors() {
        val targets = listOf(MmuColorAssignmentPolicy.Slot(0, 0x8671CB, "PLA"),
            MmuColorAssignmentPolicy.Slot(1, 0xD02727, "PLA"), MmuColorAssignmentPolicy.Slot(2, 0xC8C8C8, "PLA"))
        val mapping = listOf(0xC8C8C8, 0x8671CB, 0xD02727).mapIndexed { tool, color ->
            MmuColorAssignmentPolicy.suggest(color, "PLA", tool, targets)
        }.toIntArray()
        assertEquals("3,1,2", GCodeToolMapper.formatGateList(mapping))
        assertEquals("T2", GCodeToolMapper.remapToolLine("T0", mapping))
        assertEquals("T0", GCodeToolMapper.remapToolLine("T1", mapping))
        assertEquals("T1", GCodeToolMapper.remapToolLine("T2", mapping))
    }
    @Test fun compatibleMaterialIsPreferredOverAnIdenticalColorInAnotherMaterial() {
        val targets = listOf(MmuColorAssignmentPolicy.Slot(0, 0xFFFFFF, "PETG"),
            MmuColorAssignmentPolicy.Slot(2, 0xCCCCCC, "PLA"))
        assertEquals(2, MmuColorAssignmentPolicy.suggest(0xFFFFFF, "PLA", 0, targets))
    }
    @Test fun knownPrinterAliasesPreferTheCompatibleGateBeforeColorDistance() {
        val targets = listOf(
            MmuColorAssignmentPolicy.Slot(0, 0xFFFFFF, "PETG-CF"),
            MmuColorAssignmentPolicy.Slot(2, 0xCCCCCC, "PETG+"),
        )
        assertEquals(2, MmuColorAssignmentPolicy.suggest(0xFFFFFF, "PETG", 0, targets))
    }
    @Test fun unverifiedSlotsUseTheExistingToolOrderAndOnlyAvailableTargets() {
        val targets = listOf(MmuColorAssignmentPolicy.Slot(0, null, null), MmuColorAssignmentPolicy.Slot(3, null, null))
        assertEquals(3, MmuColorAssignmentPolicy.suggest(0xFFFFFF, "PLA", 3, targets))
        assertEquals(0, MmuColorAssignmentPolicy.suggest(0xFFFFFF, "PLA", 2, targets))
    }
    @Test fun alphaAndEqualColorsDoNotChangeSlotPreference() {
        val targets = listOf(MmuColorAssignmentPolicy.Slot(0, 0xFF112233.toInt(), "PLA"),
            MmuColorAssignmentPolicy.Slot(3, 0x112233, "PLA"))
        assertEquals(3, MmuColorAssignmentPolicy.suggest(0x112233, "PLA", 3, targets))
    }
}
