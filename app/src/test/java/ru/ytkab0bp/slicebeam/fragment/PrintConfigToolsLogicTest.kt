package ru.ytkab0bp.slicebeam.fragment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrintConfigToolsLogicTest {
    @Test
    fun emptyFlushMatrixUsesZeroDiagonalAndStandardPurgeElsewhere() {
        val matrix = PrintConfigToolsLogic.flushMatrix(null, slotCount = 1, maxSlots = 16)
        assertEquals(2, matrix.side)
        assertEquals(listOf("0", "280", "280", "0"), matrix.cells)
    }

    @Test
    fun existingFlushMatrixPreservesValuesAndGrowsForMoreFilaments() {
        val small = PrintConfigToolsLogic.flushMatrix("1.5,2;3,4,", slotCount = 2, maxSlots = 16)
        assertEquals(2, small.side)
        assertEquals(listOf("1.5", "2", "3", "4"), small.cells)

        val expanded = PrintConfigToolsLogic.flushMatrix("0,1,2,3", slotCount = 3, maxSlots = 16)
        assertEquals(3, expanded.side)
        assertEquals(listOf("0", "1", "2", "3", "0", "280", "280", "280", "0"), expanded.cells)
    }

    @Test
    fun flushMatrixSerializationAcceptsGermanDecimalsButRejectsInvalidOrNegativeValues() {
        assertEquals("0,1.25,2,0", PrintConfigToolsLogic.serializeFlushMatrix(2, listOf("0", "1,25", "2", "0")))
        assertNull(PrintConfigToolsLogic.serializeFlushMatrix(2, listOf("0", "-1", "2", "0")))
        assertNull(PrintConfigToolsLogic.serializeFlushMatrix(2, listOf("0", "NaN", "2", "0")))
        assertNull(PrintConfigToolsLogic.serializeFlushMatrix(2, listOf("0", "1", "2")))
    }

    @Test
    fun towerPositionsKeepPerPlateValuesAndRepeatTheLastDefinedCoordinate() {
        val positions = PrintConfigToolsLogic.towerPositions("10,20", "30")
        assertEquals(2, positions.plateCount)
        assertEquals(listOf("10", "20"), positions.x)
        assertEquals(listOf("30", "30"), positions.y)

        val defaults = PrintConfigToolsLogic.towerPositions(null, "")
        assertEquals(1, defaults.plateCount)
        assertEquals(listOf("15"), defaults.x)
        assertEquals(listOf("15"), defaults.y)
    }

    @Test
    fun towerPositionsIncludeTheActivePlateEvenWhenItsCoordinateUsesInheritedDefault() {
        val positions = PrintConfigToolsLogic.towerPositions("10", "20", requiredPlateCount = 4)

        assertEquals(4, positions.plateCount)
        assertEquals(listOf("10", "10", "10", "10"), positions.x)
        assertEquals(listOf("20", "20", "20", "20"), positions.y)
    }

    @Test
    fun towerPositionSerializationKeepsNegativeOffsetsAndAcceptsCommaDecimals() {
        val serialized = PrintConfigToolsLogic.serializeTowerPositions(listOf("-1,5", "20"), listOf("2", "3.25"))
        assertEquals("-1.5,20" to "2,3.25", serialized)
        assertNull(PrintConfigToolsLogic.serializeTowerPositions(listOf("NaN"), listOf("2")))
        assertFalse(PrintConfigToolsLogic.parseInput("not a coordinate") != null)
        assertTrue(PrintConfigToolsLogic.parseInput("1,75") == 1.75f)
    }

    @Test
    fun editingOnePlateTowerCoordinatePreservesOtherPlatePositions() {
        assertEquals("10,25", PrintConfigToolsLogic.updateTowerPosition("10,20", 1, "25"))
        assertEquals("10,10,30", PrintConfigToolsLogic.updateTowerPosition("10", 2, "30"))
        assertEquals("15", PrintConfigToolsLogic.updateTowerPosition(null, 0, "15"))
        assertNull(PrintConfigToolsLogic.updateTowerPosition("10,20", 1, "nope"))
    }
}
