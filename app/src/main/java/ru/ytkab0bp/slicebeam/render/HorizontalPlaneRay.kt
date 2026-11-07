package ru.ytkab0bp.slicebeam.render

import ru.ytkab0bp.slicebeam.utils.Vec3d

/** Intersects a camera ray with a horizontal world-space plane. */
internal object HorizontalPlaneRay {
    // Near-horizontal camera rays produce enormous and very sensitive XY intersections.
    // The drag handler falls back to a camera-facing plane in this range.
    private const val MIN_ABS_VERTICAL_DIRECTION = 0.08

    fun intersect(origin: Vec3d, direction: Vec3d, planeZ: Double): Vec3d? {
        if (!origin.x.isFinite() || !origin.y.isFinite() || !origin.z.isFinite() ||
            !direction.x.isFinite() || !direction.y.isFinite() || !direction.z.isFinite() ||
            !planeZ.isFinite() || kotlin.math.abs(direction.z) < MIN_ABS_VERTICAL_DIRECTION
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

/** Converts pointer motion on the grabbed plane directly into the model's XY translation. */
internal object HorizontalPlaneDrag {
    fun translation(start: Vec3d, current: Vec3d): Vec3d? {
        if (!start.x.isFinite() || !start.y.isFinite() ||
            !current.x.isFinite() || !current.y.isFinite()
        ) return null
        return Vec3d(current.x - start.x, current.y - start.y, 0.0)
    }

    /** Uses the pointer's ACTION_UP location, falling back to the last rendered preview if invalid. */
    fun finalTranslation(start: Vec3d, releasePoint: Vec3d?, lastValid: Vec3d): Vec3d {
        val releaseDelta = releasePoint?.let { translation(start, it) }
        if (releaseDelta != null) return releaseDelta
        return if (lastValid.x.isFinite() && lastValid.y.isFinite()) {
            Vec3d(lastValid.x, lastValid.y, 0.0)
        } else {
            Vec3d()
        }
    }
}
