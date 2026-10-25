package ru.ytkab0bp.slicebeam.slic3r

import ru.ytkab0bp.slicebeam.utils.Vec3d
import kotlin.math.abs

/** Cheap selection for dense models; never changes the geometry used for slicing. */
internal object LargeModelPick {
    // Surface paint creates topology data in addition to its raycast tree. Keep that heavier path
    // bounded even though GL picking itself now uses a lightweight triangle AABB tree.
    const val TRIANGLE_LIMIT = 20_000
    // A balanced raycast tree uses bounded memory and supports precise picks for common generated
    // parts such as M12 fasteners. Truly massive meshes still use bounds-only selection.
    const val EXACT_RAYCAST_TRIANGLE_LIMIT = 100_000
    // Keep decimated meshes below the hard raycast cap to allow for simplifier variance.
    const val SAFE_RAYCAST_TRIANGLE_TARGET = 80_000

    fun needsPreciseSurfaceSimplification(triangleCount: Int): Boolean = triangleCount > TRIANGLE_LIMIT

    fun needsBoundsOnlyPicking(triangleCount: Int): Boolean = triangleCount > EXACT_RAYCAST_TRIANGLE_LIMIT

    /** Surface placement uses exact triangle hits while the lightweight raycast tree stays bounded. */
    fun canUsePreciseSurfaceTools(triangleCount: Int): Boolean =
        !needsBoundsOnlyPicking(triangleCount)

    /** Leave headroom below the requested cap because decimators may return more facets. */
    fun suggestedKeepRatio(triangleCount: Int, targetTriangleCount: Int = TRIANGLE_LIMIT): Float {
        if (triangleCount <= targetTriangleCount || triangleCount <= 0 || targetTriangleCount <= 0) return 1f
        val safeTarget = targetTriangleCount * 0.8f
        return (safeTarget / triangleCount).coerceIn(0.000001f, 0.99f)
    }

    fun hit(origin: Vec3d, direction: Vec3d, min: Vec3d, max: Vec3d): GLModel.HitResult? {
        val starts = doubleArrayOf(origin.x, origin.y, origin.z)
        val rays = doubleArrayOf(direction.x, direction.y, direction.z)
        val lows = doubleArrayOf(min.x, min.y, min.z)
        val highs = doubleArrayOf(max.x, max.y, max.z)
        if (!starts.all(Double::isFinite) || !rays.all(Double::isFinite) ||
            !lows.all(Double::isFinite) || !highs.all(Double::isFinite)) return null

        var near = Double.NEGATIVE_INFINITY
        var far = Double.POSITIVE_INFINITY
        var nearAxis = -1
        var farAxis = -1
        var nearSign = 0.0
        var farSign = 0.0
        for (axis in 0..2) {
            // The approximate model bounds can be flat (for example, a lithophane).
            val low = lows[axis] - 0.5
            val high = highs[axis] + 0.5
            if (low > high) return null
            val ray = rays[axis]
            if (abs(ray) < 1e-12) {
                if (starts[axis] < low || starts[axis] > high) return null
                continue
            }
            val entry = (if (ray > 0) low else high) - starts[axis]
            val exit = (if (ray > 0) high else low) - starts[axis]
            val tNear = entry / ray
            val tFar = exit / ray
            if (tNear > near) {
                near = tNear
                nearAxis = axis
                nearSign = if (ray > 0) -1.0 else 1.0
            }
            if (tFar < far) {
                far = tFar
                farAxis = axis
                farSign = if (ray > 0) 1.0 else -1.0
            }
            if (near > far) return null
        }
        val distance = if (near >= 0) near else far
        val axis = if (near >= 0) nearAxis else farAxis
        val sign = if (near >= 0) nearSign else farSign
        if (!distance.isFinite() || distance < 0 || axis < 0) return null
        val normal = Vec3d()
        when (axis) {
            0 -> normal.x = sign
            1 -> normal.y = sign
            else -> normal.z = sign
        }
        return GLModel.HitResult(
            Vec3d(origin.x + direction.x * distance, origin.y + direction.y * distance, origin.z + direction.z * distance),
            normal,
            approximate = true,
        )
    }
}
