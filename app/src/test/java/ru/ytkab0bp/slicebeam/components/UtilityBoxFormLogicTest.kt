package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilityBoxFormLogicTest {
    @Test fun standardBoxUsesExistingDimensionsAndIncludesLid() {
        val values = UtilityBoxFormLogic.normalized(UtilityBoxFormLogic.defaults())
        assertEquals("70", values.width)
        assertEquals("50", values.depth)
        assertEquals("35", values.height)
        assertTrue(values.withLid)
    }

    @Test fun invalidValuesFallBackAndValuesOutsideTheRangeAreClamped() {
        assertEquals(70f, UtilityBoxFormLogic.bounded("bad", 70f, 12f, 220f))
        assertEquals(220f, UtilityBoxFormLogic.bounded("300", 70f, 12f, 220f))
        assertEquals(0f, UtilityBoxFormLogic.bounded("-2", 0f, 0f, 60f))
    }

    @Test fun lidCanBeDisabledWithoutChangingBoxDimensions() {
        val values = UtilityBoxFormLogic.normalized(UtilityBoxFormLogic.defaults().copy(withLid = false))
        assertFalse(values.withLid)
        assertEquals("8", values.lidSkirt)
        assertEquals("0.3", values.clearance)
    }
}
