package ru.ytkab0bp.slicebeam.render

import org.junit.Assert.assertEquals
import org.junit.Test

class PrimeTowerDragLogicTest {
    @Test
    fun automaticPositionAvoidsModelsAndStaysInsideTheBed() {
        val map = PrimeTowerPlacementMap(
            bedMinX = 0.0,
            bedMinY = 0.0,
            bedMaxX = 200.0,
            bedMaxY = 200.0,
            obstacles = listOf(PrimeTowerObstacle(0.0, 0.0, 70.0, 70.0)),
        )

        val position = requireNotNull(PrimeTowerDragLogic.findFreePosition(map, 35.0, 35.0))

        assertEquals(true, PrimeTowerDragLogic.isPositionFree(map, position.x, position.y, 35.0, 35.0))
        assertEquals(true, position.x >= 0.0 && position.y >= 0.0)
        assertEquals(true, position.x + 35.0 <= 200.0 && position.y + 35.0 <= 200.0)
    }

    @Test
    fun automaticPositionReturnsNullWhenModelsCoverTheWholeBed() {
        val map = PrimeTowerPlacementMap(
            bedMinX = 0.0,
            bedMinY = 0.0,
            bedMaxX = 80.0,
            bedMaxY = 80.0,
            obstacles = listOf(PrimeTowerObstacle(-10.0, -10.0, 90.0, 90.0)),
        )

        assertEquals(null, PrimeTowerDragLogic.findFreePosition(map, 35.0, 35.0))
    }

    @Test
    fun printerBedShapeBuildsPreviewBoundsAndRejectsFootprintsOutsideNonRectangularBed() {
        val map = requireNotNull(PrimeTowerDragLogic.fromBedShape("0x0,100x0,80x100,20x100"))

        assertEquals(0.0, map.bedMinX, 0.0001)
        assertEquals(100.0, map.bedMaxX, 0.0001)
        assertEquals(false, PrimeTowerDragLogic.isPositionFree(map, 2.0, 50.0, 10.0, 10.0))
        assertEquals(true, PrimeTowerDragLogic.isPositionFree(map, 25.0, 50.0, 10.0, 10.0))
        val automatic = requireNotNull(PrimeTowerDragLogic.findFreePosition(map, 10.0, 10.0))
        assertEquals(true, PrimeTowerDragLogic.isPositionFree(map, automatic.x, automatic.y, 10.0, 10.0))
    }

    @Test
    fun malformedOrDegeneratePrinterBedShapeDoesNotOfferUnsafePlacement() {
        assertEquals(null, PrimeTowerDragLogic.fromBedShape("not-a-bed"))
        assertEquals(null, PrimeTowerDragLogic.fromBedShape("0x0,100x0,200x0"))
    }

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
