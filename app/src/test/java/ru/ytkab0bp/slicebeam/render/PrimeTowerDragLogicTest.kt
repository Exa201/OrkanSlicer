package ru.ytkab0bp.slicebeam.render

import org.junit.Assert.assertEquals
import org.junit.Test

class PrimeTowerDragLogicTest {
    @Test
    fun towerHasPhoneFriendlyTouchMarginAroundVisibleFootprint() {
        assertEquals(
            true,
            PrimeTowerDragLogic.containsTouchTarget(
                pointerX = 38.0,
                pointerY = 45.0,
                towerX = 50.0,
                towerY = 50.0,
                towerWidth = 25.0,
                towerDepth = 30.0,
            ),
        )
        assertEquals(
            false,
            PrimeTowerDragLogic.containsTouchTarget(
                pointerX = 37.9,
                pointerY = 45.0,
                towerX = 50.0,
                towerY = 50.0,
                towerWidth = 25.0,
                towerDepth = 30.0,
            ),
        )
    }

    @Test
    fun dragPreservesGrabPointInsteadOfJumpingTowerCenterToPointer() {
        val position = PrimeTowerDragLogic.position(
            pointerX = 72.0,
            pointerY = 53.0,
            grabOffsetX = 12.0,
            grabOffsetY = 8.0,
            bedMinX = 0.0,
            bedMinY = 0.0,
            bedMaxX = 200.0,
            bedMaxY = 200.0,
            towerWidth = 25.0,
            towerDepth = 30.0,
        )

        assertEquals(60.0, position.x, 0.0001)
        assertEquals(45.0, position.y, 0.0001)
    }

    @Test
    fun dragKeepsWholeTowerInsideTheBed() {
        val position = PrimeTowerDragLogic.position(
            pointerX = -20.0,
            pointerY = 220.0,
            grabOffsetX = 10.0,
            grabOffsetY = 10.0,
            bedMinX = 5.0,
            bedMinY = 7.0,
            bedMaxX = 105.0,
            bedMaxY = 107.0,
            towerWidth = 25.0,
            towerDepth = 30.0,
        )

        assertEquals(5.0, position.x, 0.0001)
        assertEquals(77.0, position.y, 0.0001)
    }
}
