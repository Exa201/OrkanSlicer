package ru.ytkab0bp.slicebeam.slic3r

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObjectGroupDistributionTest {
    @Test
    fun distributesVariableWidthObjectsWithEqualGapsAndPreservesEndpointsOnEveryAxis() {
        val bounds = mapOf(
            7 to ObjectBounds3D(0.0, 0.0, 0.0, 10.0, 10.0, 10.0),
            2 to ObjectBounds3D(20.0, 20.0, 20.0, 40.0, 40.0, 40.0),
            9 to ObjectBounds3D(70.0, 70.0, 70.0, 100.0, 100.0, 100.0),
        )
        for (axis in ObjectDistributionAxis.entries) {
            val deltas = requireNotNull(ObjectGroupDistribution.plan(bounds, axis))
            val first = bounds.getValue(7)
            val middle = bounds.getValue(2)
            val last = bounds.getValue(9)
            assertEquals(0.0, deltas.getValue(7).component(axis), 0.0)
            assertEquals(0.0, deltas.getValue(9).component(axis), 0.0)
            val firstGap = middle.start(axis) + deltas.getValue(2).component(axis) - first.end(axis)
            val secondGap = last.start(axis) - (middle.end(axis) + deltas.getValue(2).component(axis))
            assertEquals("distribution on $axis must produce equal clear gaps", firstGap, secondGap, 1e-9)
            assertEquals(20.0, firstGap, 1e-9)
            val delta = deltas.getValue(2)
            when (axis) {
                ObjectDistributionAxis.X -> { assertEquals(0.0, delta.y, 0.0); assertEquals(0.0, delta.z, 0.0) }
                ObjectDistributionAxis.Y -> { assertEquals(0.0, delta.x, 0.0); assertEquals(0.0, delta.z, 0.0) }
                ObjectDistributionAxis.Z -> { assertEquals(0.0, delta.x, 0.0); assertEquals(0.0, delta.y, 0.0) }
            }
        }
    }

    @Test
    fun rejectsFewerThanThreeObjectsInvalidBoundsAndInsufficientSpan() {
        val a = ObjectBounds3D(0.0, 0.0, 0.0, 10.0, 10.0, 10.0)
        val b = ObjectBounds3D(20.0, 20.0, 20.0, 30.0, 30.0, 30.0)
        assertNull(ObjectGroupDistribution.plan(mapOf(0 to a, 1 to b), ObjectDistributionAxis.X))
        assertNull(ObjectGroupDistribution.plan(mapOf(
            0 to a, 1 to b, 2 to b.copy(maxX = Double.NaN),
        ), ObjectDistributionAxis.X))
        assertNull(ObjectGroupDistribution.plan(mapOf(
            0 to a,
            1 to ObjectBounds3D(1.0, 0.0, 0.0, 10.0, 10.0, 10.0),
            2 to ObjectBounds3D(2.0, 0.0, 0.0, 10.0, 10.0, 10.0),
        ), ObjectDistributionAxis.X))
    }

    private fun ObjectBounds3D.start(axis: ObjectDistributionAxis) = when (axis) {
        ObjectDistributionAxis.X -> minX
        ObjectDistributionAxis.Y -> minY
        ObjectDistributionAxis.Z -> minZ
    }

    private fun ObjectBounds3D.end(axis: ObjectDistributionAxis) = when (axis) {
        ObjectDistributionAxis.X -> maxX
        ObjectDistributionAxis.Y -> maxY
        ObjectDistributionAxis.Z -> maxZ
    }

    private fun ObjectPlacementDelta.component(axis: ObjectDistributionAxis) = when (axis) {
        ObjectDistributionAxis.X -> x
        ObjectDistributionAxis.Y -> y
        ObjectDistributionAxis.Z -> z
    }
}
