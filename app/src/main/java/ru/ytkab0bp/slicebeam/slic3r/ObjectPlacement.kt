package ru.ytkab0bp.slicebeam.slic3r

import kotlin.math.abs

internal data class ObjectBounds3D(
    val minX: Double, val minY: Double, val minZ: Double,
    val maxX: Double, val maxY: Double, val maxZ: Double,
)

internal data class ObjectPlacementDelta(val x: Double, val y: Double, val z: Double)

/** Computes a stable world-space placement that centers one object's footprint atop another. */
internal object ObjectPlacement {
    fun onTop(moving: ObjectBounds3D, target: ObjectBounds3D, clearanceMm: Double = 0.0): ObjectPlacementDelta? {
        val values = listOf(moving.minX, moving.minY, moving.minZ, moving.maxX, moving.maxY, moving.maxZ,
            target.minX, target.minY, target.minZ, target.maxX, target.maxY, target.maxZ, clearanceMm)
        if (values.any { !it.isFinite() } || clearanceMm < 0.0) return null
        if (moving.maxX < moving.minX || moving.maxY < moving.minY || moving.maxZ < moving.minZ ||
            target.maxX < target.minX || target.maxY < target.minY || target.maxZ < target.minZ) return null
        return ObjectPlacementDelta(
            x = (target.minX + target.maxX - moving.minX - moving.maxX) / 2.0,
            y = (target.minY + target.maxY - moving.minY - moving.maxY) / 2.0,
            z = target.maxZ + clearanceMm - moving.minZ,
        )
    }
}
