package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransformCutPolicyTest {
    @Test
    fun zRangeOrdersBoundsAndKeepsTheModelMidpoint() {
        val range = TransformCutPolicy.zRange(8.0, 2.0)

        assertEquals(2f, range.min, 0f)
        assertEquals(8f, range.max, 0f)
        assertEquals(5f, range.initial, 0f)
    }

    @Test
    fun flatAndInvalidBoundsStillProduceAUsableFiniteSliderRange() {
        val flat = TransformCutPolicy.zRange(3.5, 3.5)
        assertEquals(3.5f, flat.min, 0f)
        assertTrue(flat.max > flat.min)
        assertEquals(3.5f, flat.initial, 0f)

        val invalid = TransformCutPolicy.zRange(Double.NaN, Double.POSITIVE_INFINITY)
        assertEquals(0f, invalid.min, 0f)
        assertEquals(1f, invalid.max, 0f)
        assertEquals(0.5f, invalid.initial, 0f)
    }

    @Test
    fun degreeConversionPreservesTheNativeCutPlaneConvention() {
        assertEquals(0f, TransformCutPolicy.degreesToRadians(0f), 0f)
        assertEquals(Math.PI.toFloat(), TransformCutPolicy.degreesToRadians(180f), 0.000001f)
        assertEquals(-Math.PI.toFloat(), TransformCutPolicy.degreesToRadians(-180f), 0.000001f)
        assertEquals(Math.PI.toFloat(), TransformCutPolicy.degreesToRadians(270f), 0.000001f)
    }

    @Test
    fun connectorControlsRoundAndClampToSupportedValues() {
        assertEquals(1.23f, TransformCutPolicy.roundHundredths(1.234f), 0.000001f)
        assertEquals(0f, TransformCutPolicy.roundHundredths(Float.NaN), 0f)
        assertEquals(0, TransformCutPolicy.connectorType(-1f))
        assertEquals(2, TransformCutPolicy.connectorType(2.4f))
        assertEquals(0, TransformCutPolicy.connectorType(Float.NaN))
        assertFalse(TransformCutPolicy.zRange(-2.0, 4.0).max.isInfinite())
    }
}
