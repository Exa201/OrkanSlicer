package ru.ytkab0bp.slicebeam.render

import kotlin.math.max
import kotlin.math.min

internal data class PrimeTowerDragPosition(val x: Double, val y: Double)

/** Keeps the point grabbed by the user at the same place on the tower while dragging. */
internal object PrimeTowerDragLogic {
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
