package ru.ytkab0bp.slicebeam.render

import ru.ytkab0bp.slicebeam.utils.Vec3d

/** Intersects a camera ray with a horizontal world-space plane. */
internal object HorizontalPlaneRay {
    fun intersect(origin: Vec3d, direction: Vec3d, planeZ: Double): Vec3d? {
        if (!origin.x.isFinite() || !origin.y.isFinite() || !origin.z.isFinite() ||
            !direction.x.isFinite() || !direction.y.isFinite() || !direction.z.isFinite() ||
            !planeZ.isFinite() || kotlin.math.abs(direction.z) < 1e-9
        ) return null

        val distance = (planeZ - origin.z) / direction.z
        if (!distance.isFinite() || distance < 0.0) return null
        return Vec3d(
            origin.x + direction.x * distance,
            origin.y + direction.y * distance,
            planeZ,
        ).takeIf { it.x.isFinite() && it.y.isFinite() }
    }
}
