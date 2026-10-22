package ru.ytkab0bp.slicebeam.slic3r

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.ytkab0bp.slicebeam.utils.Vec3d

class ObjectPlacementTest {
    @Test fun centersMovingObjectOnTargetAndMakesFacesTouch() {
        val delta = ObjectPlacement.onTop(
            moving = ObjectBounds3D(-2.0, -1.0, 2.0, 2.0, 1.0, 8.0),
            target = ObjectBounds3D(10.0, 20.0, 0.0, 18.0, 26.0, 4.0),
        )
        requireNotNull(delta)
        assertEquals(14.0, delta.x, 0.0)
        assertEquals(23.0, delta.y, 0.0)
        assertEquals(2.0, delta.z, 0.0)
    }

    @Test fun clearanceIsAddedAboveTargetSurface() {
        val delta = requireNotNull(ObjectPlacement.onTop(
            ObjectBounds3D(0.0, 0.0, 0.0, 1.0, 1.0, 1.0),
            ObjectBounds3D(2.0, 2.0, 0.0, 4.0, 4.0, 5.0),
            clearanceMm = 0.25,
        ))
        assertEquals(2.5, delta.x, 0.0)
        assertEquals(2.5, delta.y, 0.0)
        assertEquals(5.25, delta.z, 0.0)
    }

    @Test fun pickedSurfacePointCentersAndSeatsMovingGroupAtThatPoint() {
        val delta = requireNotNull(ObjectPlacement.onSurface(
            ObjectBounds3D(-4.0, 2.0, 3.0, 8.0, 12.0, 15.0),
            Vec3d(20.0, -5.0, 31.5),
        ))
        assertEquals(18.0, delta.x, 0.0)
        assertEquals(-12.0, delta.y, 0.0)
        assertEquals(28.5, delta.z, 0.0)
    }

    @Test fun surfacePlacementRejectsInvalidPointAndBounds() {
        val valid = ObjectBounds3D(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
        assertNull(ObjectPlacement.onSurface(valid, Vec3d(Double.NaN, 0.0, 0.0)))
        assertNull(ObjectPlacement.onSurface(valid.copy(maxZ = -1.0), Vec3d(0.0, 0.0, 0.0)))
    }

    @Test fun groupBoundsAllowSeveralSelectedObjectsToBePlacedTogether() {
        val group = requireNotNull(ObjectPlacement.enclosing(listOf(
            ObjectBounds3D(-8.0, 2.0, 1.0, -2.0, 8.0, 4.0),
            ObjectBounds3D(3.0, -4.0, 0.0, 9.0, 5.0, 6.0),
        )))
        assertEquals(-8.0, group.minX, 0.0)
        assertEquals(-4.0, group.minY, 0.0)
        assertEquals(0.0, group.minZ, 0.0)
        assertEquals(9.0, group.maxX, 0.0)
        assertEquals(8.0, group.maxY, 0.0)
        assertEquals(6.0, group.maxZ, 0.0)

        val delta = requireNotNull(ObjectPlacement.onTop(group, ObjectBounds3D(10.0, 20.0, 0.0, 18.0, 26.0, 4.0)))
        assertEquals(13.5, delta.x, 0.0)
        assertEquals(21.0, delta.y, 0.0)
        assertEquals(4.0, delta.z, 0.0)
    }

    @Test fun droppingGroupToBedUsesOneVerticalDeltaAndPreservesRelativeHeights() {
        val group = ObjectBounds3D(-5.0, 2.0, 12.0, 18.0, 22.0, 41.0)
        val delta = requireNotNull(ObjectPlacement.ontoBed(group, bedZ = 0.0))
        assertEquals(0.0, delta.x, 0.0)
        assertEquals(0.0, delta.y, 0.0)
        assertEquals(-12.0, delta.z, 0.0)
        assertEquals(0.0, group.minZ + delta.z, 0.0)
        assertEquals(29.0, group.maxZ + delta.z, 0.0)
    }

    @Test fun enclosingRejectsEmptyOrInvalidObjectSets() {
        assertNull(ObjectPlacement.enclosing(emptyList()))
        assertNull(ObjectPlacement.enclosing(listOf(ObjectBounds3D(0.0, 0.0, 0.0, Double.NaN, 1.0, 1.0))))
    }

    @Test fun rejectsInvalidBoundsAndNegativeClearance() {
        val valid = ObjectBounds3D(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
        assertNull(ObjectPlacement.onTop(valid.copy(maxZ = -1.0), valid))
        assertNull(ObjectPlacement.onTop(valid.copy(maxX = Double.NaN), valid))
        assertNull(ObjectPlacement.onTop(valid, valid, -0.1))
        assertNull(ObjectPlacement.ontoBed(valid.copy(minZ = Double.NaN), 0.0))
    }
}
