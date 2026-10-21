package ru.ytkab0bp.slicebeam.slic3r

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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

    @Test fun rejectsInvalidBoundsAndNegativeClearance() {
        val valid = ObjectBounds3D(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
        assertNull(ObjectPlacement.onTop(valid.copy(maxZ = -1.0), valid))
        assertNull(ObjectPlacement.onTop(valid.copy(maxX = Double.NaN), valid))
        assertNull(ObjectPlacement.onTop(valid, valid, -0.1))
    }
}
