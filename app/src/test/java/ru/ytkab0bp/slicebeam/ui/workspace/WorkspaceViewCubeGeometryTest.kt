package ru.ytkab0bp.slicebeam.ui.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceViewCubeGeometryTest {
    @Test
    fun defaultCubeShowsThreeClickableFacesInsideItsTouchBounds() {
        val polygons = WorkspaceViewCubeGeometry.project(52f, 52f, Math.toRadians(-35.0).toFloat(), Math.toRadians(-25.0).toFloat())
        assertEquals(3, polygons.size)
        polygons.forEach { polygon ->
            val x = polygon.points.map { it.x }.average().toFloat()
            val y = polygon.points.map { it.y }.average().toFloat()
            assertTrue(x in 0f..52f)
            assertTrue(y in 0f..52f)
            assertEquals(polygon.face, WorkspaceViewCubeGeometry.hitTest(polygons, x, y))
        }
    }

    @Test
    fun eachStandardOrientationShowsItsRequestedFace() {
        WorkspaceViewFace.values().forEach { face ->
            val orientation = WorkspaceViewCubeGeometry.orientationFor(face)
            val polygons = WorkspaceViewCubeGeometry.project(52f, 52f, orientation.first, orientation.second)
            assertNotNull("$face should be visible in its standard orientation", polygons.firstOrNull { it.face == face })
        }
    }

    @Test
    fun cameraDirectionRemainsNormalizedAfterCubeRotation() {
        val direction = WorkspaceViewCubeGeometry.cameraDirection(0.8f, -0.6f)
        val length = kotlin.math.sqrt(direction.first * direction.first + direction.second * direction.second + direction.third * direction.third)
        assertEquals(1.0, length, 1e-6)
    }
}
