package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransformScalePolicyTest {
    @Test
    fun linkedRulerKeepsTheExistingAxisOffsetsWhenMovingEachAxis() {
        val base = TransformScaleRulerValues(x = 100, y = 80, z = 125)
        val offsets = TransformScalePolicy.linkOffsets(base)

        assertEquals(TransformScaleRulerValues(110, 90, 135), TransformScalePolicy.moveRuler(base, TransformScaleAxis.X, 110, true, offsets))
        assertEquals(TransformScaleRulerValues(120, 100, 145), TransformScalePolicy.moveRuler(base, TransformScaleAxis.Y, 100, true, offsets))
        assertEquals(TransformScaleRulerValues(95, 75, 120), TransformScalePolicy.moveRuler(base, TransformScaleAxis.Z, 120, true, offsets))
    }

    @Test
    fun unlinkedRulerChangesOnlyItsOwnAxisAndNeverScalesBelowOnePercent() {
        val base = TransformScaleRulerValues(100, 80, 125)

        assertEquals(
            TransformScaleRulerValues(1, 80, 125),
            TransformScalePolicy.moveRuler(base, TransformScaleAxis.X, 0, false, TransformScalePolicy.linkOffsets(base)),
        )
    }

    @Test
    fun rulerDragAdvancesOnePercentPerTenDpAndSnapsAtTheSameThreshold() {
        val tenDp = TransformScalePolicy.drag(100, 0f, -20f, density = 2f)
        assertEquals(101, tenDp.value)
        assertEquals(0f, tenDp.progress, 0.0001f)
        assertEquals(101, TransformScalePolicy.settle(101, -0.74f))
        assertEquals(102, TransformScalePolicy.settle(101, -0.75f))
        assertEquals(100, TransformScalePolicy.settle(101, 0.75f))
        assertEquals(TransformScaleRulerDrag(1, 0f), TransformScalePolicy.drag(1, 0f, 100f, density = 1f))
        assertEquals(TransformScaleRulerDrag(150, 0f), TransformScalePolicy.drag(150, 0f, 1f, 1f, min = 200, max = 100))
        assertEquals(150, TransformScalePolicy.settle(150, 0f, min = 200, max = 100))
    }

    @Test
    fun manualPercentAndMillimeterInputsAreFinitePositiveAndLocaleFriendly() {
        assertEquals(1.25, TransformScalePolicy.scaleFactor("125", false, 80.0)!!, 0.000001)
        assertEquals(1.25, TransformScalePolicy.scaleFactor("100,0", true, 80.0)!!, 0.000001)
        assertNull(TransformScalePolicy.scaleFactor("0", false, 80.0))
        assertNull(TransformScalePolicy.scaleFactor("NaN", false, 80.0))
        assertNull(TransformScalePolicy.scaleFactor("50", true, 0.0))
        assertEquals(Int.MAX_VALUE, TransformScalePolicy.roundedPercent(30_000_000.0))
        assertEquals(1, TransformScalePolicy.roundedPercent(Double.POSITIVE_INFINITY))
    }
}
