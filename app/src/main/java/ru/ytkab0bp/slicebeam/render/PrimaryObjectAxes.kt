package ru.ytkab0bp.slicebeam.render

import ru.ytkab0bp.slicebeam.utils.Vec3d
import kotlin.math.cos
import kotlin.math.sin

/** Orthonormal world-space axes for the selected object's stored X/Y/Z Euler rotation. */
internal object PrimaryObjectAxes {
    fun fromEulerXYZ(rx: Double, ry: Double, rz: Double): List<Vec3d>? {
        if (!rx.isFinite() || !ry.isFinite() || !rz.isFinite()) return null
        val sx = sin(rx)
        val cx = cos(rx)
        val sy = sin(ry)
        val cy = cos(ry)
        val sz = sin(rz)
        val cz = cos(rz)

        // Native ModelVolume transforms use Rz * Ry * Rx.
        val x = Vec3d(cz * cy, sz * cy, -sy)
        val y = Vec3d(cz * sy * sx - sz * cx, sz * sy * sx + cz * cx, cy * sx)
        val z = Vec3d(cz * sy * cx + sz * sx, sz * sy * cx - cz * sx, cy * cx)
        return listOf(x, y, z)
    }
}
