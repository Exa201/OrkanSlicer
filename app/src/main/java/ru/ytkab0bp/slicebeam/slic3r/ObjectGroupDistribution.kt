package ru.ytkab0bp.slicebeam.slic3r

/** Axis on which selected object bounds are distributed with equal clear gaps. */
enum class ObjectDistributionAxis { X, Y, Z }

/** Pure planner for distributing three or more objects while preserving the outer objects. */
internal object ObjectGroupDistribution {
    private const val MIN_GAP_TOLERANCE_MM = 1e-7

    /** Returns per-object translations, or null when the selection cannot be distributed safely. */
    fun plan(
        boundsByIndex: Map<Int, ObjectBounds3D>,
        axis: ObjectDistributionAxis,
    ): Map<Int, ObjectPlacementDelta>? {
        if (boundsByIndex.size < 3 || ObjectPlacement.enclosing(boundsByIndex.values) == null) return null
        val sorted = boundsByIndex.entries.sortedWith(
            compareBy<Map.Entry<Int, ObjectBounds3D>> { it.value.start(axis) }.thenBy { it.key },
        )
        val start = sorted.first().value.start(axis)
        val end = sorted.last().value.end(axis)
        val occupied = sorted.sumOf { it.value.size(axis) }
        val gap = (end - start - occupied) / (sorted.size - 1)
        if (!gap.isFinite() || gap < -MIN_GAP_TOLERANCE_MM) return null

        val result = LinkedHashMap<Int, ObjectPlacementDelta>(sorted.size)
        var targetStart = start
        sorted.forEachIndexed { position, entry ->
            val delta = if (position == 0 || position == sorted.lastIndex) 0.0
            else targetStart - entry.value.start(axis)
            if (!delta.isFinite()) return null
            result[entry.key] = when (axis) {
                ObjectDistributionAxis.X -> ObjectPlacementDelta(delta, 0.0, 0.0)
                ObjectDistributionAxis.Y -> ObjectPlacementDelta(0.0, delta, 0.0)
                ObjectDistributionAxis.Z -> ObjectPlacementDelta(0.0, 0.0, delta)
            }
            targetStart += entry.value.size(axis) + gap.coerceAtLeast(0.0)
        }
        return result
    }

    private fun ObjectBounds3D.start(axis: ObjectDistributionAxis): Double = when (axis) {
        ObjectDistributionAxis.X -> minX
        ObjectDistributionAxis.Y -> minY
        ObjectDistributionAxis.Z -> minZ
    }

    private fun ObjectBounds3D.end(axis: ObjectDistributionAxis): Double = when (axis) {
        ObjectDistributionAxis.X -> maxX
        ObjectDistributionAxis.Y -> maxY
        ObjectDistributionAxis.Z -> maxZ
    }

    private fun ObjectBounds3D.size(axis: ObjectDistributionAxis): Double = end(axis) - start(axis)
}
