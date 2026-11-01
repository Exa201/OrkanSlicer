package ru.ytkab0bp.slicebeam.slic3r

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Slic3rUtilsNozzleLocaleTest {
    @Test
    fun singleDecimalCommaIsNotMistakenForAnExtruderSeparator() {
        assertEquals("0.6", Slic3rUtils.firstNozzleDiameter("0,6"))
        assertEquals("0.4", Slic3rUtils.firstNozzleDiameter(" 0,4 "))
        assertEquals("0.6", Slic3rUtils.extractNozzle("Printer 0,6 nozzle"))
    }

    @Test
    fun multiNozzleValuesStillSelectTheirFirstEntry() {
        assertEquals("0.4", Slic3rUtils.firstNozzleDiameter("0.4,0.6"))
        assertEquals("0.6", Slic3rUtils.firstNozzleDiameter("0,6;0,4"))
        assertEquals("0.4", Slic3rUtils.firstNozzleDiameter("0.4;0.6"))
    }

    @Test
    fun profileCompatibilityUsesTheParsedDecimalCommaDiameter() {
        assertTrue(Slic3rUtils.isPrinterCompatible(
            presetName = "Printer 0.6 nozzle",
            compatibleList = "",
            condition = "",
            activePrinterName = "Printer",
            activeModel = null,
            activeNozzle = Slic3rUtils.firstNozzleDiameter("0,6"),
            checker = null,
        ))
        assertFalse(Slic3rUtils.isPrinterCompatible(
            presetName = "Printer 0.4 nozzle",
            compatibleList = "",
            condition = "",
            activePrinterName = "Printer",
            activeModel = null,
            activeNozzle = Slic3rUtils.firstNozzleDiameter("0,6"),
            checker = null,
        ))
    }

    @Test
    fun layerHeightAndLineWidthValidationAcceptDecimalCommaValues() {
        assertTrue(Slic3rUtils.layerHeightFitsNozzle("0,24", "0,4"))
        assertFalse(Slic3rUtils.layerHeightFitsNozzle("0,5", "0,4"))
        assertTrue(Slic3rUtils.lineWidthFitsNozzle("0,42", "0,4"))
        assertFalse(Slic3rUtils.lineWidthFitsNozzle("0,6", "0,4"))
    }
}
