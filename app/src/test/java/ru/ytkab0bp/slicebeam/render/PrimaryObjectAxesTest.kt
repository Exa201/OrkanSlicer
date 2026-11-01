package ru.ytkab0bp.slicebeam.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.math.PI

class PrimaryObjectAxesTest {
    @Test
    fun zeroRotationProducesWorldAxes() {
        val axes = requireNotNull(PrimaryObjectAxes.fromEulerXYZ(0.0, 0.0, 0.0))
        assertEquals(1.0, axes[0].x, 1e-12)
        assertEquals(1.0, axes[1].y, 1e-12)
        assertEquals(1.0, axes[2].z, 1e-12)
        assertOrthonormal(axes)
    }

    @Test
    fun quarterTurnAroundZRotatesLocalXAndYInWorldSpace() {
        val axes = requireNotNull(PrimaryObjectAxes.fromEulerXYZ(0.0, 0.0, PI / 2.0))
        assertEquals(0.0, axes[0].x, 1e-12)
        assertEquals(1.0, axes[0].y, 1e-12)
        assertEquals(-1.0, axes[1].x, 1e-12)
        assertEquals(0.0, axes[1].y, 1e-12)
        assertEquals(1.0, axes[2].z, 1e-12)
        assertOrthonormal(axes)
    }

    @Test
    fun rejectsNonFiniteRotations() {
        assertNull(PrimaryObjectAxes.fromEulerXYZ(Double.NaN, 0.0, 0.0))
        assertNull(PrimaryObjectAxes.fromEulerXYZ(0.0, Double.POSITIVE_INFINITY, 0.0))
        assertNotNull(PrimaryObjectAxes.fromEulerXYZ(0.0, 0.0, 0.0))
    }

    private fun assertOrthonormal(axes: List<ru.ytkab0bp.slicebeam.utils.Vec3d>) {
        axes.forEach { axis -> assertEquals(1.0, dot(axis, axis), 1e-12) }
        assertEquals(0.0, dot(axes[0], axes[1]), 1e-12)
        assertEquals(0.0, dot(axes[0], axes[2]), 1e-12)
        assertEquals(0.0, dot(axes[1], axes[2]), 1e-12)
    }

    private fun dot(a: ru.ytkab0bp.slicebeam.utils.Vec3d, b: ru.ytkab0bp.slicebeam.utils.Vec3d) =
        a.x * b.x + a.y * b.y + a.z * b.z
}
