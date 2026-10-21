package ru.ytkab0bp.slicebeam.render

import kotlin.math.max
import kotlin.math.min

internal data class PrimeTowerDragPosition(val x: Double, val y: Double)
internal data class PrimeTowerPoint(val x: Double, val y: Double)
internal data class PrimeTowerObstacle(val minX: Double, val minY: Double, val maxX: Double, val maxY: Double)
internal data class PrimeTowerPlacementMap(
    val bedMinX: Double,
    val bedMinY: Double,
    val bedMaxX: Double,
    val bedMaxY: Double,
    val obstacles: List<PrimeTowerObstacle>,
    /** Optional printable-bed outline parsed from the active printer profile. */
    val bedBoundary: List<PrimeTowerPoint> = emptyList(),
)

/** Keeps the point grabbed by the user at the same place on the tower while dragging. */
internal object PrimeTowerDragLogic {
    fun isPositionFree(
        map: PrimeTowerPlacementMap,
        x: Double,
        y: Double,
        towerWidth: Double,
        towerDepth: Double,
    ): Boolean {
        if (!x.isFinite() || !y.isFinite() || !towerWidth.isFinite() || !towerDepth.isFinite()) return false
        if (x < map.bedMinX || y < map.bedMinY ||
            x + towerWidth > map.bedMaxX || y + towerDepth > map.bedMaxY
        ) return false
        if (map.bedBoundary.size >= 3 && !footprintInsideBoundary(map, x, y, towerWidth, towerDepth)) return false
        val gap = 3.0
        return map.obstacles.none { obstacle ->
            x < obstacle.maxX + gap && x + towerWidth > obstacle.minX - gap &&
                y < obstacle.maxY + gap && y + towerDepth > obstacle.minY - gap
        }
    }

    /** Finds a grid-aligned tower footprint inside the bed and clear of every model bounding box. */
    fun findFreePosition(
        map: PrimeTowerPlacementMap,
        towerWidth: Double,
        towerDepth: Double,
    ): PrimeTowerDragPosition? {
        if (!map.bedMinX.isFinite() || !map.bedMinY.isFinite() ||
            !map.bedMaxX.isFinite() || !map.bedMaxY.isFinite() ||
            !towerWidth.isFinite() || !towerDepth.isFinite() || towerWidth <= 0.0 || towerDepth <= 0.0
        ) return null

        val inset = 2.0
        val minX = map.bedMinX + inset
        val minY = map.bedMinY + inset
        val maxX = map.bedMaxX - towerWidth - inset
        val maxY = map.bedMaxY - towerDepth - inset
        if (maxX < minX || maxY < minY) return null

        val step = 5.0
        var y = minY
        while (y <= maxY + 0.0001) {
            var x = minX
            while (x <= maxX + 0.0001) {
                if (isPositionFree(map, x, y, towerWidth, towerDepth)) return PrimeTowerDragPosition(x, y)
                x += step
            }
            y += step
        }
        return null
    }

    /** Parses the standard Orca/Slic3r `bed_shape` value (for example `0x0,220x0,...`). */
    fun fromBedShape(shape: String?, obstacles: List<PrimeTowerObstacle> = emptyList()): PrimeTowerPlacementMap? {
        if (shape.isNullOrBlank()) return null
        val number = "[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?"
        val pointPattern = Regex("^\\s*($number)[xX]($number)\\s*$")
        val boundary = shape.split(',').map { token ->
            val match = pointPattern.matchEntire(token.trim()) ?: return null
            val x = match.groupValues[1].toDoubleOrNull() ?: return null
            val y = match.groupValues[2].toDoubleOrNull() ?: return null
            if (!x.isFinite() || !y.isFinite()) return null
            PrimeTowerPoint(x, y)
        }.distinct()
        if (boundary.size < 3) return null
        val minX = boundary.minOf { it.x }
        val minY = boundary.minOf { it.y }
        val maxX = boundary.maxOf { it.x }
        val maxY = boundary.maxOf { it.y }
        if (maxX <= minX || maxY <= minY) return null
        return PrimeTowerPlacementMap(minX, minY, maxX, maxY, obstacles, boundary)
    }

    /** Samples the footprint edges so even concave bed outlines reject an overhanging tower. */
    private fun footprintInsideBoundary(
        map: PrimeTowerPlacementMap,
        x: Double,
        y: Double,
        width: Double,
        depth: Double,
    ): Boolean {
        val corners = listOf(
            PrimeTowerPoint(x, y), PrimeTowerPoint(x + width, y),
            PrimeTowerPoint(x + width, y + depth), PrimeTowerPoint(x, y + depth),
        )
        for (edge in corners.indices) {
            val start = corners[edge]
            val end = corners[(edge + 1) % corners.size]
            val distance = kotlin.math.hypot(end.x - start.x, end.y - start.y)
            val samples = kotlin.math.ceil(distance / 5.0).toInt().coerceAtLeast(1)
            for (sample in 0..samples) {
                val fraction = sample.toDouble() / samples
                if (!containsBedPoint(
                        map.bedBoundary,
                        start.x + (end.x - start.x) * fraction,
                        start.y + (end.y - start.y) * fraction,
                    )
                ) return false
            }
        }
        return true
    }

    private fun containsBedPoint(boundary: List<PrimeTowerPoint>, x: Double, y: Double): Boolean {
        var inside = false
        var previous = boundary.last()
        for (current in boundary) {
            val dx = current.x - previous.x
            val dy = current.y - previous.y
            val lengthSquared = dx * dx + dy * dy
            if (lengthSquared > 0.0) {
                val projection = (((x - previous.x) * dx + (y - previous.y) * dy) / lengthSquared).coerceIn(0.0, 1.0)
                val nearestX = previous.x + projection * dx
                val nearestY = previous.y + projection * dy
                if (kotlin.math.hypot(x - nearestX, y - nearestY) <= 1e-6) return true
            }
            if ((current.y > y) != (previous.y > y) &&
                x < (previous.x - current.x) * (y - current.y) / (previous.y - current.y) + current.x
            ) inside = !inside
            previous = current
        }
        return inside
    }

    /** Includes a touch margin so the wipe tower can be grabbed comfortably on a phone. */
    fun containsTouchTarget(
        pointerX: Double,
        pointerY: Double,
        towerX: Double,
        towerY: Double,
        towerWidth: Double,
        towerDepth: Double,
    ): Boolean {
        if (!pointerX.isFinite() || !pointerY.isFinite() || !towerX.isFinite() || !towerY.isFinite()) return false
        if (towerWidth <= 0.0 || towerDepth <= 0.0) return false
        val touchMargin = max(12.0, min(towerWidth, towerDepth) * 0.35)
        return pointerX >= towerX - touchMargin && pointerX <= towerX + towerWidth + touchMargin &&
            pointerY >= towerY - touchMargin && pointerY <= towerY + towerDepth + touchMargin
    }

    fun position(
        pointerX: Double,
        pointerY: Double,
        grabOffsetX: Double,
        grabOffsetY: Double,
        bedMinX: Double,
        bedMinY: Double,
        bedMaxX: Double,
        bedMaxY: Double,
        towerWidth: Double,
        towerDepth: Double,
    ): PrimeTowerDragPosition {
        val maxX = max(bedMinX, bedMaxX - towerWidth)
        val maxY = max(bedMinY, bedMaxY - towerDepth)
        return PrimeTowerDragPosition(
            x = max(bedMinX, min(maxX, pointerX - grabOffsetX)),
            y = max(bedMinY, min(maxY, pointerY - grabOffsetY)),
        )
    }
}
