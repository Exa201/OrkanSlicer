package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrinterScopedProcessProfileTest {
    @Test fun editableCopiesAreBoundToTheirPrinter() {
        val source = ConfigObject("Standard").apply { put("line_width", "105%") }
        val kobra = PrinterScopedProcessProfile.copy(source, "Kobra 3 V2")
        val qidi = PrinterScopedProcessProfile.copy(source, "Qidi Q2")
        kobra.put("line_width", "110%")

        assertEquals("105%", source["line_width"])
        assertEquals("110%", kobra["line_width"])
        assertEquals("105%", qidi["line_width"])
        assertTrue(PrinterScopedProcessProfile.belongsTo(kobra, "Kobra 3 V2"))
        assertFalse(PrinterScopedProcessProfile.belongsTo(kobra, "Qidi Q2"))
    }
}
