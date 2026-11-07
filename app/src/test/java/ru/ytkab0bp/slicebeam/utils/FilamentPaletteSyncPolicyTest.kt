package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.*
import org.junit.Test

class FilamentPaletteSyncPolicyTest {
    private fun slot(material: String, rgb: Int) = FilamentSlot(rgb, material)

    @Test fun explicitSyncAddsAllOccupiedFilaments() {
        val gates = listOf(slot("PLA", 0x112233), slot("PETG", 0x445566), slot("ABS", 0x778899), slot("TPU", 0xAABBCC))
        val result = FilamentPaletteSyncPolicy.replaceFromPrinter(gates, intArrayOf(0, 1, 2, 3))
        assertEquals(listOf("PLA", "PETG", "ABS", "TPU"), result.slots.map { it.type })
        assertEquals(gates.map { it.color }, result.slots.map { it.color })
        assertArrayEquals(intArrayOf(0, 1, 2, 3), result.gates)
    }

    @Test fun explicitSyncRetainsEmptySlotsBeforeTheOnlyOccupiedSlot() {
        val result = FilamentPaletteSyncPolicy.replaceFromPrinter(listOf(null, null, slot("PETG", 0xFF0000), null), intArrayOf(0, 1, 2, 3))
        assertEquals(4, result.slots.size)
        assertEquals(listOf(null, null, "PETG", null), result.slots.map { it.type })
        assertArrayEquals(intArrayOf(0, 1, 2, 3), result.gates)
    }

    @Test(expected = IllegalArgumentException::class)
    fun explicitSyncRejectsPermutedPrinterToolMapsInsteadOfCompactingThem() {
        FilamentPaletteSyncPolicy.replaceFromPrinter(
            listOf(slot("PLA", 0xFFFFFF), null, slot("PETG", 0xFF0000), null),
            intArrayOf(2, 0, 3, 1),
        )
    }

    @Test fun secondPhysicalSlotEmptyKeepsThirdAndFourthGcodeToolsAtTheirIndices() {
        val result = FilamentPaletteSyncPolicy.replaceFromPrinter(
            listOf(slot("PLA", 0x111111), null, slot("PETG", 0x222222), slot("TPU", 0x333333)),
            intArrayOf(0, 1, 2, 3),
        )
        assertArrayEquals(intArrayOf(0, 1, 2, 3), result.gates)
        assertEquals(listOf("PLA", null, "PETG", "TPU"), result.slots.map { it.type })
        assertEquals("T1", GCodeToolMapper.remapToolLine("T1", result.gates))
        assertEquals("T2", GCodeToolMapper.remapToolLine("T2", result.gates))
        assertEquals("1,2,3,4", GCodeToolMapper.formatGateList(result.gates))
    }

    @Test(expected = IllegalArgumentException::class)
    fun explicitSyncRejectsPrinterToolMapsThatWouldRewriteSlotCommands() {
        FilamentPaletteSyncPolicy.replaceFromPrinter(listOf(slot("PLA", 0), slot("PETG", 0)), intArrayOf(0, 0))
    }

    @Test(expected = IllegalArgumentException::class) fun emptyPrinterIsRejected() {
        FilamentPaletteSyncPolicy.replaceFromPrinter(listOf(null, null), intArrayOf(0, 1))
    }

    @Test fun automaticRefreshDoesNotRestoreDeletedFilaments() {
        val configured = listOf(slot("PETG", 0))
        val result = FilamentPaletteSyncPolicy.updateExisting(configured, List(4) { slot("PLA", 0xFFFFFF) })
        assertEquals(1, result.size)
        assertEquals("PLA", result[0].type)
    }

    @Test fun automaticRefreshPreservesSlotsWhenPrinterDataIsMissing() {
        val configured = listOf(slot("PETG", 0x123456), slot("PLA", 0))
        val result = FilamentPaletteSyncPolicy.updateExisting(configured, listOf(null, slot("ABS", 0xFFFFFF)))
        assertSame(configured[0], result[0])
        assertEquals("ABS", result[1].type)
        assertEquals(2, result.size)
    }

    @Test fun uploadedColorsFollowRemappedLogicalTools() {
        val slots = listOf(slot("PLA", 0xFFFFFF), slot("PETG", 0xFF0000))
        assertEquals("#FFFFFF;#19C8A0;#FF0000", AnycubicGCodeProcessor.formatMappedPalette(intArrayOf(0, 2), slots, true))
        assertEquals("PLA;PLA;PETG", AnycubicGCodeProcessor.formatMappedPalette(intArrayOf(0, 2), slots, false))
    }

    @Test fun singleLogicalToolKeepsItsMaterialWhenUsingPhysicalSlotFour() {
        val slots = listOf(slot("PETG", 0x123456))
        assertEquals("PLA;PLA;PLA;PETG", AnycubicGCodeProcessor.formatMappedPalette(intArrayOf(3), slots, false))
        assertEquals("#19C8A0;#19C8A0;#19C8A0;#123456", AnycubicGCodeProcessor.formatMappedPalette(intArrayOf(3), slots, true))
    }
}
