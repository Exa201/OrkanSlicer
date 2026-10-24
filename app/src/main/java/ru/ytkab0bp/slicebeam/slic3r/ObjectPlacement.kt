package ru.ytkab0bp.slicebeam.slic3r

import ru.ytkab0bp.slicebeam.utils.Vec3d

internal data class ObjectBounds3D(
    val minX: Double, val minY: Double, val minZ: Double,
    val maxX: Double, val maxY: Double, val maxZ: Double,
)

internal data class ObjectPlacementDelta(val x: Double, val y: Double, val z: Double)

/** Computes a stable world-space placement that centers one object's footprint atop another. */
internal object ObjectPlacement {
    /** Returns the world-space bounds of a set of objects, or null for invalid/empty input. */
    fun enclosing(bounds: Iterable<ObjectBounds3D>): ObjectBounds3D? {
        val items = bounds.toList()
        if (items.isEmpty()) return null
        if (items.any {
                listOf(it.minX, it.minY, it.minZ, it.maxX, it.maxY, it.maxZ).any { value -> !value.isFinite() } ||
                    it.maxX < it.minX || it.maxY < it.minY || it.maxZ < it.minZ
            }) return null
        return ObjectBounds3D(
            minX = items.minOf { it.minX }, minY = items.minOf { it.minY }, minZ = items.minOf { it.minZ },
            maxX = items.maxOf { it.maxX }, maxY = items.maxOf { it.maxY }, maxZ = items.maxOf { it.maxZ },
        )
    }

    fun onTop(moving: ObjectBounds3D, target: ObjectBounds3D, clearanceMm: Double = 0.0): ObjectPlacementDelta? {
        val values = listOf(moving.minX, moving.minY, moving.minZ, moving.maxX, moving.maxY, moving.maxZ,
            target.minX, target.minY, target.minZ, target.maxX, target.maxY, target.maxZ, clearanceMm)
        if (values.any { !it.isFinite() } || clearanceMm < 0.0) return null
        if (moving.maxX < moving.minX || moving.maxY < moving.minY || moving.maxZ < moving.minZ ||
            target.maxX < target.minX || target.maxY < target.minY || target.maxZ < target.minZ) return null
        val delta = ObjectPlacementDelta(
            x = midpoint(target.minX, target.maxX) - midpoint(moving.minX, moving.maxX),
            y = midpoint(target.minY, target.maxY) - midpoint(moving.minY, moving.maxY),
            z = target.maxZ + clearanceMm - moving.minZ,
        )
        return delta.takeIf { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() }
    }

    /** Centers a moving assembly on a picked point and keeps its bounds above the tapped plane. */
    fun onSurface(
        moving: ObjectBounds3D,
        point: Vec3d,
        surfaceNormal: Vec3d = Vec3d(0.0, 0.0, 1.0),
    ): ObjectPlacementDelta? {
        val values = listOf(
            moving.minX, moving.minY, moving.minZ, moving.maxX, moving.maxY, moving.maxZ,
            point.x, point.y, point.z, surfaceNormal.x, surfaceNormal.y, surfaceNormal.z,
        )
        if (values.any { !it.isFinite() } || moving.maxX < moving.minX ||
            moving.maxY < moving.minY || moving.maxZ < moving.minZ
        ) return null
        val normalLength = kotlin.math.sqrt(
            surfaceNormal.x * surfaceNormal.x + surfaceNormal.y * surfaceNormal.y + surfaceNormal.z * surfaceNormal.z,
        )
        if (!normalLength.isFinite() || normalLength <= 1e-9) return null
        val nx = surfaceNormal.x / normalLength
        val ny = surfaceNormal.y / normalLength
        val nz = surfaceNormal.z / normalLength
        // Surface placement only supports upward-facing surfaces. The renderer applies the
        // same slope limit before dispatching the tap; keep this pure geometry helper safe too.
        if (nz < 0.7) return null
        val dx = point.x - midpoint(moving.minX, moving.maxX)
        val dy = point.y - midpoint(moving.minY, moving.maxY)
        if (!dx.isFinite() || !dy.isFinite()) return null
        var dz = Double.NEGATIVE_INFINITY
        for (x in listOf(moving.minX, moving.maxX)) {
            for (y in listOf(moving.minY, moving.maxY)) {
                for (z in listOf(moving.minZ, moving.maxZ)) {
                    val relativeX = x + dx - point.x
                    val relativeY = y + dy - point.y
                    val relativeZWithoutTranslation = z - point.z
                    dz = maxOf(
                        dz,
                        -(nx * relativeX + ny * relativeY + nz * relativeZWithoutTranslation) / nz,
                    )
                }
            }
        }
        if (!dz.isFinite()) return null
        val delta = ObjectPlacementDelta(
            x = dx,
            y = dy,
            z = dz,
        )
        return delta.takeIf { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() }
    }

    /** Drops an entire object group onto the printable bed while preserving its internal Z offsets. */
    fun ontoBed(moving: ObjectBounds3D, bedZ: Double): ObjectPlacementDelta? {
        val values = listOf(moving.minX, moving.minY, moving.minZ, moving.maxX, moving.maxY, moving.maxZ, bedZ)
        if (values.any { !it.isFinite() } ||
            moving.maxX < moving.minX || moving.maxY < moving.minY || moving.maxZ < moving.minZ
        ) return null
        val deltaZ = bedZ - moving.minZ
        if (!deltaZ.isFinite()) return null
        return ObjectPlacementDelta(0.0, 0.0, deltaZ)
    }

    /** Avoid overflowing the sum when both endpoints are large but their midpoint is representable. */
    private fun midpoint(min: Double, max: Double): Double = min / 2.0 + max / 2.0
}
