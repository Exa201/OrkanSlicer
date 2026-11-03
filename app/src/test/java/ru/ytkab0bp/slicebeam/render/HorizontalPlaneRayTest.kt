package ru.ytkab0bp.slicebeam.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.ytkab0bp.slicebeam.utils.Vec3d

class HorizontalPlaneRayTest {
    @Test
    fun intersectsThePickedSurfacePlaneRatherThanTheBedPlane() {
        val origin = Vec3d(0.0, 0.0, 100.0)
        val direction = Vec3d(1.0, 2.0, -4.0)

        val hit = requireNotNull(HorizontalPlaneRay.intersect(origin, direction, planeZ = 40.0))

        assertEquals(15.0, hit.x, 1e-9)
        assertEquals(30.0, hit.y, 1e-9)
        assertEquals(40.0, hit.z, 1e-9)
        val bedHit = requireNotNull(HorizontalPlaneRay.intersect(origin, direction, planeZ = 0.0))
        // At an oblique camera angle the bed-plane projection differs from the grabbed
        // point's height plane, which caused the visible mesh to drift from the pointer.
        assertEquals(25.0, bedHit.x, 1e-9)
        assertEquals(50.0, bedHit.y, 1e-9)
    }

    @Test
    fun ignoresRaysThatCannotReachTheRequestedPlane() {
        assertNull(HorizontalPlaneRay.intersect(Vec3d(0.0, 0.0, 10.0), Vec3d(1.0, 0.0, 0.0), 0.0))
        assertNull(HorizontalPlaneRay.intersect(Vec3d(0.0, 0.0, 10.0), Vec3d(1.0, 0.0, 1.0), 0.0))
    }
}
