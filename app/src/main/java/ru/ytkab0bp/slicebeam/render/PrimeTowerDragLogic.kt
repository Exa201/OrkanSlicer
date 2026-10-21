package ru.ytkab0bp.slicebeam.render

import kotlin.math.max
import kotlin.math.min

internal data class PrimeTowerDragPosition(val x: Double, val y: Double)
internal data class PrimeTowerObstacle(val minX: Double, val minY: Double, val maxX: Double, val maxY: Double)
internal data class PrimeTowerPlacementMap(
    val bedMinX: Double,
    val bedMinY: Double,
    val bedMaxX: Double,
    val bedMaxY: Double,
    val obstacles: List<PrimeTowerObstacle>,
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
