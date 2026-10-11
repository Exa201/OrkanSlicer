package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartSliceInputValuesTest {
    @Test
    fun blankOptionalDimensionsRemainUnset() {
        val result = SmartSliceInputValues.resolve("", "  ")
        assertEquals(null, result.values?.wallMm)
        assertEquals(null, result.values?.layerMm)
        assertFalse(result.invalidWall)
        assertFalse(result.invalidLayer)
    }

    @Test
    fun acceptsPositiveValuesWithLocaleDecimalComma() {
        val result = SmartSliceInputValues.resolve("1,25", "0,16")
        assertEquals(1.25, result.values?.wallMm ?: Double.NaN, 0.00001)
        assertEquals(0.16, result.values?.layerMm ?: Double.NaN, 0.00001)
    }

    @Test
    fun rejectsMalformedAndNonPositiveValuesIndependently() {
        val malformed = SmartSliceInputValues.resolve("abc", "0")
        assertNull(malformed.values)
        assertTrue(malformed.invalidWall)
        assertTrue(malformed.invalidLayer)
        val oneInvalid = SmartSliceInputValues.resolve("", "-0.2")
        assertFalse(oneInvalid.invalidWall)
        assertTrue(oneInvalid.invalidLayer)
    }

    @Test
    fun rejectsInfinityAndNan() {
        val result = SmartSliceInputValues.resolve("NaN", "Infinity")
        assertTrue(result.invalidWall)
        assertTrue(result.invalidLayer)
    }
}
