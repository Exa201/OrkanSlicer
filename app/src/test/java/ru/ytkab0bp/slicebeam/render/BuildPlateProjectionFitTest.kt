package ru.ytkab0bp.slicebeam.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildPlateProjectionFitTest {
    @Test
    fun containsPlateAcrossShortPortraitSquareAndLandscapeViewports() {
        val projectedHalfWidth = 240.0
        val projectedHalfHeight = 310.0

        listOf(0.38, 0.55, 0.72, 1.0, 1.45, 2.1).forEach { aspect ->
            val (halfWidth, halfHeight) = BuildPlateProjectionFit.halfExtents(
                projectedHalfWidth, projectedHalfHeight, aspect, zoom = 1.0,
            )

            assertTrue("plate width must fit at aspect=$aspect", halfWidth >= projectedHalfWidth)
            assertTrue("plate height must fit at aspect=$aspect", halfHeight >= projectedHalfHeight)
            assertEquals("orthographic extents must preserve aspect at $aspect", aspect, halfWidth / halfHeight, 1e-9)
        }
    }

    @Test
    fun zoomIsAppliedAfterViewportFit() {
        val fitted = BuildPlateProjectionFit.halfExtents(240.0, 310.0, aspectRatio = 0.45, zoom = 1.0)
        val zoomed = BuildPlateProjectionFit.halfExtents(240.0, 310.0, aspectRatio = 0.45, zoom = 2.0)

        assertEquals(fitted.first / 2.0, zoomed.first, 1e-9)
        assertEquals(fitted.second / 2.0, zoomed.second, 1e-9)
    }
}
