package ru.ytkab0bp.slicebeam.render

/** Uniformly shrinks an oversized object to fit a rectangular build volume. */
internal object BuildVolumeFitPolicy {
    data class FitPlan(
        val uniformScale: Double,
        val targetCenterX: Double,
        val targetCenterY: Double,
        val targetMinZ: Double,
    )

    fun uniformScale(
        objectSizeX: Double,
        objectSizeY: Double,
        objectSizeZ: Double,
        volumeSizeX: Double,
        volumeSizeY: Double,
        volumeSizeZ: Double,
        clearanceMm: Double = 0.02,
    ): Double? {
        val dimensions = listOf(objectSizeX, objectSizeY, objectSizeZ)
        val available = listOf(volumeSizeX, volumeSizeY, volumeSizeZ)
        if (dimensions.any { !it.isFinite() || it <= 0.0 } ||
            available.any { !it.isFinite() || it <= clearanceMm } ||
            !clearanceMm.isFinite() || clearanceMm < 0.0) return null

        val fit = minOf(1.0, available.indices.minOf { axis ->
            (available[axis] - clearanceMm) / dimensions[axis]
        })
        return fit.takeIf { it.isFinite() && it > 0.0 }
    }

    /** Conservatively fits an object's XY bounding-box circumcircle inside a round bed. */
    fun uniformCircleScale(
        objectSizeX: Double,
        objectSizeY: Double,
        objectSizeZ: Double,
        bedRadius: Double,
        volumeSizeZ: Double,
        clearanceMm: Double = 0.02,
    ): Double? {
        val dimensions = listOf(objectSizeX, objectSizeY, objectSizeZ)
        if (dimensions.any { !it.isFinite() || it <= 0.0 } ||
            !bedRadius.isFinite() || bedRadius <= clearanceMm ||
            !volumeSizeZ.isFinite() || volumeSizeZ <= clearanceMm ||
            !clearanceMm.isFinite() || clearanceMm < 0.0) return null

        val footprintRadius = Math.hypot(objectSizeX, objectSizeY) / 2.0
        if (!footprintRadius.isFinite() || footprintRadius <= 0.0) return null
        val fit = minOf(
            1.0,
            (bedRadius - clearanceMm) / footprintRadius,
            (volumeSizeZ - clearanceMm) / objectSizeZ,
        )
        return fit.takeIf { it.isFinite() && it > 0.0 }
    }

    /** Computes the renderer's uniform scale and final bed-aligned target for a circular plate. */
    fun circularFitPlan(
        objectMinX: Double,
        objectMinY: Double,
        objectMinZ: Double,
        objectMaxX: Double,
        objectMaxY: Double,
        objectMaxZ: Double,
        bedCenterX: Double,
        bedCenterY: Double,
        bedRadius: Double,
        bedMinZ: Double,
        bedMaxZ: Double,
        clearanceMm: Double = 0.02,
    ): FitPlan? {
        val values = listOf(
            objectMinX, objectMinY, objectMinZ, objectMaxX, objectMaxY, objectMaxZ,
            bedCenterX, bedCenterY, bedMinZ, bedMaxZ,
        )
        if (values.any { !it.isFinite() }) return null
        val sizeX = objectMaxX - objectMinX
        val sizeY = objectMaxY - objectMinY
        val sizeZ = objectMaxZ - objectMinZ
        val scale = uniformCircleScale(sizeX, sizeY, sizeZ, bedRadius, bedMaxZ - bedMinZ, clearanceMm)
            ?: return null
        return FitPlan(scale, bedCenterX, bedCenterY, bedMinZ)
    }
}
