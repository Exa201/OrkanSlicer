package ru.ytkab0bp.slicebeam.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.ytkab0bp.slicebeam.utils.Vec3d

class HorizontalPlaneRayTest {
    @Test
    fun dragTranslationUsesPointerAnchorInsteadOfMeshOrObjectOrigin() {
        val grabbedPoint = Vec3d(124.5, 63.0, 18.0)
        val movedPointer = Vec3d(141.0, 54.5, 18.0)

        val delta = requireNotNull(HorizontalPlaneDrag.translation(grabbedPoint, movedPointer))

        assertEquals(16.5, delta.x, 1e-9)
        assertEquals(-8.5, delta.y, 1e-9)
        assertEquals(0.0, delta.z, 1e-9)
    }

    @Test
    fun dragTranslationRejectsNonFinitePointerCoordinates() {
        assertNull(HorizontalPlaneDrag.translation(Vec3d(), Vec3d(Double.NaN, 1.0, 0.0)))
    }

    @Test
    fun dragCommitUsesTheFinalReleasePointInsteadOfTheLastMovePoint() {
        val grabbedPoint = Vec3d(10.0, 20.0, 6.0)
        val lastMovePoint = Vec3d(24.0, 31.0, 6.0)
        val releasePoint = Vec3d(28.0, 35.0, 6.0)
        val lastPreview = requireNotNull(HorizontalPlaneDrag.translation(grabbedPoint, lastMovePoint))

        val committed = HorizontalPlaneDrag.finalTranslation(grabbedPoint, releasePoint, lastPreview)

        assertEquals(18.0, committed.x, 1e-9)
        assertEquals(15.0, committed.y, 1e-9)
        assertEquals(0.0, committed.z, 1e-9)
    }

    @Test
    fun dragCommitKeepsLastValidPreviewWhenReleaseRayCannotBeResolved() {
        val lastPreview = Vec3d(14.0, -9.0, 0.0)

        val committed = HorizontalPlaneDrag.finalTranslation(Vec3d(3.0, 4.0, 0.0), null, lastPreview)

        assertEquals(14.0, committed.x, 1e-9)
        assertEquals(-9.0, committed.y, 1e-9)
        assertEquals(0.0, committed.z, 1e-9)
    }

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

    @Test
    fun rejectsNearParallelDragRaySoCallerCanUseStableFallbackPlane() {
        val unstable = HorizontalPlaneRay.intersect(
            origin = Vec3d(0.0, 0.0, 100.0),
            direction = Vec3d(0.99995, 0.0, -0.01),
            planeZ = 0.0,
        )

        assertNull(unstable)
    }
}
