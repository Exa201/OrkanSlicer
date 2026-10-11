package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickSliceProfileValuesTest {
    @Test
    fun qualityPresetReplacesUnchangedExistingLayerHeight() {
        val result = QuickSliceProfileValues.resolve(0, "0.20", "0.20", "15", false, false)
        assertEquals("0.28", result.settings?.layerHeight)
        assertEquals("15", result.settings?.infillPercent)
    }

    @Test
    fun customLayerHeightAcceptsGermanDecimalCommaAndIsCanonicalized() {
        val result = QuickSliceProfileValues.resolve(2, "0.20", "0,320", "20,0", true, true)
        assertEquals("0.32", result.settings?.layerHeight)
        assertEquals("20", result.settings?.infillPercent)
        assertTrue(result.settings?.supports == true)
        assertTrue(result.settings?.brim == true)
    }

    @Test
    fun blankInputsUseSafeDefaults() {
        val result = QuickSliceProfileValues.resolve(1, "0.20", "", "", false, false)
        assertEquals("0.20", result.settings?.layerHeight)
        assertEquals("15", result.settings?.infillPercent)
    }

    @Test
    fun rejectsNonPositiveLayerAndOutOfRangeInfill() {
        val result = QuickSliceProfileValues.resolve(1, "0.20", "0", "101", false, false)
        assertNull(result.settings)
        assertTrue(result.invalidLayerHeight)
        assertTrue(result.invalidInfill)
    }

    @Test
    fun rejectsMalformedValuesAndAcceptsInfillBounds() {
        assertTrue(QuickSliceProfileValues.resolve(1, "0.20", "oops", "0", false, false).invalidLayerHeight)
        assertTrue(QuickSliceProfileValues.resolve(1, "0.20", "0.2", "nope", false, false).invalidInfill)
        val bounds = QuickSliceProfileValues.resolve(1, "0.20", "0.2", "100", false, false)
        assertFalse(bounds.invalidLayerHeight)
        assertFalse(bounds.invalidInfill)
        assertEquals("100", bounds.settings?.infillPercent)
    }
}
