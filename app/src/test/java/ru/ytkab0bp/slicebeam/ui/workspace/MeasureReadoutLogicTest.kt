package ru.ytkab0bp.slicebeam.ui.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.ytkab0bp.slicebeam.utils.Vec3d

class MeasureReadoutLogicTest {
    @Test
    fun readoutStaysEmptyUntilBothPointsArePicked() {
        assertNull(MeasureReadoutLogic.fromPoints(null, null))
        assertNull(MeasureReadoutLogic.fromPoints(Vec3d(1.0, 2.0, 3.0), null))
        assertNull(MeasureReadoutLogic.fromPoints(null, Vec3d(4.0, 6.0, 3.0)))
    }

    @Test
    fun readoutUsesEuclideanDistanceAndAbsoluteAxisDeltas() {
        val pointA = Vec3d(1.0, 2.0, 3.0)
        val pointB = Vec3d(4.0, 6.0, -9.0)

        val forward = MeasureReadoutLogic.fromPoints(pointA, pointB)!!
        val reverse = MeasureReadoutLogic.fromPoints(pointB, pointA)!!

        assertEquals(13.0, forward.distance, 0.000001)
        assertEquals(3.0, forward.deltaX, 0.000001)
        assertEquals(4.0, forward.deltaY, 0.000001)
        assertEquals(12.0, forward.deltaZ, 0.000001)
        assertEquals(forward, reverse)
    }
}
