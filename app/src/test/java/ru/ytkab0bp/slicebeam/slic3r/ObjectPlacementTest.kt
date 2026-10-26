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

    @Test fun tiltedSurfacePlacementKeepsEveryGroupBoundsCornerAboveThePlane() {
        val moving = ObjectBounds3D(-4.0, 2.0, 3.0, 8.0, 12.0, 15.0)
        val point = Vec3d(20.0, -5.0, 31.5)
        val normal = Vec3d(0.6, 0.0, 0.8)
        val delta = requireNotNull(ObjectPlacement.onSurface(moving, point, normal))

        val length = kotlin.math.sqrt(normal.x * normal.x + normal.y * normal.y + normal.z * normal.z)
        val minSignedDistance = listOf(moving.minX, moving.maxX).minOf { x ->
            listOf(moving.minY, moving.maxY).minOf { y ->
                listOf(moving.minZ, moving.maxZ).minOf { z ->
                    normal.x / length * (x + delta.x - point.x) +
                        normal.y / length * (y + delta.y - point.y) +
                        normal.z / length * (z + delta.z - point.z)
                }
            }
        }
        assertEquals(0.0, minSignedDistance, 1e-9)
        val movedCenter = Vec3d(
            (moving.minX + moving.maxX) / 2.0 + delta.x,
            (moving.minY + moving.maxY) / 2.0 + delta.y,
            (moving.minZ + moving.maxZ) / 2.0 + delta.z,
        )
        val projectedCenter = (movedCenter.x - point.x) * normal.x / length +
            (movedCenter.y - point.y) * normal.y / length +
            (movedCenter.z - point.z) * normal.z / length
        assertEquals("the group center should stay over the picked point", 0.0, projectedCenter - 8.4, 1e-9)
    }

    @Test fun sideSurfacePlacementKeepsTheWholeGroupOutsideThePickedPlane() {
        val moving = ObjectBounds3D(0.0, 0.0, 0.0, 2.0, 2.0, 1.0)
        val point = Vec3d(10.0, 10.0, 5.0)
        val unitNormal = requireNotNull(ObjectPlacement.onSurface(moving, point, Vec3d(1.0, 0.0, 0.0)))
        val scaledNormal = requireNotNull(ObjectPlacement.onSurface(moving, point, Vec3d(10.0, 0.0, 0.0)))
        assertEquals(unitNormal, scaledNormal)
        assertEquals(10.0, unitNormal.x, 0.0)
        assertEquals(9.0, unitNormal.y, 0.0)
        assertEquals(4.5, unitNormal.z, 0.0)
        assertNull("downward-facing surfaces would place the group beneath an overhang",
            ObjectPlacement.onSurface(moving, point, Vec3d(0.0, 0.0, -1.0)))
        assertNull(ObjectPlacement.onSurface(moving, point, Vec3d(Double.NaN, 0.0, 1.0)))
        assertNull(ObjectPlacement.onSurface(moving, point, Vec3d(0.0, 0.0, 0.0)))
    }

    @Test fun surfaceBooleanPlacementPenetratesAlongTheNormalizedSurfaceNormal() {
        val bounds = ObjectBounds3D(10.0, 20.0, 30.0, 14.0, 24.0, 34.0)
        val delta = requireNotNull(ObjectPlacement.forSurfaceBoolean(
            bounds, Vec3d(5.0, 6.0, 7.0), Vec3d(0.0, 0.0, 4.0), penetrationMm = 0.8,
        ))

        assertEquals(-7.0, delta.x, 1e-9)
        assertEquals(-16.0, delta.y, 1e-9)
        // Surface contact would move min Z to 7; Boolean placement overlaps it by 0.8 mm.
        assertEquals(-23.8, delta.z, 1e-9)
    }

    @Test fun surfaceBooleanRejectsInvalidPenetrationAndUnsupportedDownwardFace() {
        val bounds = ObjectBounds3D(0.0, 0.0, 0.0, 2.0, 2.0, 2.0)
        assertNull(ObjectPlacement.forSurfaceBoolean(bounds, Vec3d(), Vec3d(0.0, 0.0, 1.0), 0.0))
        assertNull(ObjectPlacement.forSurfaceBoolean(bounds, Vec3d(), Vec3d(0.0, 0.0, 1.0), 6.0))
        assertNull(ObjectPlacement.forSurfaceBoolean(bounds, Vec3d(), Vec3d(0.0, 0.0, -1.0), 0.8))
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

    @Test fun placementRejectsNonFiniteDeltasAndUsesStableMidpoints() {
        val huge = 1e308
        val farTarget = ObjectBounds3D(huge, huge, 0.0, huge, huge, 1.0)
        val origin = ObjectBounds3D(0.0, 0.0, 0.0, 0.0, 0.0, 1.0)
        val centered = requireNotNull(ObjectPlacement.onTop(origin, farTarget))
        assertEquals("finite target centers must not overflow during midpoint calculation", huge, centered.x, 0.0)
        assertEquals(huge, centered.y, 0.0)

        assertNull("unrepresentable lateral placement is rejected",
            ObjectPlacement.onSurface(
                ObjectBounds3D(-huge, 0.0, 0.0, -huge, 0.0, 1.0),
                Vec3d(huge, 0.0, 0.0),
            ))
        assertNull("unrepresentable vertical bed placement is rejected",
            ObjectPlacement.ontoBed(origin.copy(minZ = -huge, maxZ = 0.0), huge))
    }
}
