package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.utils.FastenerStandards

class FastenerFormLogicTest {
    @Test
    fun allThreeFormsStartWithPrintableMetricDefaults() {
        assertNull(FastenerFormLogic.validate(FastenerFormLogic.defaults(type = 0, size = 0)))
        assertNull(FastenerFormLogic.validate(FastenerFormLogic.defaults(type = 1, size = 0)))
        assertNull(FastenerFormLogic.validate(FastenerFormLogic.defaults(type = 2, size = 0)))
    }

    @Test
    fun changingKindOrMetricSizeRefreshesItsRelatedDimensions() {
        val bolt = FastenerFormLogic.selectSize(type = 0, size = 2)
        assertEquals("16", bolt.first)
        assertEquals("0.5", bolt.second)
        assertEquals("2.01", bolt.third)
        assertEquals("5.5", bolt.fourth)

        val nut = FastenerFormLogic.selectType(type = 1, size = 2)
        assertEquals("2.4", nut.first)
        assertEquals("0.5", nut.second)
        assertEquals("0.2", nut.third)
        assertEquals("5.5", nut.fourth)

        val washer = FastenerFormLogic.selectType(type = 2, size = 2)
        assertEquals("7", washer.first)
        assertEquals("3.4", washer.second)
        assertEquals("0.8", washer.third)
        assertTrue(FastenerFormLogic.validate(washer) == null)
    }

    @Test
    fun selectingM12UsesTheLargestStandardAndValidBoltDimensions() {
        val m12Index = FastenerStandards.count() - 1
        val bolt = FastenerFormLogic.selectSize(type = 0, size = m12Index)

        assertEquals(12f, FastenerStandards.diameter(bolt.size), 0f)
        assertEquals("1.75", bolt.second)
        assertEquals("18", bolt.fourth)
        assertNull(FastenerFormLogic.validate(bolt))
    }

    @Test
    fun fitPresetUpdatesClearanceAndFitSampleUsesLoosePreset() {
        val nut = FastenerFormLogic.defaults(type = 1, size = 3)
        assertEquals("0.12", FastenerFormLogic.selectFitPreset(nut, 0).third)
        assertEquals("0.3", FastenerFormLogic.selectFitPreset(nut, 2).third)
        assertNull(FastenerFormLogic.validateFitSample(nut))
        assertNotNull(FastenerFormLogic.validateFitSample(nut.copy(fourth = "4")))
    }

    @Test
    fun validationRejectsUnsafeInputsAndAcceptsGermanDecimalCommas() {
        assertNull(FastenerFormLogic.validate(FastenerFormLogic.defaults(type = 0, size = 4).copy(first = "16,5")))
        assertTrue(FastenerFormLogic.parse("0,45") == .45f)
        assertTrue(FastenerFormLogic.parse("NaN").isNaN())
        assertNotNull(FastenerFormLogic.validate(FastenerFormLogic.defaults(type = 0, size = 0).copy(first = "2")))
        assertNotNull(FastenerFormLogic.validate(FastenerFormLogic.defaults(type = 1, size = 0).copy(fourth = "2.5")))
        assertNotNull(FastenerFormLogic.validate(FastenerFormLogic.defaults(type = 2, size = 0).copy(second = "4.8")))
    }

    @Test
    fun invalidKindsAreRejectedAndOutOfRangeDefaultsAreClamped() {
        assertNotNull(FastenerFormLogic.validate(FastenerFormLogic.defaults(type = 0, size = 0).copy(type = 5)))
        val safe = FastenerFormLogic.defaults(type = 7, size = 99)
        assertEquals(2, safe.type)
        assertEquals(8, safe.size)
        assertFalse(FastenerFormLogic.parse("-").isFinite())
    }
}
